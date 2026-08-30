package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar e validar a resposta estruturada do Codex para uma avaliação. */
@Component
public class CustomerEvaluationCodexRunner {
  private static final String BEHAVIORAL_V1 = "BEHAVIORAL_V1";
  private static final String BEHAVIORAL_V2 = "BEHAVIORAL_V2";
  private static final String BEHAVIORAL_V3 = "BEHAVIORAL_V3";
  private static final String BEHAVIORAL_V4 = "BEHAVIORAL_V4";
  private static final Pattern PUBLIC_URL =
      Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE);
  private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
  private final String executable;
  private final String model;
  private final long timeoutMinutes;
  private final String repositoryPath;
  private final String sandboxMode;
  private final ObjectMapper objectMapper;
  private final CodexTelemetryReporter telemetry;

  /** Configura o executor somente leitura e sua telemetria auditável. */
  public CustomerEvaluationCodexRunner(
      @Value("${CUSTOMER_AGENT_CODEX_EXECUTABLE:codex}") String executable,
      @Value("${CUSTOMER_AGENT_MODEL:gpt-5.6-sol}") String model,
      @Value("${CUSTOMER_AGENT_EVALUATION_TIMEOUT_MINUTES:40}") long timeoutMinutes,
      @Value("${CUSTOMER_AGENT_REPOSITORY_PATH:/workspace}") String repositoryPath,
      @Value("${CUSTOMER_AGENT_CODEX_SANDBOX:read-only}") String sandboxMode,
      ObjectMapper objectMapper,
      CodexTelemetryReporter telemetry) {
    this.executable = executable;
    this.model = model;
    this.timeoutMinutes = timeoutMinutes;
    this.repositoryPath = repositoryPath;
    this.sandboxMode = sandboxMode;
    this.objectMapper = objectMapper;
    this.telemetry = telemetry;
  }

  /** Executa a avaliação por stdin e retorna somente uma resposta compatível com o schema. */
  Map<String, String> run(long evaluationId, Map<?, ?> job)
      throws IOException, InterruptedException {
    List<Path> visualEvidence = downloadVisualEvidence(String.valueOf(job.get("assetReference")));
    try {
      String baselinePrompt = buildBaselinePrompt(job);
      JsonNode baseline =
          execute(
              evaluationId,
              baselinePrompt,
              "prompts/customer-agent/v1/evaluation-schema.json",
              null,
              visualEvidence);
      String simulationVersion = String.valueOf(job.get("simulationVersion"));
      if (!BEHAVIORAL_V1.equals(simulationVersion)
          && !BEHAVIORAL_V2.equals(simulationVersion)
          && !BEHAVIORAL_V3.equals(simulationVersion)
          && !BEHAVIORAL_V4.equals(simulationVersion)) {
        return baselinePayload(baseline, baselinePrompt);
      }

      String behavioralPrompt = buildBehavioralPrompt(job, baseline, simulationVersion);
      String behavioralSchema = schemaResource(simulationVersion);
      JsonNode behavioral =
          execute(
              evaluationId, behavioralPrompt, behavioralSchema, simulationVersion, visualEvidence);
      LinkedHashMap<String, String> payload = new LinkedHashMap<>();
      payload.put("assessment", behavioral.get("assessment").asText());
      payload.put("hypothesisJson", objectMapper.writeValueAsString(behavioral.get("hypotheses")));
      payload.put(
          "rawModelResponse",
          "BASELINE_V1_REQUEST\n"
              + baselinePrompt
              + "\nBASELINE_V1_RESPONSE\n"
              + objectMapper.writeValueAsString(baseline)
              + "\n"
              + simulationVersion
              + "_REQUEST\n"
              + behavioralPrompt
              + "\n"
              + simulationVersion
              + "_RESPONSE\n"
              + objectMapper.writeValueAsString(behavioral));
      payload.put("model", model);
      payload.put("baselineResultJson", objectMapper.writeValueAsString(baseline));
      payload.put("behavioralResultJson", objectMapper.writeValueAsString(behavioral));
      return payload;
    } finally {
      for (Path image : visualEvidence) Files.deleteIfExists(image);
    }
  }

  /** Executa uma fase versionada e valida o contrato correspondente à versão solicitada. */
  private JsonNode execute(
      long evaluationId,
      String prompt,
      String schemaResource,
      String behavioralVersion,
      List<Path> visualEvidence)
      throws IOException, InterruptedException {
    Path answer = Files.createTempFile("customer-agent-evaluation-answer-", ".json");
    Path processLog = Files.createTempFile("customer-agent-evaluation-process-", ".log");
    Path schema = materialize(schemaResource, ".json");
    Path mcp = materialize("mcp/customer-agent.mjs", ".mjs");
    try {
      ProcessBuilder builder =
          new ProcessBuilder(buildCommand(answer, schema, mcp, visualEvidence));
      builder.redirectErrorStream(true).redirectOutput(processLog.toFile());
      builder
          .environment()
          .put(
              "MCP_BACKEND_URL",
              System.getenv().getOrDefault("CUSTOMER_AGENT_MCP_URL", "http://backend:8000"));
      builder.environment().put("MCP_EVALUATION_ID", String.valueOf(evaluationId));
      Process process = builder.start();
      process.getOutputStream().write(prompt.getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      try (CodexTelemetryReporter.Session session =
          telemetry.monitor(evaluationId, process, processLog)) {
        if (!process.waitFor(timeoutMinutes, TimeUnit.MINUTES)) {
          process.destroyForcibly();
          process.waitFor(10, TimeUnit.SECONDS);
          throw new IllegalStateException("Timeout do Codex após " + timeoutMinutes + " minutos.");
        }
        String diagnostic = Files.readString(processLog, StandardCharsets.UTF_8);
        if (process.exitValue() != 0) {
          throw new IllegalStateException("Codex falhou: " + diagnostic);
        }
        String raw = Files.readString(answer, StandardCharsets.UTF_8);
        JsonNode result = objectMapper.readTree(raw);
        if (behavioralVersion == null) validateBaseline(result);
        else validateBehavioral(result, behavioralVersion);
        session.success();
        return result;
      }
    } finally {
      Files.deleteIfExists(answer);
      Files.deleteIfExists(processLog);
      Files.deleteIfExists(schema);
      Files.deleteIfExists(mcp);
    }
  }

  /** Converte o comportamento legado sem preencher campos exclusivos do comparativo. */
  private Map<String, String> baselinePayload(JsonNode result, String prompt) throws IOException {
    LinkedHashMap<String, String> payload = new LinkedHashMap<>();
    String raw = objectMapper.writeValueAsString(result);
    payload.put("assessment", result.get("assessment").asText());
    payload.put("hypothesisJson", objectMapper.writeValueAsString(result.get("hypotheses")));
    payload.put(
        "rawModelResponse", "BASELINE_V1_REQUEST\n" + prompt + "\nBASELINE_V1_RESPONSE\n" + raw);
    payload.put("model", model);
    payload.put("baselineResultJson", raw);
    payload.put("behavioralResultJson", null);
    return payload;
  }

  /** Monta o comando com arquivo final, schema versionado e sandbox somente leitura. */
  List<String> buildCommand(Path answer, Path schema) {
    return buildCommand(answer, schema, Path.of("customer-agent.mjs"));
  }

  /** Monta o comando com o MCP exclusivo do Agente Cliente. */
  List<String> buildCommand(Path answer, Path schema, Path mcp) {
    return buildCommand(answer, schema, mcp, List.of());
  }

  /** Monta o comando anexando somente evidências visuais já baixadas e validadas. */
  List<String> buildCommand(Path answer, Path schema, Path mcp, List<Path> visualEvidence) {
    List<String> command =
        new ArrayList<>(
            List.of(
                executable,
                "--search",
                "exec",
                "-",
                "--skip-git-repo-check",
                "--cd",
                repositoryPath,
                "--output-schema",
                schema.toString(),
                "--output-last-message",
                answer.toString(),
                "--color",
                "never",
                "--config",
                "mcp_servers.customer_agent.command=\"node\"",
                "--config",
                "mcp_servers.customer_agent.args=[\"" + mcp.toAbsolutePath() + "\"]"));
    if ("danger-full-access".equals(sandboxMode)) {
      command.add(4, "--dangerously-bypass-approvals-and-sandbox");
    } else {
      command.addAll(4, List.of("--sandbox", "read-only"));
    }
    if (model != null && !model.isBlank()) {
      command.add("--model");
      command.add(model);
    }
    for (Path image : visualEvidence) {
      command.add("--image");
      command.add(image.toAbsolutePath().toString());
    }
    return command;
  }

  /** Baixa até três imagens públicas citadas no ativo para inspeção multimodal direta. */
  List<Path> downloadVisualEvidence(String assetReference)
      throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    List<Path> result = new ArrayList<>();
    var matcher = PUBLIC_URL.matcher(assetReference == null ? "" : assetReference);
    while (matcher.find() && result.size() < 3) {
      URI uri = URI.create(matcher.group().replaceAll("[),.;]+$", ""));
      if (!publicHost(uri.getHost())) continue;
      HttpResponse<byte[]> response =
          client.send(
              HttpRequest.newBuilder(uri).timeout(java.time.Duration.ofSeconds(45)).GET().build(),
              HttpResponse.BodyHandlers.ofByteArray());
      String contentType = response.headers().firstValue("Content-Type").orElse("");
      if (response.statusCode() < 200
          || response.statusCode() >= 300
          || !contentType.toLowerCase(java.util.Locale.ROOT).startsWith("image/")
          || response.body().length == 0
          || response.body().length > MAX_IMAGE_BYTES) continue;
      String suffix =
          contentType.toLowerCase(java.util.Locale.ROOT).contains("jpeg") ? ".jpg" : ".png";
      Path image = Files.createTempFile("customer-agent-visual-evidence-", suffix);
      Files.write(image, response.body());
      result.add(image);
    }
    return result;
  }

  /** Bloqueia loopback e redes privadas para impedir que a evidência vire acesso interno. */
  private boolean publicHost(String host) {
    if (host == null) return false;
    String value = host.toLowerCase(java.util.Locale.ROOT);
    return !value.equals("localhost")
        && !value.equals("0.0.0.0")
        && !value.equals("::1")
        && !value.startsWith("127.")
        && !value.startsWith("10.")
        && !value.startsWith("192.168.")
        && !value.startsWith("169.254.")
        && !value.matches("172\\.(1[6-9]|2\\d|3[01])\\..*");
  }

  /** Resolve o prompt versionado usando o contexto congelado e pesquisa pública auditável. */
  private String buildBaselinePrompt(Map<?, ?> job) throws IOException {
    return read("prompts/customer-agent/v1/evaluation.md")
        .replace("{{PERSONA_JSON}}", String.valueOf(job.get("persona")))
        .replace("{{ASSET_TYPE}}", String.valueOf(job.get("assetType")))
        .replace("{{ASSET_REFERENCE}}", String.valueOf(job.get("assetReference")));
  }

  /** Resolve o prompt comportamental versionado com o mesmo contexto e baseline congelado. */
  private String buildBehavioralPrompt(Map<?, ?> job, JsonNode baseline, String simulationVersion)
      throws IOException {
    String resource = promptResource(simulationVersion);
    String prompt =
        read(resource)
            .replace("{{PERSONA_JSON}}", String.valueOf(job.get("persona")))
            .replace("{{ASSET_TYPE}}", String.valueOf(job.get("assetType")))
            .replace("{{ASSET_REFERENCE}}", String.valueOf(job.get("assetReference")))
            .replace("{{BASELINE_JSON}}", objectMapper.writeValueAsString(baseline));
    if (BEHAVIORAL_V4.equals(simulationVersion)) {
      return prompt.replace("{{PSIQUE_BEHAVIORAL_CORE_V4}}", behavioralCoreV4());
    }
    if (BEHAVIORAL_V3.equals(simulationVersion)) {
      return prompt.replace("{{PSIQUE_BEHAVIORAL_CORE_V3}}", behavioralCoreV3());
    }
    return prompt.replace("{{PSIQUE_BEHAVIORAL_CORE_V2}}", behavioralCoreV2());
  }

  /** Seleciona o prompt imutável correspondente à versão solicitada. */
  private String promptResource(String simulationVersion) {
    if (BEHAVIORAL_V4.equals(simulationVersion))
      return "prompts/customer-agent/behavioral-v4/evaluation.md";
    if (BEHAVIORAL_V3.equals(simulationVersion))
      return "prompts/customer-agent/behavioral-v3/evaluation.md";
    if (BEHAVIORAL_V2.equals(simulationVersion))
      return "prompts/customer-agent/behavioral-v2/evaluation.md";
    return "prompts/customer-agent/behavioral-v1/evaluation.md";
  }

  /** Seleciona o schema estrito correspondente à versão solicitada. */
  private String schemaResource(String simulationVersion) {
    if (BEHAVIORAL_V4.equals(simulationVersion))
      return "prompts/customer-agent/behavioral-v4/evaluation-schema.json";
    if (BEHAVIORAL_V3.equals(simulationVersion))
      return "prompts/customer-agent/behavioral-v3/evaluation-schema.json";
    if (BEHAVIORAL_V2.equals(simulationVersion))
      return "prompts/customer-agent/behavioral-v2/evaluation-schema.json";
    return "prompts/customer-agent/behavioral-v1/evaluation-schema.json";
  }

  /** Lê o núcleo científico único compartilhado pelas execuções de Psique v2. */
  private String behavioralCoreV2() throws IOException {
    return read("prompts/psique/behavioral-core-v2.md");
  }

  /** Lê o núcleo sensorial compartilhado pelas novas execuções de Psique v3. */
  private String behavioralCoreV3() throws IOException {
    return read("prompts/psique/behavioral-core-v3.md");
  }

  /** Lê o núcleo estético compartilhado pelas novas execuções de Psique v4. */
  private String behavioralCoreV4() throws IOException {
    return read("prompts/psique/behavioral-core-v4.md");
  }

  /** Materializa um recurso do classpath para consumo seguro pelo processo Codex. */
  private Path materialize(String resource, String suffix) throws IOException {
    Path path = Files.createTempFile("customer-agent-resource-", suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Lê integralmente um recurso versionado do classpath. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Rejeita qualquer resposta sem decisão, parecer e hipóteses estruturadas. */
  private void validateBaseline(JsonNode result) {
    if (!result.hasNonNull("decision")
        || !result.hasNonNull("assessment")
        || !result.has("hypotheses")
        || !result.get("hypotheses").isArray()
        || !result.has("sources")
        || !result.get("sources").isArray()) {
      throw new IllegalArgumentException("Resposta fora do contrato do Agente Cliente v1.");
    }
  }

  /** Rejeita simulação comportamental incompleta ou probabilidades incoerentes. */
  void validateBehavioral(JsonNode result) {
    validateBehavioral(result, BEHAVIORAL_V1);
  }

  /** Rejeita simulações v2, v3 ou v4 que omitam seus motores humanos obrigatórios. */
  void validateBehavioral(JsonNode result, String simulationVersion) {
    validateBaseline(result);
    if (!result.has("initialState")
        || !result.has("mentalTransitions")
        || !result.get("mentalTransitions").isArray()
        || !result.has("actionProbabilities")
        || !result.has("memoryRecall")
        || !result.has("baselineComparison")) {
      throw new IllegalArgumentException("Resposta fora do contrato comportamental v1.");
    }
    if ((BEHAVIORAL_V2.equals(simulationVersion)
            || BEHAVIORAL_V3.equals(simulationVersion)
            || BEHAVIORAL_V4.equals(simulationVersion))
        && (!result.has("affectiveImpulse")
            || !result.has("motivationalDynamics")
            || !result.has("noveltyFamiliarity")
            || !result.has("relationalValue")
            || !result.has("postHocRationalization")
            || !result.has("ethicalBoundary")
            || !"FOUNDATIONAL"
                .equals(result.path("relationalValue").path("foundationalNeed").asText()))) {
      throw new IllegalArgumentException("Resposta fora do contrato comportamental v2 de Psique.");
    }
    if (BEHAVIORAL_V3.equals(simulationVersion)) {
      PsiqueSensoryContract.validate(result.path("sensoryExperience"));
    }
    if (BEHAVIORAL_V4.equals(simulationVersion)) {
      PsiqueVisualCompositionContract.validate(result.path("sensoryExperience"));
    }
    int total = 0;
    var probabilities = result.get("actionProbabilities").fields();
    while (probabilities.hasNext()) total += probabilities.next().getValue().asInt();
    if (total != 100) {
      throw new IllegalArgumentException(
          "Probabilidades comportamentais devem somar 100; soma recebida=" + total);
    }
  }
}
