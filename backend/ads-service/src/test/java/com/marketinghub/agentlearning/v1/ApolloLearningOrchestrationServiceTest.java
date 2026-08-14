package com.marketinghub.agentlearning.v1;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agentmemory.service.AgentMemoryService;
import com.marketinghub.agentmemory.service.retrieveMemory.MemoryResponse;
import com.marketinghub.repository.jpa.agentlearning.ApolloLearningObservationRepository;
import com.marketinghub.repository.jpa.agentlearning.GovernedAgentLearningExperimentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: provar acumulação, QA independente e ausência de efeitos externos no piloto de
 * Apolo.
 */
class ApolloLearningOrchestrationServiceTest {
  private ApolloLearningObservationRepository observations;
  private GovernedAgentLearningExperimentRepository experiments;
  private AgentMemoryService memoryService;
  private GovernedAgentLearningService learningService;
  private ApolloLearningOrchestrationService service;

  /** Prepara contratos isolados para validar a orquestração sem banco ou provider. */
  @BeforeEach
  void setUp() {
    observations = mock(ApolloLearningObservationRepository.class);
    experiments = mock(GovernedAgentLearningExperimentRepository.class);
    memoryService = mock(AgentMemoryService.class);
    learningService = mock(GovernedAgentLearningService.class);
    service =
        new ApolloLearningOrchestrationService(
            observations, experiments, memoryService, learningService, new ObjectMapper());
    when(observations.findByJobId(anyLong())).thenReturn(Optional.empty());
    when(observations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(experiments.findByAgentKeyAndCandidateVersion(anyString(), anyString()))
        .thenReturn(Optional.empty());
  }

  /** Deve apenas acumular enquanto a amostra mínima ainda não existe. */
  @Test
  void shouldCollectWithoutCreatingPrematureExperiment() {
    when(observations.findByScopeIdAndBaselineVersionAndCandidateVersionOrderByIdAsc(
            any(), any(), any()))
        .thenReturn(List.of(observation(1L)));
    ApolloLearningObservationResponse response = service.observe(request());
    assertEquals("COLLECTING_REPLAY_CASES", response.status());
    verifyNoInteractions(memoryService, learningService);
  }

  /** Deve criar memória candidata e avaliar somente após 10 replays e 5 holdouts. */
  @Test
  void shouldCloseGovernedSampleWithoutExternalEffects() {
    List<ApolloLearningObservation> sample =
        IntStream.rangeClosed(1, 15).mapToObj(value -> observation((long) value)).toList();
    when(observations.findByScopeIdAndBaselineVersionAndCandidateVersionOrderByIdAsc(
            any(), any(), any()))
        .thenReturn(sample);
    when(memoryService.register(eq("apollo"), any()))
        .thenReturn(
            new MemoryResponse(
                44L,
                "CANDIDATE",
                "DIRECAO_AUDIOVISUAL",
                "conteudo",
                "evidencia",
                null,
                "job/15",
                new BigDecimal("0.60"),
                null,
                0,
                Instant.now()));
    when(learningService.create(any())).thenReturn(response(77L, "FROZEN"));
    when(learningService.evaluate(eq(77L), any())).thenReturn(response(77L, "READY_FOR_PROMOTION"));

    ApolloLearningObservationResponse result = service.observe(request());

    assertEquals(77L, result.experimentId());
    assertEquals("READY_FOR_PROMOTION", result.status());
    verify(memoryService)
        .register(eq("apollo"), argThat(value -> value.sourceExecutionId().equals("job/99")));
    verify(learningService)
        .evaluate(
            eq(77L),
            argThat(
                value ->
                    value.replayCaseCount() == 10
                        && value.holdoutCaseCount() == 5
                        && !value.externalProviderCalled()
                        && !value.spendingAuthorized()
                        && !value.publicationPerformed()));
  }

  /** Deve rejeitar qualquer observação que declare gasto, publicação ou provider. */
  @Test
  void shouldRejectExternalEffect() {
    ApolloLearningObservationRequest unsafe =
        new ApolloLearningObservationRequest(
            99L,
            "project-4",
            "api-v1",
            "codex-v1",
            BigDecimal.valueOf(70),
            BigDecimal.valueOf(80),
            BigDecimal.ONE,
            BigDecimal.ONE,
            "{}",
            true,
            false,
            false);
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> service.observe(unsafe));
    verifyNoInteractions(observations);
  }

  /** Monta uma observação já aprovada pelo QA determinístico. */
  private ApolloLearningObservation observation(Long jobId) {
    ApolloLearningObservation value = new ApolloLearningObservation();
    value.setJobId(jobId);
    value.setScopeId("project-4");
    value.setBaselineVersion("api-v1");
    value.setCandidateVersion("codex-v1");
    value.setBaselineScore(BigDecimal.valueOf(70));
    value.setCandidateScore(BigDecimal.valueOf(80));
    value.setBaselineCost(BigDecimal.ONE);
    value.setCandidateCost(BigDecimal.ONE);
    value.setComparisonJson(comparison());
    value.setQaReviewer("BACKEND_DETERMINISTIC_QA_V1");
    value.setQaPassed(true);
    value.setCreatedAt(Instant.now());
    return value;
  }

  /** Monta a observação recebida do executor sombra. */
  private ApolloLearningObservationRequest request() {
    return new ApolloLearningObservationRequest(
        99L,
        "project-4",
        "api-v1",
        "codex-v1",
        BigDecimal.valueOf(70),
        BigDecimal.valueOf(80),
        BigDecimal.ONE,
        BigDecimal.ONE,
        comparison(),
        false,
        false,
        false);
  }

  /** Monta o relatório bruto que o QA do backend revalida sem confiar nos campos resumidos. */
  private String comparison() {
    return "{\"shadowMode\":true,\"providerCalled\":false,\"spendingAuthorized\":false,"
        + "\"comparison\":{\"shadowMode\":true,\"current\":{\"qualityScore\":70,"
        + "\"expectedCostUsd\":1},\"candidate\":{\"qualityScore\":80,"
        + "\"expectedCostUsd\":1,\"gateApproved\":true}}}";
  }

  /** Monta a resposta mínima devolvida pela governança compartilhada. */
  private LearningExperimentResponse response(Long id, String status) {
    return new LearningExperimentResponse(
        id,
        "apollo",
        "VIDEO_STORYBOARD",
        "project-4",
        "codex-v1",
        "api-v1",
        status,
        44L,
        null,
        null,
        null,
        BigDecimal.ONE,
        BigDecimal.ZERO,
        true,
        true,
        Instant.now(),
        Instant.now(),
        null);
  }
}
