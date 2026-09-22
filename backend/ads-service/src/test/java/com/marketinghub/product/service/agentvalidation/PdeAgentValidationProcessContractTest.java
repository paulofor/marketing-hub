package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Comprova compatibilidade estrutural sem liberar contratos futuros desconhecidos. */
class PdeAgentValidationProcessContractTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Aceita mudanças editoriais e versões independentes que conservam as responsabilidades. */
  @ParameterizedTest
  @ValueSource(ints = {9, 10, 42})
  void acceptsCompatibleRevisions(int version) throws Exception {
    var process = process(version);
    assertThat(PdeAgentValidationProcessContract.supports(process, json)).isTrue();
  }

  /** Ausência de qualquer etapa obrigatória impede assumir o mesmo contrato. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "prototypeCorrection",
        "agentValidationGate",
        "technicalHomologation",
        "psiqueAdherent",
        "psiqueRecovery",
        "psiqueSafety",
        "commercialIntegrityReview"
      })
  void rejectsMissingContractActivity(String activity) throws Exception {
    var process = process(9);
    ObjectNode diagram = (ObjectNode) json.readTree(process.getDiagramJson());
    ArrayNode nodes = (ArrayNode) diagram.path("nodes");
    for (int i = nodes.size() - 1; i >= 0; i--)
      if (activity.equals(nodes.get(i).path("id").asText())) nodes.remove(i);
    process.setDiagramJson(diagram.toString());
    assertThat(PdeAgentValidationProcessContract.supports(process, json)).isFalse();
  }

  /** Rejeita mudanças que substituem o gate determinístico ou o gatilho de correção. */
  @ParameterizedTest
  @ValueSource(
      strings = {"executionMode", "activationMode", "responsibilityDomain", "remediatesActivities"})
  void rejectsIncompatibleSemantics(String field) throws Exception {
    var process = process(9);
    ObjectNode diagram = (ObjectNode) json.readTree(process.getDiagramJson());
    for (var node : diagram.path("nodes"))
      if (node.has(field)) ((ObjectNode) node).put(field, "DIFFERENT_CONTRACT");
    process.setDiagramJson(diagram.toString());
    assertThat(PdeAgentValidationProcessContract.supports(process, json)).isFalse();
  }

  /** JSON inválido, ausente ou sem grafo falha de forma fechada nas revisões novas. */
  @ParameterizedTest
  @ValueSource(strings = {"", "{invalid", "{}", "null", "{\"nodes\":{}}"})
  void rejectsInvalidDiagram(String diagram) throws Exception {
    var process = process(9);
    process.setDiagramJson(diagram);
    assertThat(PdeAgentValidationProcessContract.supports(process, json)).isFalse();
  }

  /** Contrato de outro processo e identificadores ambíguos nunca reutilizam o executor. */
  @Test
  void rejectsOtherProcessAndDuplicateActivities() throws Exception {
    var process = process(9);
    process.setProcessCode("independent-process");
    assertThat(PdeAgentValidationProcessContract.supports(process, json)).isFalse();
    process = process(9);
    ObjectNode diagram = (ObjectNode) json.readTree(process.getDiagramJson());
    ArrayNode nodes = (ArrayNode) diagram.path("nodes");
    for (var node : nodes) {
      if ("agentValidationGate".equals(node.path("id").asText())) {
        nodes.add(node.deepCopy());
        break;
      }
    }
    process.setDiagramJson(diagram.toString());
    assertThat(PdeAgentValidationProcessContract.supports(process, json)).isFalse();
  }

  /** Lê o grafo canônico preservado pela migração, com identidade sintética independente. */
  private BusinessProcessDefinition process(int version) throws Exception {
    var process = new BusinessProcessDefinition();
    process.setId(9107L);
    process.setProcessCode("pde-construction-approval");
    process.setVersionNumber(version);
    for (var source :
        json.readTree(
            Path.of("../../infra/testing/pde-commercial-principles/sources.json").toFile()))
      if (process.getProcessCode().equals(source.path("processCode").asText()))
        process.setDiagramJson(source.path("diagram").toString());
    return process;
  }
}
