package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a auditoria das falhas de avaliação do Agente Cliente. */
class CustomerAgentSchedulerTest {

  /** Preserva mensagem, causa e stack trace enviados ao backend. */
  @Test
  void shouldPersistDetailedFailureChain() {
    Exception failure = new IllegalStateException("falha externa", new RuntimeException("causa"));

    String detail = CustomerAgentScheduler.detailedError(failure);

    assertThat(detail)
        .contains("IllegalStateException: falha externa")
        .contains("Caused by: java.lang.RuntimeException: causa");
  }

  /** Limita a falha persistida para proteger o contrato HTTP e o banco. */
  @Test
  void shouldLimitPersistedFailureSize() {
    Exception failure = new IllegalStateException("x".repeat(20000));

    assertThat(CustomerAgentScheduler.detailedError(failure)).hasSize(16000);
  }
}
