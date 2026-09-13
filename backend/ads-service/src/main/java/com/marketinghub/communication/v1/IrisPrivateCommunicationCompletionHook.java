package com.marketinghub.communication.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: impedir aprovação de comunicação privada com entradas substituídas durante a
 * tarefa.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IrisPrivateCommunicationCompletionHook implements AgentTaskCompletionHook {
  private final IrisPrivateProductContext context;
  private final ObjectMapper json;

  /** Protege a mensagem privada; a produção visual conserva seu próprio handler de conclusão. */
  @Override
  public boolean supports(AgentTask task) {
    return IrisPrivateProductContext.supports(task.getSourceReference())
        && task.getProcessDefinition() != null
        && "pde-communication-sales-journey".equals(task.getProcessDefinition().getProcessCode())
        && "communicationContract".equals(task.getProcessActivityId())
        && task.getAssignedAgent() != null
        && "communication-director".equals(task.getAssignedAgent().getAgentKey());
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
