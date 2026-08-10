package com.marketinghub.geralanding.agent.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.geralanding.qualityreview.service.LandingQualityReviewedEvent;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Valida fila, segregação e callback do executor premium de landing. */
class LandingGenerationAgentExecutionServiceTest {
  private GeraLandingStageExecutionRepository repository;
  private LandingGenerationAgentCoordinator coordinator;
  private ExperimentRepository experimentRepository;
  private LandingGenerationAgentExecutionService service;

  /** Prepara dependências isoladas antes de cada cenário. */
  @BeforeEach
  void setUp() {
    repository = mock(GeraLandingStageExecutionRepository.class);
    coordinator = mock(LandingGenerationAgentCoordinator.class);
    experimentRepository = mock(ExperimentRepository.class);
    service =
        new LandingGenerationAgentExecutionService(
            repository, coordinator, experimentRepository, new ObjectMapper());
    when(experimentRepository.findById(88L)).thenReturn(Optional.empty());
    when(repository
            .findTop20ByStageCodeAndStatusAndExecutionRequestedAtBeforeOrderByExecutionRequestedAtAsc(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                any()))
        .thenReturn(List.of());
  }

  /** Deve enfileirar reprovação sem executar Codex dentro do backend. */
  @Test
  void shouldEnqueueRejectedQualityReview() {
    service.onQualityReviewCompleted(
        new LandingQualityReviewedEvent(
            88L, "{\"approvalRecommendation\":\"REGENERATE_BEFORE_PUBLICATION\",\"score\":70}"));

    verify(repository).save(any(GeraLandingStageExecution.class));
  }

  /** Deve reservar uma pendência e preservar o experimento no snapshot. */
  @Test
  void shouldClaimPendingWithSegregatedContext() {
    GeraLandingStageExecution execution = execution("job-88", "INICIADO");
    when(repository.findTop3ByStageCodeAndStatusOrderByExecutionRequestedAtAsc(
            "landing-generation-agent-v1", "INICIADO"))
        .thenReturn(List.of(execution));

    List<LandingAgentPendingResponse> result = service.claimPending(1);

    assertEquals(1, result.size());
    assertEquals(88L, result.getFirst().experimentId());
    assertEquals("PROCESSANDO", execution.getStatus());
  }

  /** Deve tornar callback repetido idempotente e não avançar novamente. */
  @Test
  void shouldIgnoreRepeatedCompletedCallback() {
    GeraLandingStageExecution execution = execution("job-88", "CONCLUIDO");
    when(repository.findTopByIdJobOrderByExecutionRequestedAtDesc(any()))
        .thenReturn(Optional.of(execution));
    LandingAgentResultRequest request =
        new LandingAgentResultRequest("{}", "request", "{}", "gpt-5.6-sol", null, null, null, null);

    service.complete("job-88", request);

    org.mockito.Mockito.verifyNoInteractions(coordinator);
  }

  /** Cria uma execução mínima compatível com a fila. */
  private GeraLandingStageExecution execution(String id, String status) {
    return GeraLandingStageExecution.builder()
        .idJob(id.getBytes(StandardCharsets.UTF_8))
        .experimentId(88L)
        .stageCode("landing-generation-agent-v1")
        .promptContent("{\"score\":70}")
        .status(status)
        .executionRequestedAt(Instant.now())
        .createdAt(Instant.now())
        .build();
  }
}
