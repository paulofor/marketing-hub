package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir que o consumidor de Íris invada atividades de outros agentes. */
class CommunicationAgentTaskConsumerTest {

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
