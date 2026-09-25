package com.marketinghub.communication.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/** Responsabilidade: reconhecer revisões estruturalmente compatíveis da jornada comercial PDE. */
@Slf4j
final class PrivateCommunicationProcessContract {
  private static final String PROCESS_CODE = "pde-communication-sales-journey";
  private static final String CRITERIA_VERSION = "PDE_COMMERCIAL_PRINCIPLES_V1";
  private static final Set<String> NODE_IDS =
      Set.of(
          "start",
          "communicationContract",
          "creatives",
          "destination",
          "integration",
          "gate",
          "end");
  private static final Set<String> FLOWS =
      Set.of(
          "start->communicationContract",
          "communicationContract->creatives",
          "creatives->destination",
          "destination->integration",
          "integration->gate",
          "gate->end");

  /** Impede instâncias de um verificador sem estado. */
  private PrivateCommunicationProcessContract() {}

  /**
   * Aceita a prova anterior somente quando origem e destino preservam o mesmo contrato executável.
   */
  static boolean supports(
      BusinessProcessDefinition source,
      BusinessProcessDefinition target,
      ObjectMapper objectMapper) {
    if (!identity(source)
        || !identity(target)
        || source.getVersionNumber() > target.getVersionNumber()) return false;
    try {
      return structure(source, objectMapper)
          && structure(target, objectMapper)
          && Objects.equals(
              acquisitionPolicy(source, objectMapper), acquisitionPolicy(target, objectMapper));
    } catch (JsonProcessingException ex) {
      log.error(
          "Contrato da jornada comercial PDE inválido. sourceProcessDefinitionId={} targetProcessDefinitionId={}",
          source.getId(),
          target.getId(),
          ex);
      return false;
    }
  }

  /** Impede reutilização quando uma revisão altera materialmente o canal comercial executável. */
  private static String acquisitionPolicy(
      BusinessProcessDefinition process, ObjectMapper objectMapper) throws JsonProcessingException {
    return objectMapper
        .readTree(process.getDiagramJson())
        .path("commercialAcquisitionPolicyVersion")
        .asText("");
  }

  /** Confere a identidade e a versão mínima que introduziu o contrato privado. */
  private static boolean identity(BusinessProcessDefinition process) {
    return process != null
        && PROCESS_CODE.equals(process.getProcessCode())
        && process.getVersionNumber() != null
        && process.getVersionNumber() >= 7
        && process.getDiagramJson() != null
        && !process.getDiagramJson().isBlank();
  }

  /**
   * Confere atividades, responsabilidades, subprocessos e fluxos sem depender de textos editoriais.
   */
  private static boolean structure(BusinessProcessDefinition process, ObjectMapper objectMapper)
      throws JsonProcessingException {
    JsonNode diagram = objectMapper.readTree(process.getDiagramJson());
    if (diagram == null || !diagram.isObject()) return false;
    if (process.getVersionNumber() > 7
        && !CRITERIA_VERSION.equals(diagram.path("commercialCriteriaVersion").asText())) {
      return false;
    }
    JsonNode nodes = diagram.path("nodes");
    JsonNode flows = diagram.path("flows");
    if (!nodes.isArray() || !flows.isArray()) return false;
    Map<String, JsonNode> byId = new HashMap<>();
    for (JsonNode node : nodes) {
      String id = node.path("id").asText();
      if (id.isBlank() || byId.put(id, node) != null) return false;
    }
    if (!byId.keySet().equals(NODE_IDS)
        || !"START".equals(byId.get("start").path("type").asText())
        || !"TASK".equals(byId.get("communicationContract").path("type").asText())
        || !"TASK".equals(byId.get("creatives").path("type").asText())
        || !"TASK".equals(byId.get("destination").path("type").asText())
        || !"TASK".equals(byId.get("integration").path("type").asText())
        || !"GATEWAY".equals(byId.get("gate").path("type").asText())
        || !"END".equals(byId.get("end").path("type").asText())) return false;
    JsonNode communication = byId.get("communicationContract");
    Set<String> agents = new HashSet<>();
    communication.path("responsibleAgentKeys").forEach(value -> agents.add(value.asText()));
    if (!agents.equals(Set.of("communication-director"))
        || !"COMMUNICATION_MATERIALIZATION"
            .equals(communication.path("responsibilityDomain").asText())
        || !"iris-communication-worker".equals(communication.path("executionResourceCode").asText())
        || !"creative-production-approval"
            .equals(byId.get("creatives").path("subprocessCode").asText())
        || !"landing-page-generation"
            .equals(byId.get("destination").path("subprocessCode").asText())) return false;
    Set<String> connections = new HashSet<>();
    for (JsonNode flow : flows) {
      String connection = flow.path("from").asText() + "->" + flow.path("to").asText();
      if (!connections.add(connection)) return false;
    }
    return connections.equals(FLOWS);
  }
}
