package com.marketinghub.agenttask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.validationscope.v1.PdeValidationScopePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsabilidade: preparar somente o replay explícito de estratégia íntegra recusada pelo escopo.
 */
final class AtenaPreservedStrategyRecovery {
  private static final Logger log = LoggerFactory.getLogger(AtenaPreservedStrategyRecovery.class);

  /** Impede instâncias de uma política sem persistência ou efeitos externos. */
  private AtenaPreservedStrategyRecovery() {}

  /** Reconhece a atividade canônica sem autorizar reexecução de outros agentes ou processos. */
  static boolean supports(AgentTask task) {
    return task.getAssignedAgent() != null
        && task.getProcessDefinition() != null
        && AtenaBpmWorkerContract.supports(
            task.getAssignedAgent().getAgentKey(),
            task.getProcessDefinition().getProcessCode(),
            task.getProcessActivityId());
  }

  /** Revalida o retorno original sem alterar estado, tokens, custo, prompt ou evidência. */
  static boolean available(AgentTask task, ObjectMapper json) {
    if (!supports(task)
        || task.getExecutionError() == null
        || !task.getExecutionError()
            .contains("Atena exigiu comparação fora do protocolo da candidata")
        || task.getResultJson() == null
        || task.getEvidenceJson() == null
        || task.getExecutionActivityPrompt() == null
        || !"MODEL".equals(task.getExecutionMode())) return false;
    try {
      String prompt = task.getExecutionActivityPrompt();
      String marker = "Contexto da tarefa:";
      int offset = prompt.indexOf(marker);
      if (offset < 0) return false;
      JsonNode original = json.readTree(prompt.substring(offset + marker.length()).stripLeading());
      JsonNode cycle =
          json.readTree(original.path("processContextJson").asText()).path("learningSalesCycle");
      JsonNode protocol = cycle.path("agentValidationExecution");
      JsonNode result = json.readTree(task.getResultJson());
      JsonNode evidence = json.readTree(task.getEvidenceJson());
      var identity = cycle.deepCopy();
      ((com.fasterxml.jackson.databind.node.ObjectNode) identity)
          .put("prototypeVersion", cycle.path("productVersion").asText());
      return original.path("taskId").asLong(-1) == task.getId()
          && task.getSourceReference().equals(original.path("sourceReference").asText())
          && task.getSourceReference()
              .equals("experiment:" + protocol.path("experimentId").asLong(-1))
          && original.path("processVersion").asInt(-1)
              == task.getProcessDefinition().getVersionNumber()
          && original.path("taskTarget").path("productId").asLong(-1)
              == protocol.path("productId").asLong(-2)
          && "PDE_VALIDATION_SCOPE_V1".equals(protocol.path("scopeContractVersion").asText())
          && protocol.path("inputComparisonApplicable").isBoolean()
          && !protocol.path("inputComparisonApplicable").asBoolean()
          && !protocol.has("documentedInputComparison")
          && PdeValidationScopePolicy.sameCandidate(identity, protocol)
          && "APPROVE".equals(result.path("decision").asText())
          && "marketStrategy".equals(result.path("activity").asText())
          && "MARKET_STRATEGY_V4"
              .equals(result.path("marketStrategicContract").path("contractVersion").asText())
          && "READY_FOR_AGENT_VALIDATION"
              .equals(result.path("marketStrategicContract").path("status").asText())
          && "Atena".equals(evidence.path("agent").asText())
          && task.getSourceReference().equals(evidence.path("sourceReference").asText())
          && PdeValidationScopePolicy.violation(result, protocol).isEmpty();
    } catch (Exception ex) {
      log.warn(
          "Recuperação de estratégia recusada taskId={} sourceReference={}",
          task.getId(),
          task.getSourceReference(),
          ex);
      return false;
    }
  }

