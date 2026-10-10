package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: impedir reserva legada ou contrato de Atena em outra fila. */
class AtenaBpmWorkerContractTest {
  /** Reconhece a fila própria e preserva os módulos e atividades que não usam este protocolo. */
  @Test
  void scopesExactQueueAndCompatibleVersion() {
    assertThat(
            AtenaBpmWorkerContract.supports(
                "experiment-strategist", "pde-commercial-plan-offer", "marketStrategy"))
        .isTrue();
    assertThat(AtenaBpmWorkerContract.accepts(null)).isFalse();
    assertThat(AtenaBpmWorkerContract.accepts(AtenaBpmWorkerContract.VERSION)).isTrue();
    assertThatThrownBy(() -> AtenaBpmWorkerContract.accepts("ATENA_LEGACY_V1"))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(
            AtenaBpmWorkerContract.supports(
                "customer-agent", "pde-commercial-plan-offer", "marketStrategy"))
        .isFalse();
    assertThat(
            AtenaBpmWorkerContract.supports(
                "experiment-strategist", "other-module", "marketStrategy"))
        .isFalse();
    assertThat(
            AtenaBpmWorkerContract.supports(
                "experiment-strategist", "pde-commercial-plan-offer", "economics"))
        .isFalse();
  }
}
