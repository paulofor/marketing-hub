package com.marketinghub.communication.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: impedir aprovação de comunicação privada ou inicial com entradas substituídas
 * durante a tarefa.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IrisPrivateCommunicationCompletionHook implements AgentTaskCompletionHook {
  private final CommunicationMaterializationContextProvider context;
  private final ObjectMapper json;

  /** Protege a mensagem privada; a produção visual conserva seu próprio handler de conclusão. */
  @Override
  public boolean supports(AgentTask task) {
    return task.getProcessDefinition() != null
        && "pde-communication-sales-journey".equals(task.getProcessDefinition().getProcessCode())
        && "communicationContract".equals(task.getProcessActivityId())
        && task.getAssignedAgent() != null
        && "communication-director".equals(task.getAssignedAgent().getAgentKey())
        && (IrisPrivateProductContext.supports(task.getSourceReference())
            || isInitialExperiment(task.getSourceReference()));
  }

  /** Distingue o primeiro experimento dos demais contratos comerciais com a mesma referência. */
  private boolean isInitialExperiment(String sourceReference) {
    if (sourceReference == null || !sourceReference.matches("experiment:[1-9][0-9]*")) return false;
    return context
        .resolve(sourceReference)
        .map(
            value ->
                IrisCommunicationMaterializationContextProvider.INITIAL_EXPERIMENT_PRIVATE_MODE
                    .equals(value.get("mode")))
        .orElse(false);
  }

  /**
   * Compara as entradas no formato JSON transportado, preservando valores e ignorando tipos JPA.
   */
  @Override
  public CompletionDisposition apply(AgentTask task, CompleteAgentTaskRequest request) {
    try {
      var current =
          json.readTree(
              json.writeValueAsString(context.resolve(task.getSourceReference()).orElseThrow()));
      var supplied = json.readTree(request.evidenceJson()).path("communicationInputReference");
      if (!"READY".equals(current.path("inputReadiness").asText()) || !supplied.isObject())
        throw new IllegalArgumentException(
            "A comunicação privada não possui entrada aprovada auditável.");
      String currentMode = current.path("mode").asText();
      String suppliedMode = supplied.path("mode").asText();
      if (IrisCommunicationMaterializationContextProvider.INITIAL_EXPERIMENT_PRIVATE_MODE.equals(
              currentMode)
          || IrisCommunicationMaterializationContextProvider.INITIAL_EXPERIMENT_PRIVATE_MODE.equals(
              suppliedMode)) {
        String currentHash = current.path("communicationInputHash").asText();
        if (!currentHash.matches("[0-9a-f]{64}")
            || !supplied.path("communicationInputHash").asText().matches("[0-9a-f]{64}")
            || !IrisCommunicationInputFingerprint.equivalent(json, current, supplied)) {
          throw new IllegalArgumentException(
              "A versão ou a autorização visual mudou durante a comunicação inicial.");
        }
        return CompletionDisposition.COMPLETE;
      }
      if (!IrisPrivateProductContext.supports(task.getSourceReference())) {
        return CompletionDisposition.COMPLETE;
      }
      for (String key :
          List.of(
              "sourceReference",
              "mode",
              "prototypeVersion",
              "marketStrategicContract",
              "privatePrototypeAcceptance",
              "validationGate",
              "gateInstanceId",
              "discoveryLineage"))
        if (current.path(key).isMissingNode() || !current.path(key).equals(supplied.path(key)))
          throw new IllegalArgumentException(
              "A entrada privada mudou durante a tarefa: "
                  + key
                  + ". Atualize o contexto antes de repetir.");
      return CompletionDisposition.COMPLETE;
    } catch (Exception ex) {
      log.error(
          "Callback de comunicação privada inválido. taskId={} sourceReference={}",
          task.getId(),
          task.getSourceReference(),
          ex);
      throw new IllegalArgumentException(
          "Não foi possível comprovar a entrada vigente da comunicação privada.", ex);
    }
  }
}
