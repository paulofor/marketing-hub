package com.marketinghub.customeragent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.customeragent.CustomerDigitalObservation;
import com.marketinghub.repository.jpa.customeragent.CustomerAgentEvaluationRepository;
import com.marketinghub.repository.jpa.customeragent.CustomerDigitalObservationRepository;
import com.marketinghub.repository.jpa.customeragent.CustomerPersonaRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar a governança operacional das filas do Agente Cliente. */
class CustomerAgentServiceTest {

  /** Confirma que reservas abandonadas são encerradas antes de consultar nova pendência. */
  @Test
  void shouldExpireAbandonedObservationsBeforeClaiming() {
    CustomerPersonaRepository personas = mock(CustomerPersonaRepository.class);
    CustomerAgentEvaluationRepository evaluations = mock(CustomerAgentEvaluationRepository.class);
    CustomerDigitalObservationRepository observations =
        mock(CustomerDigitalObservationRepository.class);
    CustomerAgentMotivationService motivations = mock(CustomerAgentMotivationService.class);
    CustomerDigitalObservation abandoned = new CustomerDigitalObservation();
    abandoned.setStatus("RUNNING");
    abandoned.setStartedAt(Instant.parse("2026-08-05T01:00:00Z"));
    when(observations.findByStatusAndStartedAtBeforeOrderByStartedAtAsc(
            eq("RUNNING"), any(Instant.class), any(Pageable.class)))
        .thenReturn(List.of(abandoned));
    when(observations.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any(Pageable.class)))
        .thenReturn(List.of());
    var service = new CustomerAgentService(personas, evaluations, observations, motivations);

    assertThatThrownBy(service::claimPendingObservation)
        .isInstanceOf(ResponseStatusException.class);

    assertThat(abandoned.getStatus()).isEqualTo("FAILED");
    assertThat(abandoned.getFinishedAt()).isNotNull();
    assertThat(abandoned.getRawModelResponse()).contains("lease observacional expirado");
    verify(observations).saveAll(List.of(abandoned));
  }
}
