package com.marketinghub.researchintelligence.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifica que a capacidade chega a tarefas pertinentes sem alterar tarefas não relacionadas. */
class ExpectationHarnessTest {
  /** Preserva a orientação até o contrato audiovisual e mantém vídeos legados compatíveis. */
  @Test
  void preservesProtocolAcrossAudiovisualProjection() {
    var service =
        new ResearchIntelligenceService(
            Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC));
    var mapper =
        new com.marketinghub.salesvideo.mapper.VideoProjectResearchIntelligenceMapper(service);
    var json = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
    var first =
        com.marketinghub.salesvideo.VideoProject.builder()
            .id(101L)
            .productId(201L)
            .title("Surpresa útil no produto sintético A")
            .build();
    var second =
        com.marketinghub.salesvideo.VideoProject.builder()
            .id(102L)
            .productId(202L)
            .title("Curiosidade no produto sintético B")
            .build();
    var firstSelection = mapper.selectForVideoAgent(first, "videomaker");
    for (var project : List.of(first, second)) {
      var projected = json.valueToTree(mapper.selectForVideoAgent(project, "videomaker"));
      assertThat(projected.path("expectationIntelligence").path("contractVersion").asText())
          .isEqualTo("CONSUMER_EXPECTATION_V1");
      assertThat(projected.path("expectationIntelligence").has("tools")).isFalse();
      assertThat(projected.path("routes").get(0).path("agentKey").asText()).isEqualTo("videomaker");
      assertThat(
              json.valueToTree(mapper.selectForVideoProject(project))
                  .path("expectationIntelligence")
                  .path("contractVersion")
                  .asText())
          .isEqualTo("CONSUMER_EXPECTATION_V1");
    }
    assertThat(mapper.selectForVideoAgent(second, "videomaker").contextFingerprint())
        .isNotEqualTo(firstSelection.contextFingerprint());
    var legacy =
        com.marketinghub.salesvideo.VideoProject.builder()
            .id(103L)
            .title("Demonstração audiovisual de uma rotina")
            .build();
    var legacySelection = json.valueToTree(mapper.selectForVideoAgent(legacy, "videomaker"));
    assertThat(
            legacySelection.path("expectationIntelligence").isMissingNode()
                || legacySelection.path("expectationIntelligence").isNull())
        .isTrue();
    assertThat(mapper.selectForVideoAgent(first, "unknown")).isNull();
  }

  /** Todas as dimensões declaradas pelo protocolo devem chegar ao contexto dos agentes. */
  @Test
  void routesEveryDeclaredExpectationAxis() {
    var service =
        new ResearchIntelligenceService(
            Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC));
    for (var axis :
        com.marketinghub.consumerexpectation.v1.service.protocol.ExpectationProtocol.catalog()
            .path("axes")) {
      var selection =
          service.selectForAgentTask("experiment-strategist", axis.asText(), "Produto sintético");
      assertThat(selection).as(axis.asText()).isNotNull();
      assertThat(selection.expectationIntelligence()).as(axis.asText()).isNotNull();
    }
  }

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
