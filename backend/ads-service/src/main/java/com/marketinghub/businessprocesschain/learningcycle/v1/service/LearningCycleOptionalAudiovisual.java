package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.agenttask.BusinessProcessOptionalActivity;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: comprovar a dispensa vigente de audiovisual sem dispensar os demais gates. */
@Component
@RequiredArgsConstructor
public class LearningCycleOptionalAudiovisual {
  private static final String COMMUNICATION = "pde-communication-sales-journey";
  private static final String CREATIVE = "creative-production-approval";
  private final BusinessProcessActivityInstanceRepository instances;
  private final AgentTaskRepository tasks;
  private final BusinessProcessChainDefinitionRepository chains;
  private final LearningCycleJson json;

  /**
   * Confere tarefa, formato, versão e ocorrência atuais; ausência ou adiamento nunca dispensam
   * vídeo.
   */
  public Optional<ObjectNode> proof(LearningSalesCycle cycle) {
    if (cycle.getProductVersion() == null || cycle.getVersionChangedAt() == null)
      return Optional.empty();
    String reference = "experiment:" + cycle.getExperimentId();
    var communication =
        tasks
            .findFunctionalSnapshots(reference, List.of(COMMUNICATION), cycle.getVersionChangedAt())
            .stream()
            .filter(t -> "communicationContract".equals(t.processActivityId()))
            .findFirst()
            .orElse(null);
    if (communication == null
        || communication.createdAt() == null
        || communication.resultJson() == null
        || !"COMPLETED".equals(communication.status())
        || !"communication-director".equals(communication.agentKey())) return Optional.empty();
    var chain = chains.findById(cycle.getChainDefinitionId()).orElse(null);
    if (chain == null
        || chain.getItems().stream()
            .noneMatch(
                i ->
                    Objects.equals(
                            i.getProcessDefinition().getId(), communication.processDefinitionId())
                        && COMMUNICATION.equals(i.getProcessDefinition().getProcessCode())))
      return Optional.empty();
    JsonNode output = json.read(communication.resultJson()).path("functionalOutput");
    if (!output.isObject()
        || !output.has("audiovisualBrief")
        || !(output.path("audiovisualBrief").isNull()
            || (output.path("audiovisualBrief").isTextual()
                && output.path("audiovisualBrief").asText().isBlank()))) return Optional.empty();
    var history =
        instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                CREATIVE, reference);
    var route = latest(history, "route");
    var omitted = latest(history, "audiovisual");
    if (route == null
        || omitted == null
        || !route.isObjectiveAchieved()
        || !"COMPLETED".equals(route.getStatus())
        || !BusinessProcessOptionalActivity.isOmitted(omitted)
        || !Objects.equals(
            route.getActivityDefinition().getProcessDefinition().getId(),
            omitted.getActivityDefinition().getProcessDefinition().getId())
        || route.getExitedAt() == null
        || route.getExitedAt().isBefore(communication.createdAt())
        || omitted.getExitedAt() == null
        || omitted.getExitedAt().isBefore(route.getExitedAt())) return Optional.empty();
    JsonNode decision = json.read(route.getObjectiveEvidenceJson());
    JsonNode omission = json.read(omitted.getObjectiveEvidenceJson());
    if (!"COMMUNICATION_FORMATS_V1".equals(decision.path("evidenceType").asText())
        || !matches(cycle, communication.id(), reference, decision)
        || !matches(cycle, communication.id(), reference, omission)) return Optional.empty();
    var proof = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
    proof.put("contractVersion", "LEARNING_CYCLE_OPTIONAL_AUDIOVISUAL_V1");
    proof.put("productId", cycle.getProductId());
    proof.put("cycleId", cycle.getId());
    proof.put("experimentId", cycle.getExperimentId());
    proof.put("productVersion", cycle.getProductVersion());
    proof.put("sourceReference", reference);
    proof.put("communicationTaskId", communication.id());
    proof.put("formatInstanceId", route.getId());
    proof.put("omissionInstanceId", omitted.getId());
    proof.put("audiovisualRequired", false);
    proof.put("publicationAuthorized", false);
    proof.put("spendAuthorized", false);
    return Optional.of(proof);
  }

  /** Aceita somente negações booleanas explícitas e a versão congelada na preparação da peça. */
  private boolean matches(LearningSalesCycle cycle, Long taskId, String reference, JsonNode data) {
    return Objects.equals(reference, data.path("sourceReference").asText())
        && data.path("communicationTaskId").asLong(-1) == taskId
        && isFalse(data, "audiovisualRequired")
        && isFalse(data, "audiovisualPlanned")
        && isFalse(data, "audiovisualProductionDeferred")
        && cycle
            .getProductVersion()
            .equals(data.path("privateCreativePreparation").path("prototypeVersion").asText());
  }

  /** Impede coerção de texto, número ou campo ausente em uma dispensa. */
  private boolean isFalse(JsonNode data, String field) {
    return data.path(field).isBoolean() && !data.path(field).booleanValue();
  }

  /** Usa a ocorrência mais recente, inclusive quando ela revoga uma decisão antes aceita. */
  private BusinessProcessActivityInstance latest(
      List<BusinessProcessActivityInstance> history, String activity) {
    return history.stream()
        .filter(
            i ->
                i.getActivityDefinition() != null
                    && activity.equals(i.getActivityDefinition().getActivityId()))
        .findFirst()
        .orElse(null);
  }
}
