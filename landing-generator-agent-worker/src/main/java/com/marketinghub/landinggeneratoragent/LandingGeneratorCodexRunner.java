package com.marketinghub.landinggeneratoragent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar o Codex 5.6 Sol isolado e validar o plano de correção da landing. */
@Component
public class LandingGeneratorCodexRunner {
  private static final long ACTIVITY_POLL_SECONDS = 15L;
  private static final Pattern CHECKOUT_ANCHOR =
      Pattern.compile(
          "<a\\b(?=[^>]*(?:id\\s*=\\s*[\\\"']checkout-cta-primary[\\\"']|data-analytics-role\\s*=\\s*[\\\"']primary-checkout[\\\"']))[^>]*>",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern HREF =
      Pattern.compile("\\bhref\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']", Pattern.CASE_INSENSITIVE);
  private static final Pattern IMAGE_SRC_ATTRIBUTE =
      Pattern.compile("(?is)<img\\b[^>]*\\bsrc\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
  private final LandingGeneratorAgentProperties properties;
  private final ObjectMapper objectMapper;
  private final CodexTelemetryReporter telemetry;
  private final LandingHtmlCodexGenerator htmlGenerator;

  /** Inicializa o runner com configuração, JSON e telemetria. */
  public LandingGeneratorCodexRunner(
      LandingGeneratorAgentProperties properties,
      ObjectMapper objectMapper,
      CodexTelemetryReporter telemetry,
      LandingHtmlCodexGenerator htmlGenerator) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.telemetry = telemetry;
    this.htmlGenerator = htmlGenerator;
  }

  /** Executa o agente em sandbox read-only e devolve auditoria integral. */
  public Map<String, Object> run(LandingAgentJob job) throws IOException, InterruptedException {
    Path output = Files.createTempFile("landing-agent-", ".json");
    Path log = Files.createTempFile("landing-agent-process-", ".log");
    Path schema =
        materialize(
            "prompts/landing-generator/v1/remediation-schema.json", "landing-schema-", ".json");
    Path mcp = materialize("mcp/landing-generator.mjs", "landing-mcp-", ".mjs");
    String request = buildPrompt(job);
    try {
      Process process =
          new ProcessBuilder(command(output, schema, mcp, job))
              .redirectErrorStream(true)
              .redirectOutput(log.toFile())
              .start();
      process.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      try (CodexTelemetryReporter.Session session = telemetry.monitor(job, process, log)) {
        if (!waitWhileActive(process, log)) {
          process.destroyForcibly();
          process.waitFor(10, TimeUnit.SECONDS);
          throw new CodexActivityTimeoutException(
              "Timeout do Codex do Agente Gerador de Landing sem atividade comprovada");
        }
        if (process.exitValue() != 0)
          throw new IllegalStateException(
              "Codex encerrou com código " + process.exitValue() + ": " + Files.readString(log));
        String raw = Files.readString(output);
        JsonNode decision = objectMapper.readTree(raw);
        CodexTelemetryReporter.TokenUsage htmlUsage = materializeHtmlIfNeeded(decision, job);
        raw = objectMapper.writeValueAsString(decision);
        validate(decision, job);
        CodexTelemetryReporter.TokenUsage usage = session.tokenUsage();
        session.success();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("decisionJson", raw);
        result.put("requestJson", request);
        result.put("responseJson", raw);
        result.put("model", properties.getModel());
        result.put(
            "inputTokens",
            sum(usage.inputTokens(), htmlUsage == null ? null : htmlUsage.inputTokens()));
        result.put(
            "cachedInputTokens",
            sum(
                usage.cachedInputTokens(),
                htmlUsage == null ? null : htmlUsage.cachedInputTokens()));
        result.put(
            "outputTokens",
            sum(usage.outputTokens(), htmlUsage == null ? null : htmlUsage.outputTokens()));
        result.put("costUsd", null);
        return result;
      }
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(log);
      Files.deleteIfExists(schema);
      Files.deleteIfExists(mcp);
    }
  }

  /** Materializa o artefato em interação dedicada quando a decisão por código vier sem HTML. */
  CodexTelemetryReporter.TokenUsage materializeHtmlIfNeeded(JsonNode decision, LandingAgentJob job)
      throws IOException, InterruptedException {
    if (!requiresIntegralHtml(decision)) return null;
    LandingHtmlCodexGenerator.GeneratedHtml generated = htmlGenerator.generate(job, decision);
    ((com.fasterxml.jackson.databind.node.ObjectNode) decision)
        .put("generatedHtml", generated.html());
    return generated.usage();
  }

  /** Identifica quando a decisão por código ainda não materializou o HTML obrigatório. */
  private boolean requiresIntegralHtml(JsonNode decision) {
    return "CODEX_CODE_IMPLEMENTATION"
            .equals(decision.path("selectedGenerationApproach").path("approachCode").asText())
        && (decision.path("generatedHtml").isNull()
            || decision.path("generatedHtml").asText("").length() < 500);
  }

  /** Soma a telemetria das duas interações sem transformar ausência de medição em zero. */
  private Long sum(Long planningTokens, Long htmlTokens) {
    if (planningTokens == null && htmlTokens == null) return null;
    return (planningTokens == null ? 0 : planningTokens) + (htmlTokens == null ? 0 : htmlTokens);
  }

  /**
   * Aguarda enquanto o processo produz atividade, com teto absoluto de três janelas operacionais.
   */
  boolean waitWhileActive(Process process, Path log) throws IOException, InterruptedException {
    long idleLimit = properties.getCodexTimeout().toMillis();
    long hardLimit = Math.multiplyExact(idleLimit, 3L);
    long startedAt = System.currentTimeMillis();
    long lastActivityAt = startedAt;
    long observedSize = Files.size(log);
    while (System.currentTimeMillis() - startedAt < hardLimit) {
      if (process.waitFor(ACTIVITY_POLL_SECONDS, TimeUnit.SECONDS)) return true;
      long currentSize = Files.size(log);
      if (currentSize != observedSize) {
        observedSize = currentSize;
        lastActivityAt = System.currentTimeMillis();
      }
      if (System.currentTimeMillis() - lastActivityAt >= idleLimit) return false;
    }
    return false;
  }

  /** Monta o comando com modelo, pesquisa, MCP exclusivo e autoridade não interativa. */
  List<String> command(Path output, Path schema, Path mcp, LandingAgentJob job) {
    List<String> command =
        new ArrayList<>(
            List.of(
                properties.getCodexCommand(),
                "--search",
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
                "--json",
                "--config",
                "approval_policy=\"never\"",
                "--config",
                "mcp_servers.landing_generator.command=\"node\"",
                "--config",
                "mcp_servers.landing_generator.args=[\"" + mcp.toAbsolutePath() + "\"]",
                "--config",
                "mcp_servers.landing_generator.env={MCP_MARKETING_HUB_URL=\""
                    + properties.getMarketingHubUrl()
                    + "\",MCP_EXECUTION_ID=\""
                    + job.executionId()
                    + "\",MCP_EXPERIMENT_ID=\""
                    + job.experimentId()
                    + "\",PLAYWRIGHT_BROWSERS_PATH=\"/ms-playwright\"}"));
    if (properties.getReasoningEffort() != null && !properties.getReasoningEffort().isBlank())
      command.addAll(
          List.of(
              "--config", "model_reasoning_effort=\"" + properties.getReasoningEffort() + "\""));
    command.addAll(List.of("--model", properties.getModel()));
    return command;
  }

  /** Resolve o prompt versionado com o snapshot congelado. */
  private String buildPrompt(LandingAgentJob job) throws IOException {
    return read("prompts/landing-generator/v1/remediation.md")
        .replace("{{EXECUTION_ID}}", job.executionId())
        .replace("{{EXPERIMENT_ID}}", job.experimentId().toString())
        .replace("{{CONTEXT}}", objectMapper.writeValueAsString(job.context()));
  }

  /** Bloqueia planos vagos, abordagens indisponíveis, autoaprovação e autonomia sem controle. */
  void validate(JsonNode value, LandingAgentJob job) {
    if (!"REGENERATE_BEFORE_PUBLICATION".equals(value.path("approvalRecommendation").asText()))
      throw new IllegalArgumentException("Agente executor não pode aprovar a própria landing");
    if (value.path("recommendedRegeneration").isEmpty()
        || value.path("acceptanceCriteria").isEmpty())
      throw new IllegalArgumentException("Correção sem etapa e critérios verificáveis");
    if (value.path("score").asInt(-1) < 0)
      throw new IllegalArgumentException("Plano sem score de referência");
    if (value.path("strategyOptions").size() < 3
        || value.path("selectedStrategy").isEmpty()
        || value.path("autonomousBacklog").isEmpty())
      throw new IllegalArgumentException("Plano sem decisão autônoma comparada e executável");
    if (value.path("generationApproachOptions").size() < 3
        || value.path("selectedGenerationApproach").isEmpty())
      throw new IllegalArgumentException("Plano sem comparação da abordagem de geração");
    String selectedApproach =
        value.path("selectedGenerationApproach").path("approachCode").asText();
    boolean selectedDeclaredAvailable = false;
    for (JsonNode option : value.path("generationApproachOptions")) {
      if (selectedApproach.equals(option.path("approachCode").asText())
          && option.path("available").asBoolean(false)) {
        selectedDeclaredAvailable = true;
      }
    }
    JsonNode context = objectMapper.valueToTree(job.context());
    boolean selectedContractAvailable = false;
    for (JsonNode option : context.path("generationApproachCatalog")) {
      if (selectedApproach.equals(option.path("approachCode").asText())
          && option.path("available").asBoolean(false)) {
        selectedContractAvailable = true;
      }
    }
    if (!selectedDeclaredAvailable || !selectedContractAvailable)
      throw new IllegalArgumentException("Abordagem selecionada não possui executor disponível");
    if ("CODEX_CODE_IMPLEMENTATION".equals(selectedApproach)
        && (value.path("generatedHtml").isNull()
            || value.path("generatedHtml").asText("").length() < 500))
      throw new IllegalArgumentException("Implementação por código sem HTML completo");
    if (!"CODEX_CODE_IMPLEMENTATION".equals(selectedApproach)
        && !value.path("generatedHtml").isNull())
      throw new IllegalArgumentException("HTML completo informado fora da abordagem por código");
    if ("CODEX_CODE_IMPLEMENTATION".equals(selectedApproach))
      validateCheckoutContract(value.path("generatedHtml").asText(), context);
    if ("CODEX_CODE_IMPLEMENTATION".equals(selectedApproach))
      validateApprovedLandingAssets(value.path("generatedHtml").asText(), context);
    if (value.path("expectedMetrics").isEmpty()
        || value.path("stopConditions").path("continueWhen").isEmpty()
        || value.path("stopConditions").path("adjustWhen").isEmpty()
        || value.path("stopConditions").path("stopWhen").isEmpty())
      throw new IllegalArgumentException("Plano autônomo sem métricas e condições de controle");
  }

  /** Exige que o HTML preserve a quantidade mínima de arquivos aprovados do produto real. */
  private void validateApprovedLandingAssets(String html, JsonNode context) {
    int required = context.path("minimumApprovedLandingVisualAssets").asInt(0);
    if (required <= 0) return;
    Set<String> approvedUrls = new HashSet<>();
    for (JsonNode asset : context.path("approvedLandingVisualAssets")) {
      String assetUrl = asset.path("assetUrl").asText("");
      if (!assetUrl.isBlank()) approvedUrls.add(assetUrl);
    }
    Set<String> usedUrls = new HashSet<>();
    Matcher imageSource = IMAGE_SRC_ATTRIBUTE.matcher(html);
    while (imageSource.find()) {
      String assetUrl = imageSource.group(1).trim();
      if (approvedUrls.contains(assetUrl)) usedUrls.add(assetUrl);
    }
    if (usedUrls.size() < required)
      throw new IllegalArgumentException(
          "HTML não reutiliza a quantidade mínima de arquivos APPROVED da Biblioteca Audiovisual");
  }

  /** Impede callback quando Dédalo não preserva literalmente o checkout congelado. */
  private void validateCheckoutContract(String html, JsonNode context) {
    String canonicalUrl = context.path("checkoutContract").path("canonicalUrl").asText();
    if (canonicalUrl.isBlank())
      throw new IllegalArgumentException("Contrato canônico de checkout ausente no snapshot");
    Matcher anchors = CHECKOUT_ANCHOR.matcher(html);
    boolean found = false;
    while (anchors.find()) {
      found = true;
      Matcher href = HREF.matcher(anchors.group());
      if (!href.find() || !canonicalUrl.equals(href.group(1)))
        throw new IllegalArgumentException("Dédalo alterou o destino canônico do checkout");
    }
    if (!found) throw new IllegalArgumentException("HTML sem CTA marcado para o checkout canônico");
  }

  /** Materializa um recurso versionado somente no diretório temporário. */
  private Path materialize(String resource, String prefix, String suffix) throws IOException {
    Path path = Files.createTempFile(prefix, suffix);
    Files.writeString(path, read(resource));
    return path;
  }

  /** Lê um recurso versionado integralmente. */
  private String read(String resource) throws IOException {
    try (var input = new ClassPathResource(resource).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
