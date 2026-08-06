package com.marketinghub.customeragent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.customeragent.CustomerAgentEvaluation;
import com.marketinghub.customeragent.CustomerDigitalObservation;
import com.marketinghub.customeragent.CustomerPersona;
import com.marketinghub.repository.jpa.customeragent.CustomerAgentEvaluationRepository;
import com.marketinghub.repository.jpa.customeragent.CustomerDigitalObservationRepository;
import com.marketinghub.repository.jpa.customeragent.CustomerPersonaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar a governança operacional das filas do Agente Cliente. */
class CustomerAgentServiceTest {

  /** Reabre a mesma avaliação falha e preserva a causa para auditoria. */
  @Test
  void shouldRetryFailedEvaluationPreservingLastError() {
    CustomerAgentEvaluationRepository evaluations = mock(CustomerAgentEvaluationRepository.class);
    CustomerAgentEvaluation evaluation = new CustomerAgentEvaluation();
    CustomerPersona persona = new CustomerPersona();
    persona.setId(4L);
    persona.setName("Nail designer");
    evaluation.setId(1L);
    evaluation.setPersona(persona);
    evaluation.setAssetType("OFFER");
    evaluation.setAssetReference("Microamostra personalizada");
    evaluation.setStatus("FAILED");
    evaluation.setLastError("Timeout do Codex.");
    evaluation.setRetryCount(0);
    evaluation.setFinishedAt(Instant.now());
    when(evaluations.findById(1L)).thenReturn(Optional.of(evaluation));
    when(evaluations.save(evaluation)).thenReturn(evaluation);
    var service =
        new CustomerAgentService(
            mock(CustomerPersonaRepository.class),
            evaluations,
            mock(CustomerDigitalObservationRepository.class),
            mock(CustomerAgentMotivationService.class));

    var response = service.retryEvaluation(1L);

    assertThat(response.status()).isEqualTo("PENDING");
    assertThat(response.retryCount()).isEqualTo(1);
    assertThat(response.lastError()).isEqualTo("Timeout do Codex.");
    assertThat(response.finishedAt()).isNull();
  }

  /** Impede reprocessamento concorrente ou duplicado fora do estado de falha. */
  @Test
  void shouldRejectRetryWhenEvaluationIsNotFailed() {
    CustomerAgentEvaluationRepository evaluations = mock(CustomerAgentEvaluationRepository.class);
    CustomerAgentEvaluation evaluation = new CustomerAgentEvaluation();
    evaluation.setStatus("COMPLETED");
    when(evaluations.findById(1L)).thenReturn(Optional.of(evaluation));
    var service =
        new CustomerAgentService(
            mock(CustomerPersonaRepository.class),
            evaluations,
            mock(CustomerDigitalObservationRepository.class),
            mock(CustomerAgentMotivationService.class));

    assertThatThrownBy(() -> service.retryEvaluation(1L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Somente avaliações com falha");
  }

  /** Impede que uma referência maior que a coluna canônica vire erro 500 do MySQL. */
  @Test
  void shouldRejectAssetReferenceLongerThanDatabaseContract() {
    var service =
        new CustomerAgentService(
            mock(CustomerPersonaRepository.class),
            mock(CustomerAgentEvaluationRepository.class),
            mock(CustomerDigitalObservationRepository.class),
            mock(CustomerAgentMotivationService.class));

    assertThatThrownBy(
            () ->
                service.start(
                    new CustomerAgentContracts.StartEvaluationRequest(
                        4L, "OFFER", "a".repeat(256))))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("255 caracteres");
  }

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
