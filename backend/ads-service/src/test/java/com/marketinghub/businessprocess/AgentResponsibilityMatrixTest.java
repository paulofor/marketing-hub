package com.marketinghub.businessprocess;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar as fronteiras exclusivas dos oito agentes no catálogo BPM. */
class AgentResponsibilityMatrixTest {
  private final ObjectMapper mapper = new ObjectMapper();

  /** Aceita cada agente somente no seu domínio canônico e com uma única identidade. */
  @Test
  void acceptsTheEightCanonicalResponsibilities() {
    List.of(
            assignment("market-radar", "MARKET_EVIDENCE", "Argos"),
            assignment("experiment-strategist", "MARKET_STRATEGY", "Atena"),
            assignment("financial-agent", "FINANCIAL_VALIDATION", "Plutus"),
            assignment("landing-generator", "PDE_CONSTRUCTION", "Dédalo"),
            assignment("videomaker", "AUDIOVISUAL_PRODUCTION", "Apolo"),
            assignment("customer-agent", "HUMAN_EXPERIENCE_REVIEW", "Psique"),
            assignment(
                "meta-ad-approver", "COMMERCIAL_INTEGRITY_REVIEW", "Têmis — revisão independente"),
            assignment("growth-operator", "GROWTH_OPERATION", "Hermes"))
        .forEach(
            node ->
                assertThatCode(() -> AgentResponsibilityMatrix.validate(node, "TASK"))
                    .doesNotThrowAnyException());
  }

  /** Rejeita a antiga coautoria de Psique e Têmis na mesma atividade. */
  @Test
  void rejectsMoreThanOneResponsibleAgent() throws Exception {
    var node =
        mapper.readTree(
            "{\"owner\":\"Psique e Têmis\",\"responsibilityDomain\":\"HUMAN_EXPERIENCE_REVIEW\",\"responsibleAgentKeys\":[\"customer-agent\",\"meta-ad-approver\"]}");

    assertThatThrownBy(() -> AgentResponsibilityMatrix.validate(node, "TASK"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exatamente uma");
  }

  /** Rejeita um agente usando o domínio decisório de outro perfil. */
  @Test
  void rejectsResponsibilityDomainFromAnotherAgent() {
    var node = assignment("growth-operator", "MARKET_STRATEGY", "Hermes");

    assertThatThrownBy(() -> AgentResponsibilityMatrix.validate(node, "TASK"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("incompatível");
  }

  /** Rejeita dono textual compartilhado mesmo quando só uma chave técnica foi informada. */
  @Test
  void rejectsSharedOwnerLabel() {
    var node =
        assignment(
            "meta-ad-approver", "COMMERCIAL_INTEGRITY_REVIEW", "Têmis e executores de produto");

    assertThatThrownBy(() -> AgentResponsibilityMatrix.validate(node, "TASK"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("coautores");
  }

  /** Exige identidade técnica quando o rótulo atribui a atividade a um agente. */
  @Test
  void rejectsAgentOwnerWithoutTechnicalIdentity() throws Exception {
    var node = mapper.readTree("{\"owner\":\"Dédalo\"}");

    assertThatThrownBy(() -> AgentResponsibilityMatrix.validate(node, "TASK"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("responsibleAgentKey");
  }

  /** Mantém atividades determinísticas do backend fora da matriz de agentes. */
  @Test
  void acceptsNonAgentTaskWithoutResponsibilityDomain() throws Exception {
    var node = mapper.readTree("{\"owner\":\"Backend\"}");

    assertThatCode(() -> AgentResponsibilityMatrix.validate(node, "TASK"))
        .doesNotThrowAnyException();
  }

  /** Monta um nó de teste com o contrato mínimo de autoria. */
  private com.fasterxml.jackson.databind.JsonNode assignment(
      String agentKey, String domain, String owner) {
    var node = mapper.createObjectNode();
    node.put("owner", owner);
    node.put("responsibilityDomain", domain);
    node.putArray("responsibleAgentKeys").add(agentKey);
    return node;
  }
}
