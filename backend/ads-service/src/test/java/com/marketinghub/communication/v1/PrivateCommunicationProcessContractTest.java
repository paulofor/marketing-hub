package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: impedir reaproveitamento de comunicação entre revisões incompatíveis. */
class PrivateCommunicationProcessContractTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Aceita a revisão editorial v7 para v8 e a evidência produzida na própria v8. */
  @Test
  void acceptsCanonicalCompatibleRevisions() throws Exception {
    assertThat(PrivateCommunicationProcessContract.supports(process(63L, 7), process(85L, 8), json))
        .isTrue();
    assertThat(PrivateCommunicationProcessContract.supports(process(85L, 8), process(85L, 8), json))
        .isTrue();
  }

  /** Rejeita processos estranhos, regressão de versão e revisão sem marcador explícito. */
  @Test
  void rejectsIdentityAndVersionMismatch() throws Exception {
    var source = process(63L, 7);
    var other = process(85L, 8);
    other.setProcessCode("another-process");
    assertThat(PrivateCommunicationProcessContract.supports(source, other, json)).isFalse();
    assertThat(PrivateCommunicationProcessContract.supports(process(85L, 8), process(63L, 7), json))
        .isFalse();
    var unmarked = process(85L, 8);
    ObjectNode diagram = (ObjectNode) json.readTree(unmarked.getDiagramJson());
    diagram.remove("commercialCriteriaVersion");
    unmarked.setDiagramJson(diagram.toString());
    assertThat(PrivateCommunicationProcessContract.supports(source, unmarked, json)).isFalse();
  }

  /** Qualquer mudança de responsabilidade, subprocesso ou fluxo fecha o reaproveitamento. */
  @ParameterizedTest
  @ValueSource(strings = {"agent", "domain", "resource", "creative", "destination", "flow"})
  void rejectsChangedExecutableContract(String change) throws Exception {
    var source = process(63L, 7);
    var target = process(85L, 8);
    ObjectNode diagram = (ObjectNode) json.readTree(target.getDiagramJson());
    ObjectNode communication = node(diagram, "communicationContract");
    switch (change) {
      case "agent" -> communication.putArray("responsibleAgentKeys").add("another-agent");
      case "domain" -> communication.put("responsibilityDomain", "ANOTHER_DOMAIN");
      case "resource" -> communication.put("executionResourceCode", "another-worker");
      case "creative" -> node(diagram, "creatives").put("subprocessCode", "another-process");
      case "destination" -> node(diagram, "destination").put("subprocessCode", "another-process");
      case "flow" -> ((ArrayNode) diagram.path("flows")).remove(0);
      default -> throw new IllegalArgumentException(change);
    }
    target.setDiagramJson(diagram.toString());
    assertThat(PrivateCommunicationProcessContract.supports(source, target, json)).isFalse();
  }

  /** Localiza um nó da fixture canônica sem repetir a estrutura do processo no teste. */
  private ObjectNode node(ObjectNode diagram, String id) {
    for (var node : diagram.path("nodes"))
      if (id.equals(node.path("id").asText())) return (ObjectNode) node;
    throw new IllegalArgumentException(id);
  }

  /** Monta uma definição sintética a partir do grafo versionado usado pelo MySQL 5.7. */
  private BusinessProcessDefinition process(long id, int version) throws Exception {
    var process = new BusinessProcessDefinition();
    process.setId(id);
    process.setProcessCode("pde-communication-sales-journey");
    process.setVersionNumber(version);
    for (var source :
        json.readTree(
            Path.of("../../infra/testing/pde-commercial-principles/sources.json").toFile())) {
      if (!process.getProcessCode().equals(source.path("processCode").asText())) continue;
      ObjectNode diagram = source.path("diagram").deepCopy();
      if (version > 7) diagram.put("commercialCriteriaVersion", "PDE_COMMERCIAL_PRINCIPLES_V1");
      process.setDiagramJson(diagram.toString());
    }
    return process;
  }
}
