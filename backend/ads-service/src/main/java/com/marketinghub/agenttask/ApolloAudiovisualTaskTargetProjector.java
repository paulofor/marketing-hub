package com.marketinghub.agenttask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import java.util.Comparator;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: projetar para Apolo a decisão audiovisual exata do fluxo que criou a tarefa.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApolloAudiovisualTaskTargetProjector {
  static final String CONTRACT_VERSION = "APOLLO_COMMUNICATION_AUDIOVISUAL_INPUT_V1";
  private static final String CREATIVE_PROCESS = "creative-production-approval";
  private static final String ROUTE_ACTIVITY = "route";
  private final BusinessProcessActivityInstanceRepository instances;
  private final ObjectMapper json;

  /**
   * Substitui o contexto amplo pelo contrato mínimo da rota criativa sem alterar a identidade do
   * alvo.
   */
  public AgentTaskTargetResponse project(AgentTask task, AgentTaskTargetResponse target) {
    if (!supports(task) || target == null || instances == null) return target;
    var route =
        instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                CREATIVE_PROCESS, task.getSourceReference())
            .stream()
            .filter(
                instance ->
                    instance.getActivityDefinition() != null
                        && instance.getActivityDefinition().getProcessDefinition() != null
                        && Objects.equals(
                            task.getProcessDefinition().getId(),
                            instance.getActivityDefinition().getProcessDefinition().getId())
                        && ROUTE_ACTIVITY.equals(instance.getActivityDefinition().getActivityId()))
            .max(Comparator.comparing(BusinessProcessActivityInstance::getId));
    if (route.isEmpty()) return target;
    try {
      var instance = route.orElseThrow();
      var evidence = json.readTree(instance.getObjectiveEvidenceJson());
      if (!"COMPLETED".equals(instance.getStatus())
          || !instance.isObjectiveAchieved()
          || evidence == null
          || !evidence.isObject()
          || !"COMMUNICATION_FORMATS_V1".equals(evidence.path("evidenceType").asText())
          || !Objects.equals(
              task.getSourceReference(), evidence.path("sourceReference").asText(null))
          || !evidence.path("audiovisualRequired").isBoolean()
          || evidence.path("communicationTaskId").asLong() <= 0
          || evidence.path("publicationAuthorized").asBoolean(true)
          || evidence.path("spendAuthorized").asBoolean(true)) return target;

      ObjectNode context = json.createObjectNode();
      context.put("contractVersion", CONTRACT_VERSION);
      ObjectNode communication = context.putObject("communicationMaterialization");
      communication.put("sourceContractVersion", "COMMUNICATION_FORMATS_V1");
      communication.put("sourceReference", task.getSourceReference());
      communication.put("routeInstanceId", instance.getId());
      communication.put("communicationTaskId", evidence.path("communicationTaskId").asLong());
      communication.put("audiovisualRequired", evidence.path("audiovisualRequired").booleanValue());
      communication.put("publicationAuthorized", false);
      communication.put("spendAuthorized", false);
      return copyWithContext(target, context);
    } catch (Exception ex) {
      log.error(
          "Falha ao projetar decisão audiovisual criativa. taskId={} processDefinitionId={} sourceReference={}",
          task.getId(),
          task.getProcessDefinition().getId(),
          task.getSourceReference(),
          ex);
      return target;
    }
  }

  /** Reconhece somente a atividade audiovisual do subprocesso de produção criativa. */
  private boolean supports(AgentTask task) {
    return task != null
        && task.getProcessDefinition() != null
        && CREATIVE_PROCESS.equals(task.getProcessDefinition().getProcessCode())
        && "audiovisual".equals(task.getProcessActivityId());
  }

  /** Conserva todos os campos de identidade e troca somente o contexto funcional projetado. */
  private AgentTaskTargetResponse copyWithContext(
      AgentTaskTargetResponse target, ObjectNode context) {
    return new AgentTaskTargetResponse(
        target.sourceReference(),
        target.experimentId(),
        target.productId(),
        target.productSlug(),
        target.productName(),
        target.productInternalName(),
        target.experienceVersion(),
        target.publicUrl(),
        target.commercialCheckoutProvider(),
        target.commercialCheckoutReference(),
        target.commercialCheckoutUrl(),
        target.unitPriceBrl(),
        context);
  }
}
