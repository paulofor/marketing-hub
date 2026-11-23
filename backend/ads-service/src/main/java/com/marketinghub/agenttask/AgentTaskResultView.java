package com.marketinghub.agenttask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: extrair seções tipadas do parecer funcional persistido na tarefa. */
public final class AgentTaskResultView {
  private static final Logger log = LoggerFactory.getLogger(AgentTaskResultView.class);
  private static final ObjectMapper JSON = new ObjectMapper();

  /** Impede instanciação do conversor sem estado. */
  private AgentTaskResultView() {}

  /** Projeta interrupções conhecidas em linguagem de negócio, preservando o erro na auditoria. */
  public static String businessReason(String reason) {
    if (reason != null && reason.contains(AgentTaskResultRejectedException.CODE + "|"))
      return "A resposta foi preservada, mas seu vínculo com a especificação não foi aceito. "
          + "O contrato precisa ser corrigido e o retorno original revalidado, sem nova chamada ao modelo. "
          + "Isso ainda não comprova implementação ou homologação do produto.";
    if (reason != null && reason.contains("DEDALO_LEASE_RECOVERY_EXHAUSTED|"))
      return "O acompanhamento da execução foi interrompido. Uma nova chamada foi bloqueada para evitar cobrança duplicada. É necessário conferir o recebimento e o retorno original antes de retomar; o consumo ainda não foi confirmado.";
    if (reason != null
        && (reason.contains("input_too_large") || reason.contains("PDE_CONTEXT_BUDGET_V1"))) {
      return "A execução foi interrompida porque o contexto excedeu o limite permitido. "
          + "A preparação do contexto precisa ser corrigida antes da retomada. "
          + "O diagnóstico técnico permanece disponível na tarefa; o consumo ainda precisa ser conferido.";
    }
    return reason;
  }

  /** Apresenta a síntese do agente e identifica recortes legados sem reescrever o resultado. */
  public static AgentTaskHumanSummary humanSummary(AgentTask task) {
    if (task != null
        && "BLOCKED".equals(task.getStatus())
        && task.getExecutionError() != null
        && task.getExecutionError().startsWith(AgentTaskResultRejectedException.CODE + "|"))
      return new AgentTaskHumanSummary(
          businessReason(task.getExecutionError()), "DETERMINISTIC", "BLOCKED");
    if (task != null
        && "BLOCKED".equals(task.getStatus())
        && (task.getResultJson() == null || task.getResultJson().isBlank())
        && task.getExecutionError() != null
        && task.getExecutionError().startsWith("DEDALO_LEASE_RECOVERY_EXHAUSTED|"))
      return new AgentTaskHumanSummary(
          businessReason(task.getExecutionError()), "DETERMINISTIC", "BLOCKED");
    if (task != null
        && "BLOCKED".equals(task.getStatus())
        && (task.getResultJson() == null || task.getResultJson().isBlank())
        && task.getExecutionError() != null
        && (task.getExecutionError().contains("input_too_large")
            || task.getExecutionError().contains("PDE_CONTEXT_BUDGET_V1")))
      return new AgentTaskHumanSummary(
          "A execução foi interrompida porque o contexto excedeu o limite permitido. Nenhuma entrega foi produzida. É necessário corrigir a preparação do contexto antes de retomar; o consumo permanece sujeito à conferência.",
          "DETERMINISTIC",
          "BLOCKED");
    if (task == null || task.getResultJson() == null || task.getResultJson().isBlank()) return null;
    try {
      JsonNode result = JSON.readTree(task.getResultJson());
      String decision = result.path("decision").asText("");
      String generated = result.path("humanSummary").asText("").trim();
      if (generated.length() >= 20 && generated.length() <= 420)
        return new AgentTaskHumanSummary(generated, "AGENT", decision);
      JsonNode checks = result.path("agentValidationChecks");
      if ("PDE_TEMIS_AGENT_VALIDATION_V1".equals(result.path("contractVersion").asText())
          && java.util.Set.of("BLOCKED", "ADJUST").contains(decision)
          && checks.size() == 13
          && checks.path("technicalHarnessPassed").isBoolean()
          && !checks.path("technicalHarnessPassed").asBoolean()
          && java.util.stream.StreamSupport.stream(
                  java.util.Spliterators.spliteratorUnknownSize(checks.fields(), 0), false)
              .filter(entry -> !"technicalHarnessPassed".equals(entry.getKey()))
              .allMatch(entry -> entry.getValue().isBoolean() && entry.getValue().asBoolean()))
        return new AgentTaskHumanSummary(
            "Têmis pediu provas adicionais dos testes do aplicativo. Esse parecer ainda não aprova a etapa; o complemento precisa passar por uma nova revisão.",
            "DETERMINISTIC",
            decision);
      for (String field :
          java.util.List.of(
              "summary", "commercialRationale", "customerPerspective", "rootCause", "rationale")) {
        String text = result.path(field).asText("").replaceAll("\\s+", " ").trim();
        if (!text.isBlank()) {
          if (text.length() > 420) {
            int stop = text.lastIndexOf(' ', 417);
            text = text.substring(0, stop > 0 ? stop : 417) + "…";
          }
          return new AgentTaskHumanSummary(text, "LEGACY_EXCERPT", decision);
        }
      }
      return null;
    } catch (Exception ex) {
      log.warn("Resultado inválido ao consultar síntese humana. taskId={}", task.getId(), ex);
      return null;
    }
  }

  /** Devolve uma seção objetiva do resultado sem inferir conteúdo ausente ou inválido. */
  public static JsonNode section(AgentTask task, String fieldName) {
    if (task == null
        || task.getResultJson() == null
        || task.getResultJson().isBlank()
        || fieldName == null
        || fieldName.isBlank()) {
      return null;
    }
    try {
      JsonNode value = JSON.readTree(task.getResultJson()).path(fieldName);
      return value.isObject() || value.isArray() ? value.deepCopy() : null;
    } catch (Exception ex) {
      log.warn(
          "Resultado funcional inválido ao extrair seção. taskId={} fieldName={}",
          task.getId(),
          fieldName,
          ex);
      return null;
    }
  }
}
