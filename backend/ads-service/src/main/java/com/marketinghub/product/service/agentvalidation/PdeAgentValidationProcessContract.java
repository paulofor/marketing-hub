package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/** Reconhece revisões compatíveis do contrato de homologação e retrabalho do PDE. */
@Slf4j
final class PdeAgentValidationProcessContract {
  private static final Set<String> REVIEWS =
      Set.of(
          "technicalHomologation",
          "psiqueAdherent",
          "psiqueRecovery",
          "psiqueSafety",
          "commercialIntegrityReview");

  /** Impede instâncias de um verificador sem estado. */
  private PdeAgentValidationProcessContract() {}

  /** Preserva a versão original e exige o contrato explícito nas revisões posteriores. */
  static boolean supports(BusinessProcessDefinition process, ObjectMapper json) {
    if (process == null
        || !PdeAgentValidationGateActivityExecutor.PROCESS_CODE.equals(process.getProcessCode())
        || process.getVersionNumber() == null
        || process.getVersionNumber() < 8) return false;
    if (process.getVersionNumber() == 8) return true;
    if (process.getDiagramJson() == null || process.getDiagramJson().isBlank()) return false;
    try {
      JsonNode diagram = json.readTree(process.getDiagramJson());
      JsonNode nodes = diagram.path("nodes");
      if (!nodes.isArray()) return false;
      JsonNode correction = null;
      JsonNode gate = null;
      Set<String> activityIds = new HashSet<>();
      for (JsonNode node : nodes) {
        if (!"TASK".equals(node.path("type").asText())) continue;
        String id = node.path("id").asText();
        if (!activityIds.add(id)) return false;
        if ("prototypeCorrection".equals(id)) correction = node;
        if ("agentValidationGate".equals(id)) gate = node;
      }
      if (!activityIds.containsAll(REVIEWS) || correction == null || gate == null) return false;
      Set<String> remediates = new HashSet<>();
      correction.path("remediatesActivities").forEach(value -> remediates.add(value.asText()));
      return "ON_FUNCTIONAL_REJECTION".equals(correction.path("activationMode").asText())
          && "PDE_FUNCTIONAL_REWORK".equals(correction.path("responsibilityDomain").asText())
          && remediates.equals(REVIEWS)
          && "DETERMINISTIC".equals(gate.path("executionMode").asText())
          && "PDE_AGENT_VALIDATION_GATE".equals(gate.path("responsibilityDomain").asText());
    } catch (JsonProcessingException ex) {
      log.error(
          "Contrato de homologação PDE inválido. processDefinitionId={} version={}",
          process.getId(),
          process.getVersionNumber(),
          ex);
      return false;
    }
  }
}
