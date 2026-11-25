package com.marketinghub.researchintelligence.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifica que a capacidade chega a tarefas pertinentes sem alterar tarefas não relacionadas. */
class ExpectationHarnessTest {
  /** Confere os nove responsáveis, evidência versionada e isolamento entre dois contextos. */
  @Test
  void routesExpectationProtocolToRelevantAgentsOnly() {
    var service =
        new ResearchIntelligenceService(
            Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC));
    for (String agent :
        List.of(
            "market-radar",
            "experiment-strategist",
            "financial-agent",
            "landing-generator",
            "communication-director",
            "videomaker",
            "customer-agent",
            "meta-ad-approver",
            "growth-operator")) {
      var selection =
          service.selectForAgentTask(agent, "Investigar expectativa para produto sintético 101");
      assertThat(selection.expectationIntelligence().path("contractVersion").asText())
          .isEqualTo("CONSUMER_EXPECTATION_V1");
      assertThat(selection.routes().getFirst().cards()).hasSizeBetween(1, 4);
      assertThat(selection.expectationIntelligence().has("sources")).isFalse();
      assertThat(selection.expectationIntelligence().has("tools")).isFalse();
      assertThat(selection.routes().getFirst().cards())
          .anyMatch(c -> c.collection().equals("expectativas-surpresa"));
      assertThat(
              service
                  .selectForAgentTask(agent, "Investigar expectativa para produto sintético 202")
                  .contextFingerprint())
          .isNotEqualTo(selection.contextFingerprint());
    }
    assertThat(service.selectForAgentTask("experiment-strategist", "Analisar custos operacionais"))
        .isNull();
    assertThat(
            service
                .selectForAgentTask("videomaker", "Roteiro audiovisual de uma rotina")
                .expectationIntelligence())
        .isNull();
    assertThat(service.selectForAgentTask("unknown", "Investigar expectativa")).isNull();
  }
}
