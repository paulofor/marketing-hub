package com.marketinghub.agenttask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;

/** Responsabilidade: reconhecer a especificação privada impedida apenas pelo shell do modelo. */
final class IrisRenderPlanRecovery {
  static final String NAMESPACE_DENIED = "bwrap: No permissions to create a new namespace";
  private static final Set<String> MODES =
      Set.of("LEARNING_CYCLE_PRIVATE", "PRODUCT_PRIVATE", "INITIAL_EXPERIMENT_PRIVATE");

  /** Impede instanciar uma política determinística sem estado. */
  private IrisRenderPlanRecovery() {}

  /** Preserva o parecer original e entrega somente um plano pronto para o executor técnico. */
  static ObjectNode pendingPlan(AgentTask task, JsonNode current, ObjectMapper json)
      throws java.io.IOException {
    if (task.getAssignedAgent() == null
        || !"communication-director".equals(task.getAssignedAgent().getAgentKey())
        || task.getProcessDefinition() == null
        || !"creative-production-approval".equals(task.getProcessDefinition().getProcessCode())
        || !"nonAudiovisual".equals(task.getProcessActivityId())
        || task.getResultJson() == null
        || task.getEvidenceJson() == null
        || ("BLOCKED".equals(task.getStatus())
            && task.getExecutionError() != null
            && task.getExecutionError().startsWith("AUTO_RETRY_"))) return null;
    JsonNode result = json.readTree(task.getResultJson());
    JsonNode audit = json.readTree(task.getEvidenceJson()).path("communicationInputReference");
    if (!validPrivate(current, task.getSourceReference())
        || !validPrivate(audit, task.getSourceReference())
        || !current.path("prototypeVersion").equals(audit.path("prototypeVersion"))
        || !current
            .path("marketStrategicContract")
            .path("contentHash")
            .asText()
            .matches("[0-9a-f]{64}")
        || !current
            .path("marketStrategicContract")
            .path("contentHash")
            .equals(audit.path("marketStrategicContract").path("contentHash"))
        || !current
            .path("marketStrategicContract")
            .path("contentHash")
            .equals(result.path("strategicContractReference").path("contentHash"))
        || !result.path("strategicContractReference").path("preserved").asBoolean(false)
        || !"IRIS_COMMUNICATION_V1".equals(result.path("contractVersion").asText())
        || !"BLOCKED".equals(result.path("executionStatus").asText())
        || !"NON_AUDIOVISUAL_PACKAGE".equals(result.path("outputType").asText())
        || !"nonAudiovisual".equals(result.path("activityId").asText())
        || !task.getSourceReference().equals(result.path("sourceReference").asText())
        || result.path("alternatives").size() != 3
        || result.path("chosenAlternative").asText().isBlank()
        || !result.path("evidenceGaps").isArray()
        || result.path("evidenceGaps").size() != 1
        || !result.path("evidenceGaps").get(0).asText().contains(NAMESPACE_DENIED)
        || !"BACKEND".equals(result.path("nextHandoff").asText())) return null;
    for (String field :
        Set.of(
            "strategyPreserved",
            "productPreserved",
            "pricePreserved",
            "noFabricatedProof",
            "noPublication",
            "noExternalSpend"))
      if (!result.path("guardrails").path(field).asBoolean(false)) return null;
    var output = result.path("functionalOutput");
    var assets = output.path("staticAssets");
    if (!assets.isArray()
        || assets.isEmpty()
        || !output.path("renderedAssets").isEmpty()
        || output.path("copy").path("headline").asText().isBlank()) return null;
    for (String field :
        Set.of("expectedMetric", "continueCriteria", "adjustCriteria", "stopCriteria"))
      if (result.path(field).asText().isBlank()) return null;
    for (var asset : assets) {
      var spec = asset.path("renderSpec");
      if (!"PROOF_CARD_V1".equals(spec.path("templateVersion").asText())
          || spec.path("sourceArtifactId").asLong() <= 0
          || !spec.path("sourceSha256").asText().matches("[0-9a-f]{64}")) return null;
      for (String field : Set.of("brandLabel", "headline", "body", "ctaText"))
        if (spec.path(field).asText().isBlank()) return null;
      for (String field : Set.of("x", "y", "width", "height"))
        if (!spec.path("crop").path(field).isIntegralNumber()) return null;
      if (spec.path("crop").path("x").asInt() < 0
          || spec.path("crop").path("y").asInt() < 0
          || spec.path("crop").path("width").asInt() <= 0
          || spec.path("crop").path("height").asInt() <= 0) return null;
    }
    ObjectNode plan = result.deepCopy();
    plan.put("executionStatus", "READY_FOR_RENDER");
    plan.putArray("evidenceGaps");
    return plan;
  }

  /** Recusa preparação vencida, divergente ou com publicação, cobrança e vídeo autorizados. */
  private static boolean validPrivate(JsonNode context, String reference) {
    var contract = context.path("privateCreativePreparation");
    return reference != null
        && reference.equals(context.path("sourceReference").asText())
        && "AVAILABLE".equals(context.path("availability").asText())
        && "READY".equals(context.path("inputReadiness").asText())
        && MODES.contains(context.path("mode").asText())
        && !context.path("prototypeVersion").asText().isBlank()
        && "PDE_PRIVATE_CREATIVE_PREPARATION_V1".equals(contract.path("contractVersion").asText())
        && "PRIVATE_PREPARATION".equals(contract.path("scope").asText())
        && reference.equals(contract.path("sourceReference").asText())
        && context
            .path("prototypeVersion")
            .asText()
            .equals(contract.path("prototypeVersion").asText())
        && contract.path("nonAudiovisualEvidenceRequired").asBoolean(false)
        && "INDEPENDENT_REVIEW".equals(contract.path("nonAudiovisualEvidencePurpose").asText())
        && "PROOF_CARD_V1".equals(contract.path("nonAudiovisualTemplate").asText())
        && contract.path("commercialFormatDecisionPreserved").asBoolean(false)
        && "BRIEF_ONLY".equals(contract.path("audiovisualProductionIntent").asText())
        && contract.path("videoProductionRequestId").isIntegralNumber()
        && contract.path("videoProductionRequestId").asLong() == 0
        && !context.path("publicationAuthorized").asBoolean(true)
        && !context.path("paymentEnabled").asBoolean(true)
        && !context.path("externalMediaSpendAuthorized").asBoolean(true)
        && !contract.path("publicationAuthorized").asBoolean(true)
        && !contract.path("spendAuthorized").asBoolean(true)
        && !contract.path("commercialEvidenceClaimed").asBoolean(true);
  }
}
