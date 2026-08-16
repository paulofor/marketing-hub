package com.marketinghub.metaadapproverworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar o gate independente dos entregáveis visuais produzidos por Têmis. */
@Component
public class TemisLibraryImageReviewRunner {
  private final MetaAdApproverProperties properties;
  private final ObjectMapper objectMapper;

  /** Inicializa o revisor com configuração e parser auditável. */
  public TemisLibraryImageReviewRunner(
      MetaAdApproverProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  /** Executa uma nova sessão Codex, inspeciona a imagem por MCP e devolve parecer estruturado. */
  public Map<String, Object> run(TemisLibraryReviewJob job)
      throws IOException, InterruptedException {
    String reviewerExecutionId = UUID.randomUUID().toString();
    Path output = Files.createTempFile("temis-library-review-", ".json");
    Path processOutput = Files.createTempFile("temis-library-review-process-", ".log");
    Path schema =
        materialize("prompts/image-studio/v1/review-schema.json", "temis-review-schema-", ".json");
    Path mcp = materializeMcp();
    String request = buildPrompt(job);
    try {
      ProcessBuilder builder =
          new ProcessBuilder(buildCommand(output, schema, mcp, job))
              .redirectErrorStream(true)
              .redirectOutput(processOutput.toFile());
      builder.environment().put("MCP_MARKETING_HUB_URL", properties.getMarketingHubUrl());
      builder.environment().put("MCP_VISUAL_ASSET_ID", job.assetId().toString());
      builder.environment().put("MCP_COMMERCIAL_PLAN_ID", job.commercialPlanId().toString());
      Process process = builder.start();
      process.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      if (!process.waitFor(properties.getCodexTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        process.waitFor(10, TimeUnit.SECONDS);
        throw new IllegalStateException("Timeout da revisão independente da Biblioteca");
      }
      String processLog = Files.readString(processOutput, StandardCharsets.UTF_8);
      if (process.exitValue() != 0) {
        throw new IllegalStateException(
            "Codex revisor encerrou com código " + process.exitValue() + ": " + processLog);
      }
      String raw = Files.readString(output, StandardCharsets.UTF_8);
      JsonNode result = objectMapper.readTree(raw);
      validate(result);
      Map<String, Object> callback = new LinkedHashMap<>();
      callback.put("decision", result.path("decision").asText());
      callback.put("reviewerExecutionId", reviewerExecutionId);
      callback.put("summary", result.path("summary").asText());
      callback.put("requestJson", request);
      callback.put("responseJson", raw);
      callback.put("error", "");
      return callback;
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(processOutput);
      Files.deleteIfExists(schema);
      deleteMcpRuntime(mcp);
    }
  }

  /** Monta o comando em sandbox somente leitura e com MCP exclusivo da Biblioteca. */
  private List<String> buildCommand(Path output, Path schema, Path mcp, TemisLibraryReviewJob job) {
    List<String> command = new ArrayList<>();
    command.addAll(
        List.of(
            properties.getCodexCommand(),
            "exec",
            "-",
            "--skip-git-repo-check",
            "--sandbox",
            "read-only",
            "--cd",
            properties.getRepositoryPath(),
            "--output-schema",
            schema.toString(),
            "--output-last-message",
            output.toString(),
            "--color",
            "never",
            "--config",
            "approval_policy=\"never\"",
            "--config",
            "mcp_servers.temis_library_review.command=\"node\"",
            "--config",
            "mcp_servers.temis_library_review.args=[\"" + mcp.toAbsolutePath() + "\"]",
            "--config",
            "mcp_servers.temis_library_review.env={MCP_MARKETING_HUB_URL=\""
                + properties.getMarketingHubUrl()
                + "\",MCP_VISUAL_ASSET_ID=\""
                + job.assetId()
                + "\",MCP_COMMERCIAL_PLAN_ID=\""
                + job.commercialPlanId()
                + "\"}"));
    if (hasText(properties.getReasoningEffort())) {
      command.addAll(
          List.of(
              "--config", "model_reasoning_effort=\"" + properties.getReasoningEffort() + "\""));
    }
    if (hasText(properties.getModel())) {
      command.addAll(List.of("--model", properties.getModel()));
    }
    return command;
  }

  /** Resolve o prompt versionado com o snapshot congelado pelo backend. */
  private String buildPrompt(TemisLibraryReviewJob job) throws IOException {
    return read("prompts/image-studio/v1/review.md")
        .replace("{{ASSET_ID}}", job.assetId().toString())
        .replace("{{PLAN_ID}}", job.commercialPlanId().toString())
        .replace("{{CONTEXT}}", objectMapper.writeValueAsString(job.context()));
  }

  /** Bloqueia aprovação sem inspeção declarada e sem critérios premium mínimos. */
  private void validate(JsonNode result) {
    String decision = result.path("decision").asText();
    if (!List.of("APPROVED", "ADJUST").contains(decision)
        || result.path("summary").asText().isBlank()
        || !result.path("inspected").asBoolean(false)) {
      throw new IllegalArgumentException("Parecer independente da Biblioteca incompleto");
    }
    if ("APPROVED".equals(decision)
        && (result.path("qualityScore").asInt(-1) < 90
            || result.path("deliveryFidelityScore").asInt(-1) < 90
            || result.path("commercialReuseScore").asInt(-1) < 85
            || !result.path("issues").isEmpty())) {
      throw new IllegalArgumentException("Aprovação da Biblioteca abaixo do padrão premium");
    }
  }

  /** Materializa um recurso versionado em diretório temporário. */
  private Path materialize(String resource, String prefix, String suffix) throws IOException {
    Path path = Files.createTempFile(prefix, suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Materializa o MCP junto às dependências disponíveis na imagem ou no módulo local. */
  private Path materializeMcp() throws IOException {
    Path directory = Files.createTempDirectory("temis-library-review-mcp-");
    Path server = directory.resolve("temis-library-review.mjs");
    Files.writeString(server, read("mcp/temis-library-review.mjs"));
    Path imageDependencies = Path.of("/app/node_modules/@modelcontextprotocol/sdk");
    Path dependencies =
        Files.isDirectory(imageDependencies)
            ? imageDependencies.getParent().getParent()
            : Path.of("node_modules").toAbsolutePath();
    if (!Files.isDirectory(dependencies.resolve("@modelcontextprotocol/sdk"))) {
      throw new IOException("Dependências do MCP de revisão da Biblioteca não estão disponíveis");
    }
    Files.createSymbolicLink(directory.resolve("node_modules"), dependencies);
    return server;
  }

  /** Remove somente o runtime temporário desta execução. */
  private void deleteMcpRuntime(Path server) throws IOException {
    Path directory = server.getParent();
    Files.deleteIfExists(server);
    Files.deleteIfExists(directory.resolve("node_modules"));
    Files.deleteIfExists(directory);
  }

  /** Lê integralmente um recurso versionado. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Indica se uma configuração textual está preenchida. */
  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