  /** Exige os mesmos bytes no callback recuperado para impedir substituição do parecer pago. */
  static void requireOriginal(AgentTask task, CompleteAgentTaskRequest request, ObjectMapper json) {
    if (!supports(task)
        || task.getExecutionError() == null
        || !task.getExecutionError().startsWith("AUTO_RETRY_ONCE|")) return;
    if (!available(task, json)
        || !task.getResultJson().equals(request.resultJson())
        || !task.getEvidenceJson().equals(request.evidenceJson())
        || request.modelUsages() != null && !request.modelUsages().isEmpty()
        || request.executionAudit() != null)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.CONFLICT,
          "A recuperação deve reutilizar a análise e sua auditoria originais, sem nova cobrança.");
  }

  /** Mantém auditoria e consumo originais também quando o executor recusa o replay incompleto. */
  static FailAgentTaskRequest preserveFailure(AgentTask task, FailAgentTaskRequest request) {
    if (!supports(task) || task.getExecutionError() == null) return request;
    boolean recovering = task.getExecutionError().startsWith("AUTO_RETRY_ONCE|");
    boolean repeated =
        "BLOCKED".equals(task.getStatus())
            && task.getExecutionError().equals(request.error())
            && task.getResultJson() != null
            && task.getEvidenceJson() != null;
    if (!recovering && !repeated) return request;
    if (request.resultJson() != null && !request.resultJson().equals(task.getResultJson())
        || request.evidenceJson() != null && !request.evidenceJson().equals(task.getEvidenceJson())
        || request.modelUsages() != null && !request.modelUsages().isEmpty()
        || request.executionAudit() != null)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.CONFLICT,
          "A recuperação recusada deve preservar a análise, a auditoria e o custo originais.");
    return new FailAgentTaskRequest(
        request.error(),
        task.getResultJson(),
        task.getEvidenceJson(),
        null,
        null,
        request.blockerGuidance());
  }

  /**
   * Reconhece a mesma recusa já persistida sem exigir reenvio ou substituição do prompt auditado.
   */
  static boolean repeatedFailure(AgentTask task, FailAgentTaskRequest request) {
    return supports(task)
        && "BLOCKED".equals(task.getStatus())
        && java.util.Objects.equals(task.getExecutionError(), request.error())
        && java.util.Objects.equals(task.getResultJson(), request.resultJson())
        && java.util.Objects.equals(task.getEvidenceJson(), request.evidenceJson())
        && request.executionAudit() == null
        && (request.modelUsages() == null || request.modelUsages().isEmpty());
  }

  /** Recusa replay se identidade, capacidade ou direção estratégica mudaram desde a análise. */
  static void requireCurrentContext(
      AgentTask task, AgentTaskTargetResponse target, String contextJson, ObjectMapper json) {
    try {
      String prompt = task.getExecutionActivityPrompt();
      String marker = "Contexto da tarefa:";
      JsonNode original =
          json.readTree(prompt.substring(prompt.indexOf(marker) + marker.length()).stripLeading());
      JsonNode before = json.readTree(original.path("processContextJson").asText());
      JsonNode current = json.readTree(contextJson);
      JsonNode cycle = before.path("learningSalesCycle");
      JsonNode now = current.path("learningSalesCycle");
      boolean same =
          available(task, json)
              && target != null
              && target.productId() != null
              && target.productId() == cycle.path("productId").asLong(-1)
              && target.experimentId() != null
              && target.experimentId() == cycle.path("experimentId").asLong(-1);
      for (String field :
          new String[] {
            "productId",
            "cycleId",
            "experimentId",
            "productVersion",
            "agentValidationExecution",
            "implementedInput"
          }) same &= cycle.path(field).equals(now.path(field));
      same &= before.path("productBigIdea").equals(current.path("productBigIdea"));
      same &=
          modelingAuthority(before.path("referenceModeling"), task.getId())
              .equals(modelingAuthority(current.path("referenceModeling"), task.getId()));
      if (same) return;
    } catch (Exception ex) {
      log.warn(
          "Contexto da recuperação de Atena divergente taskId={} sourceReference={}",
          task.getId(),
          task.getSourceReference(),
          ex);
    }
    throw new org.springframework.web.server.ResponseStatusException(
        org.springframework.http.HttpStatus.CONFLICT,
        "O contexto mudou desde a análise paga. Confira a versão e os critérios antes de"
            + " reaproveitá-la.");
  }

  /**
   * Desconsidera somente o status operacional da própria tarefa, sem ocultar fontes ou decisões.
   */
  private static JsonNode modelingAuthority(JsonNode modeling, long taskId) {
    JsonNode copy = modeling.deepCopy();
    JsonNode decision = copy.path("strategicDecision");
    if (decision.isObject() && decision.path("taskId").asLong(-1) == taskId)
      ((com.fasterxml.jackson.databind.node.ObjectNode) decision).remove("status");
    return copy;
  }
}
