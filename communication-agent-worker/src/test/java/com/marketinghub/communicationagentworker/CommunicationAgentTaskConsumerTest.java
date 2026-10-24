package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir que o consumidor de Íris invada atividades de outros agentes. */
class CommunicationAgentTaskConsumerTest {

  /**
   * A falha de contrato conserva a resposta e o consumo sem callback de sucesso ou nova inferência.
   */
  @Test
  void preservesRejectedModelResponseInFailureCallback() throws Exception {
    var backend = org.mockito.Mockito.mock(CommunicationAgentBackendClient.class);
    var runner = org.mockito.Mockito.mock(CommunicationAgentCodexRunner.class);
    var properties = new CommunicationAgentProperties();
    var json = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
    var consumer =
        new CommunicationAgentTaskConsumer(backend, runner, properties, null, json, null);
    var input =
        java.util.Map.<String, Object>of(
            "taskId",
            910123L,
            "sourceReference",
            "experiment:91001",
            "processCode",
            "pde-communication-sales-journey",
            "activityId",
            "communicationContract");
    var failure =
        new CommunicationAgentCodexRunner.ExecutionException(
            "Prova ausente",
            "prompt",
            "agente",
            "atividade",
            CommunicationAgentCodexRunner.TokenUsage.empty(),
            new IllegalArgumentException("cinco pontos"),
            "{\"saida\":\"preservada\"}");
    org.springframework.test.util.ReflectionTestUtils.invokeMethod(
        consumer, "failSafely", input, null, java.time.Instant.now(), failure);
    var payload = org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
    org.mockito.Mockito.verify(backend)
        .fail(org.mockito.ArgumentMatchers.eq(910123L), payload.capture());
    assertThat(payload.getValue().get("resultJson")).isEqualTo("{\"saida\":\"preservada\"}");
    assertThat(String.valueOf(payload.getValue().get("executionAudit"))).contains("prompt");
    assertThat(String.valueOf(payload.getValue().get("error"))).doesNotContain("AUTO_RETRY");
    org.mockito.Mockito.verify(backend, org.mockito.Mockito.never())
        .complete(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyMap());
    org.mockito.Mockito.verifyNoInteractions(runner);
  }

  /** Aceita comunicação, peça não audiovisual e as quatro etapas de landing. */
  @Test
  void shouldSupportOnlyPublishedCommunicationContracts() {
    assertThat(
            CommunicationAgentTaskConsumer.supportsContract(
                "pde-communication-sales-journey", "communicationContract"))
        .isTrue();
    assertThat(
            CommunicationAgentTaskConsumer.supportsContract(
                "creative-production-approval", "nonAudiovisual"))
        .isTrue();
    assertThat(CommunicationAgentTaskConsumer.supportsContract("landing-page-generation", "select"))
        .isTrue();
    assertThat(
            CommunicationAgentTaskConsumer.supportsContract("landing-page-generation", "strategy"))
        .isTrue();
    assertThat(
            CommunicationAgentTaskConsumer.supportsContract("landing-page-generation", "compose"))
        .isTrue();
    assertThat(CommunicationAgentTaskConsumer.supportsContract("landing-page-generation", "html"))
        .isTrue();
  }

  /** Rejeita construção do produto, audiovisual, revisão, distribuição e publicação. */
  @Test
  void shouldRejectOtherAgentAndHumanActivities() {
    assertThat(
            CommunicationAgentTaskConsumer.supportsContract("pde-construction-approval", "journey"))
        .isFalse();
    assertThat(
            CommunicationAgentTaskConsumer.supportsContract(
                "pde-commercial-plan-offer", "productArchitecture"))
        .isFalse();
    assertThat(
            CommunicationAgentTaskConsumer.supportsContract(
                "pde-tasting-proof-of-value", "materialization"))
        .isFalse();
    assertThat(
            CommunicationAgentTaskConsumer.supportsContract(
                "venda-entrega-satisfacao-cliente", "materialization"))
        .isFalse();
    assertThat(
            CommunicationAgentTaskConsumer.supportsContract(
                "creative-production-approval", "audiovisual"))
        .isFalse();
    assertThat(
            CommunicationAgentTaskConsumer.supportsContract(
                "creative-production-approval", "commercial"))
        .isFalse();
    assertThat(
            CommunicationAgentTaskConsumer.supportsContract(
                "operacao-otimizacao-experimento", "task-10"))
        .isFalse();
    assertThat(CommunicationAgentTaskConsumer.supportsContract("landing-page-generation", "human"))
        .isFalse();
  }
}
