package com.marketinghub.growthoperatorworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar o Codex com sandbox somente leitura e saida JSON validada. */
@Component
public class CodexReadOnlyRunner {
  private static final Logger log = LoggerFactory.getLogger(CodexReadOnlyRunner.class);
  private final WorkerProperties properties;
  private final ObjectMapper objectMapper;
  private final CodexTelemetryReporter telemetry;

  /** Configura o executor com telemetria auditável. */
  @Autowired
  public CodexReadOnlyRunner(
      WorkerProperties properties, ObjectMapper objectMapper, CodexTelemetryReporter telemetry) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.telemetry = telemetry;
  }

  /** Mantém construção direta dos testes de comando. */
  public CodexReadOnlyRunner(WorkerProperties properties, ObjectMapper objectMapper) {
    this(properties, objectMapper, null);
  }

  /** Executa diagnóstico efêmero com memória correlacionada e repositório somente leitura. */
  public Map<String, Object> run(GrowthOperatorJob job) throws IOException, InterruptedException {
    Optional<Map<String, Object>> strategicGate = strategicContractGate(job);
    if (strategicGate.isPresent()) return strategicGate.get();
    Path output = Files.createTempFile("growth-operator-", ".json");
    Path processOutput = Files.createTempFile("growth-operator-process-", ".log");
    Path mcpServer = materializeMcpServer();
    try {
      List<String> command = buildCommand(output, mcpServer);
      ProcessBuilder processBuilder =
          new ProcessBuilder(command)
              .redirectErrorStream(true)
              .redirectOutput(processOutput.toFile());
      processBuilder
          .environment()
          .put("MCP_COMMERCIAL_PLAN_ID", String.valueOf(job.commercialPlanId()));
      processBuilder
          .environment()
          .put("MCP_SOURCE_EXECUTION_ID", "growth-operator-execution-" + job.id());
      processBuilder.environment().put("MCP_MARKETING_HUB_URL", properties.getMarketingHubUrl());
      Process process = processBuilder.start();
      process.getOutputStream().write(buildPrompt(job).getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      CodexTelemetryReporter.Session session =
          telemetry == null ? null : telemetry.monitor(job.id(), process, processOutput);
      try {
        if (!process.waitFor(properties.getCodexTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
          process.destroyForcibly();
          process.waitFor(10, TimeUnit.SECONDS);
          throw new IllegalStateException(
              "Timeout do Codex do Operador após "
                  + properties.getCodexTimeout().toMinutes()
                  + " minutos.");
        }
        String processLog = Files.readString(processOutput, StandardCharsets.UTF_8);
        int exitCode = process.exitValue();
        if (exitCode != 0) {
          throw new IllegalStateException(
              "Codex encerrou com codigo " + exitCode + ": " + processLog);
        }
        String rawResponse = Files.readString(output);
        JsonNode result = objectMapper.readTree(rawResponse);
        validateResult(result, strategicContractHash(job));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(
            "alternativesJson", objectMapper.writeValueAsString(result.get("alternatives")));
        payload.put("diagnosisJson", objectMapper.writeValueAsString(result.get("diagnosis")));
        payload.put("rawModelResponse", rawResponse);
        payload.put("toolUsageJson", extractToolUsage(processLog, job.id()));
        payload.put("recommendedDecision", result.get("decision").asText());
        payload.put("recommendedAction", result.get("recommendedAction").asText());
        payload.put("dailyReport", result.get("dailyReport").asText());
        payload.put(
            "model", hasText(properties.getModel()) ? properties.getModel() : "codex-default");
        payload.put("inputTokens", null);
        payload.put("outputTokens", null);
        payload.put("estimatedCost", null);
        if (session != null) session.success();
        return payload;
      } finally {
        if (session != null) session.close();
      }
    } finally {
      Files.deleteIfExists(output);
      Files.deleteIfExists(processOutput);
      Files.deleteIfExists(mcpServer);
    }
  }

  /** Bloqueia sem custo quando Atena ainda não entregou uma estratégia íntegra e operável. */
  Optional<Map<String, Object>> strategicContractGate(GrowthOperatorJob job) throws IOException {
    String reason = "Atena ainda não entregou o Contrato Estratégico de Mercado v2.";
    String availability = "MISSING";
    String contractVersion = null;
    String contentHash = null;
    try {
      JsonNode wrapper =
          objectMapper.readTree(job.evidenceSnapshot()).path("marketStrategicContract");
      JsonNode contract = wrapper.path("contract");
      contractVersion = textOrNull(wrapper.path("contractVersion").asText());
      String candidateHash = wrapper.path("contentHash").asText();
      contentHash = candidateHash.matches("[0-9a-f]{64}") ? candidateHash : null;
      if ("INSUFFICIENT_EVIDENCE".equals(contract.path("status").asText())) {
        availability = "INSUFFICIENT_EVIDENCE";
      }
      boolean ready =
          "AVAILABLE".equals(wrapper.path("availability").asText())
              && "MARKET_STRATEGY_V2".equals(wrapper.path("contractVersion").asText())
              && wrapper.path("contentHash").asText().matches("[0-9a-f]{64}")
              && "MARKET_STRATEGY_V2".equals(contract.path("contractVersion").asText())
              && "READY_FOR_OPERATION".equals(contract.path("status").asText())
              && "ATENA_DEFINES_STRATEGY_HERMES_OPERATES_GROWTH"
                  .equals(contract.path("operatorBoundary").asText());
      if (ready) return Optional.empty();
      if (wrapper.hasNonNull("reason") && !wrapper.path("reason").asText().isBlank()) {
        reason = wrapper.path("reason").asText();
      }
    } catch (com.fasterxml.jackson.core.JsonProcessingException | RuntimeException ex) {
      log.warn(
          "Snapshot estratégico ilegível no diagnóstico de Hermes; jobId={}, commercialPlanId={}",
          job.id(),
          job.commercialPlanId(),
          ex);
      reason = "O snapshot não contém um contrato estratégico legível de Atena.";
    }

    List<Map<String, Object>> alternatives =
        List.of(
            Map.of(
                "name",
                "Operar sem estratégia",
                "benefit",
                "Nenhum benefício confiável",
                "risk",
                "Hermes redefiniria mercado e oferta",
                "effort",
                "Baixo",
                "fit",
                "Rejeitada"),
            Map.of(
                "name",
                "Inferir estratégia de campos antigos",
                "benefit",
                "Evita uma nova pesquisa",
                "risk",
                "Produz autoria falsa e contexto não auditável",
                "effort",
                "Médio",
                "fit",
                "Rejeitada"),
            Map.of(
                "name",
                "Solicitar nova análise de Atena",
                "benefit",
                "Restaura estratégia versionada e evidenciada",
                "risk",
                "Adia a otimização até a conclusão",
                "effort",
                "Médio",
                "fit",
                "Selecionada"));
    Map<String, Object> strategicAssessment = new LinkedHashMap<>();
    strategicAssessment.put("availability", availability);
    strategicAssessment.put("contractVersion", contractVersion);
    strategicAssessment.put("contentHash", contentHash);
    strategicAssessment.put("strategyPreserved", true);
    strategicAssessment.put("revisionRequired", true);
    strategicAssessment.put("revisionReason", reason);
    Map<String, Object> decisionAudit =
        Map.of(
            "observedFacts", List.of(reason),
            "inferences", List.of(),
            "contradictoryEvidence", List.of(),
            "evidenceGaps", List.of("Contrato estratégico v2 operável de Atena."),
            "changeDecisionIf", List.of("Atena concluir contrato v2 READY_FOR_OPERATION íntegro."),
            "confidence", "HIGH",
            "verificationSummary",
                "O gate verificou versão, status, fronteira e SHA-256 antes de abrir o modelo.");
    Map<String, Object> diagnosis =
        Map.of(
            "rootCause",
            "Contrato Estratégico de Mercado ausente ou não operável.",
            "evidence",
            List.of(reason),
            "expectedMetric",
            "Contrato MARKET_STRATEGY_V2 íntegro e READY_FOR_OPERATION.",
            "continueCriteria",
            "Continuar somente após Atena concluir o contrato estratégico operável.",
            "adjustCriteria",
            "Solicitar revisão de Atena enquanto houver ausência, insuficiência ou divergência.",
            "stopCriteria",
            "Manter Hermes bloqueado diante de contrato inválido ou não operável.",
            "decisionAudit",
            decisionAudit);
    String recommendedAction =
        "Solicitar nova análise estratégica à Atena antes de Hermes operar crescimento.";
    Map<String, Object> raw = new LinkedHashMap<>();
    raw.put("strategicContractAssessment", strategicAssessment);
    raw.put("alternatives", alternatives);
    raw.put("diagnosis", diagnosis);
    raw.put("decision", "ADJUST");
    raw.put("recommendedAction", recommendedAction);
    raw.put(
        "dailyReport",
        "Hermes preservou a fronteira estratégica e aguarda um contrato v2 operável de Atena.");
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("alternativesJson", objectMapper.writeValueAsString(alternatives));
    payload.put("diagnosisJson", objectMapper.writeValueAsString(diagnosis));
    payload.put("rawModelResponse", objectMapper.writeValueAsString(raw));
    payload.put("toolUsageJson", "[]");
    payload.put("recommendedDecision", "ADJUST");
    payload.put("recommendedAction", recommendedAction);
    payload.put(
        "dailyReport",
        "Hermes preservou a fronteira estratégica e aguarda um contrato v2 operável de Atena.");
    payload.put("model", "market-strategy-contract-gate-v2");
    payload.put("inputTokens", 0L);
    payload.put("outputTokens", 0L);
    payload.put("estimatedCost", BigDecimal.ZERO);
    return Optional.of(Map.copyOf(payload));
  }

  /**
   * Converte texto vazio em ausência explícita para manter o diagnóstico compatível com o schema.
   */
  private String textOrNull(String value) {
    return hasText(value) ? value : null;
  }

  /** Recupera o hash íntegro já aprovado pelo gate para comparar a resposta do modelo. */
  private String strategicContractHash(GrowthOperatorJob job) throws IOException {
    String hash =
        objectMapper
            .readTree(job.evidenceSnapshot())
            .path("marketStrategicContract")
            .path("contentHash")
            .asText();
    if (!hash.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException("Diagnóstico de Hermes sem hash estratégico de Atena.");
    }
    return hash;
  }

  /** Extrai das linhas de auditoria MCP quais ferramentas fundamentaram a execucao. */
  private String extractToolUsage(String processLog, Long jobId) throws IOException {
    List<JsonNode> calls = new ArrayList<>();
    for (String line : processLog.lines().toList()) {
      if (!line.startsWith("{") || !line.contains("\"tool\"")) {
        continue;
      }
      try {
        JsonNode candidate = objectMapper.readTree(line);
        if (candidate.hasNonNull("tool") && candidate.hasNonNull("status")) {
          calls.add(candidate);
        }
      } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
        log.debug("Linha candidata à auditoria MCP não contém JSON válido; jobId={}", jobId, ex);
      }
    }
    return objectMapper.writeValueAsString(calls);
  }

  /** Monta o comando com sandbox read-only, sessao efemera e schema versionado. */
  List<String> buildCommand(Path output) throws IOException {
    return buildCommand(output, materializeMcpServer());
  }

  /** Monta o comando apontando para um servidor MCP local explicitamente informado. */
  List<String> buildCommand(Path output, Path mcpServer) throws IOException {
    List<String> command = new ArrayList<>();
    command.add(properties.getCodexCommand());
    command.add("--search");
    command.add("exec");
    command.add("-");
    command.add("--skip-git-repo-check");
    command.add("--sandbox");
    command.add("read-only");
    command.add("--cd");
    command.add(properties.getRepositoryPath());
    command.add("--output-schema");
    command.add(materializeSchema().toString());
    command.add("--output-last-message");
    command.add(output.toString());
    command.add("--color");
    command.add("never");
    command.add("--config");
    command.add("mcp_servers.marketing_hub_readonly.command=\"node\"");
    command.add("--config");
    command.add("mcp_servers.marketing_hub_readonly.args=[\"" + mcpServer.toAbsolutePath() + "\"]");
    if (hasText(properties.getReasoningEffort())) {
      command.add("--config");
      command.add("model_reasoning_effort=\"" + properties.getReasoningEffort() + "\"");
    }
    if (hasText(properties.getModel())) {
      command.add("--model");
      command.add(properties.getModel());
    }
    return command;
  }

  /** Materializa o catalogo MCP somente leitura em diretorio temporario gravavel. */
  private Path materializeMcpServer() throws IOException {
    Path server = Files.createTempFile("marketing-hub-readonly-mcp-", ".mjs");
    Files.writeString(server, readResource("mcp/marketing-hub-readonly.mjs"));
    server.toFile().deleteOnExit();
    return server;
  }

  /** Resolve o prompt versionado com o contexto congelado pelo backend. */
  private String buildPrompt(GrowthOperatorJob job) throws IOException {
    String template = readResource("prompts/growth-operator/v2/diagnosis.md");
    return template
        .replace("{{OBJECTIVE}}", text(job.objective()))
        .replace("{{BLOCKER}}", text(job.blocker()))
        .replace("{{EVIDENCE_SNAPSHOT}}", text(job.evidenceSnapshot()))
        .replace("{{PLAN_ID}}", String.valueOf(job.commercialPlanId()))
        .replace("{{MARKETING_HUB_URL}}", text(properties.getMarketingHubUrl()));
  }

  /** Materializa o schema do classpath fora do repositorio para uso pelo CLI. */
  private Path materializeSchema() throws IOException {
    Path schema = Files.createTempFile("growth-operator-schema-", ".json");
    Files.writeString(schema, readResource("prompts/growth-operator/v2/diagnosis-schema.json"));
    schema.toFile().deleteOnExit();
    return schema;
  }

  /** Le um recurso versionado integralmente. */
  private String readResource(String path) throws IOException {
    try (var input = new ClassPathResource(path).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Rejeita qualquer resposta que não preserve o contrato e a identidade estratégica recebidos. */
  void validateResult(JsonNode result, String expectedStrategicHash) {
    if (!result.has("alternatives")
        || result.get("alternatives").size() != 3
        || !result.hasNonNull("strategicContractAssessment")
        || !"AVAILABLE"
            .equals(result.get("strategicContractAssessment").path("availability").asText())
        || !"MARKET_STRATEGY_V2"
            .equals(result.get("strategicContractAssessment").path("contractVersion").asText())
        || !expectedStrategicHash.equals(
            result.get("strategicContractAssessment").path("contentHash").asText())
        || !result.get("strategicContractAssessment").hasNonNull("strategyPreserved")
        || !result.get("strategicContractAssessment").path("strategyPreserved").asBoolean()
        || !result.hasNonNull("diagnosis")
        || !result.get("diagnosis").hasNonNull("decisionAudit")
        || !result.hasNonNull("decision")
        || !result.hasNonNull("recommendedAction")
        || !result.hasNonNull("dailyReport")
        || (result.get("strategicContractAssessment").path("revisionRequired").asBoolean()
            && "CONTINUE".equals(result.path("decision").asText()))) {
      throw new IllegalArgumentException("Resposta Codex fora do contrato de diagnóstico v2.");
    }
  }

  /** Indica se um texto possui conteudo. */
  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  /** Normaliza valores ausentes no prompt. */
  private String text(String value) {
    return hasText(value) ? value : "nao informado";
  }
}
