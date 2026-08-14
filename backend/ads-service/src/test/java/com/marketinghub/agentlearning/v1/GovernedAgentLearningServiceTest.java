package com.marketinghub.agentlearning.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agentmemory.service.AgentMemoryService;
import com.marketinghub.agentmemory.service.registerFeedback.RegisterMemoryFeedbackRequest;
import com.marketinghub.repository.jpa.agentlearning.GovernedAgentLearningExperimentRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Valida replay, holdout, regressão e promoção externa do aprendizado governado. */
class GovernedAgentLearningServiceTest {
  private GovernedAgentLearningExperimentRepository repository;
  private AgentMemoryService memoryService;
  private GovernedAgentLearningService service;

  /** Prepara dependências isoladas e relógio determinístico. */
  @BeforeEach
  void setUp() {
    repository = mock(GovernedAgentLearningExperimentRepository.class);
    memoryService = mock(AgentMemoryService.class);
    service =
        new GovernedAgentLearningService(
            repository,
            memoryService,
            new ObjectMapper(),
            Clock.fixed(Instant.parse("2026-08-10T12:00:00Z"), ZoneOffset.UTC));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  /** Deve congelar a candidata antes de receber qualquer resultado. */
  @Test
  void shouldFreezeCandidateAndReplaySets() {
    LearningExperimentResponse response = service.create(createRequest());
    assertEquals("FROZEN", response.status());
    verify(repository)
        .save(
            argThat(
                value ->
                    "landing-generator".equals(value.getAgentKey())
                        && "candidate-v2".equals(value.getCandidateVersion())));
  }

  /** Deve impedir que um agente ainda não homologado entre no ciclo. */
  @Test
  void shouldRejectUngovernedAgent() {
    CreateLearningExperimentRequest request = createRequest();
    assertThrows(
        ResponseStatusException.class,
        () ->
            service.create(
                new CreateLearningExperimentRequest(
                    "unknown-agent",
                    request.scopeType(),
                    request.scopeId(),
                    request.memoryId(),
                    request.candidateVersion(),
                    request.baselineVersion(),
                    request.frozenReplaySetJson(),
                    request.holdoutReplaySetJson(),
                    request.minimumGain(),
                    request.maximumCostIncreaseRatio())));
  }

  /** Deve considerar elegível apenas ganho fora da amostra com custo e regressão aprovados. */
  @Test
  void shouldMarkCandidateReadyAfterCompleteLocalEvidence() {
    when(repository.findById(1L)).thenReturn(Optional.of(frozen()));
    LearningExperimentResponse response = service.evaluate(1L, evaluation(true, true, 10, 5));
    assertEquals("READY_FOR_PROMOTION", response.status());
    verify(repository)
        .save(argThat(value -> value.getDecisionEvidence().contains("externalEffects=false")));
  }

  /** Deve aceitar Apolo no ambiente compartilhado após a homologação do replay de storyboard. */
  @Test
  void shouldFreezeApolloReplayWithoutChangingItsAuthority() {
    CreateLearningExperimentRequest request = createRequest();
    LearningExperimentResponse response =
        service.create(
            new CreateLearningExperimentRequest(
                "apollo",
                request.scopeType(),
                request.scopeId(),
                request.memoryId(),
                request.candidateVersion(),
                request.baselineVersion(),
                request.frozenReplaySetJson(),
                request.holdoutReplaySetJson(),
                request.minimumGain(),
                request.maximumCostIncreaseRatio()));

    assertEquals("FROZEN", response.status());
    assertEquals("apollo", response.agentKey());
  }

  /** Deve rejeitar qualquer avaliação sombra que tenha produzido efeito externo. */
  @Test
  void shouldRejectShadowEvaluationWithExternalEffect() {
    when(repository.findById(1L)).thenReturn(Optional.of(frozen()));
    EvaluateLearningExperimentRequest safe = evaluation(true, true, 10, 5);
    EvaluateLearningExperimentRequest unsafe =
        new EvaluateLearningExperimentRequest(
            safe.baselineHoldoutScore(),
            safe.candidateHoldoutScore(),
            safe.baselineCost(),
            safe.candidateCost(),
            safe.replayCaseCount(),
            safe.holdoutCaseCount(),
            safe.regressionPassed(),
            safe.localValidationPassed(),
            true,
            false,
            false,
            safe.baselineResultJson(),
            safe.candidateResultJson());

    assertThrows(ResponseStatusException.class, () -> service.evaluate(1L, unsafe));
    verify(repository, never()).save(any());
  }

  /** Deve rejeitar melhoria aparente quando o holdout é insuficiente. */
  @Test
  void shouldRejectCandidateWithoutEnoughHoldoutCases() {
    when(repository.findById(1L)).thenReturn(Optional.of(frozen()));
    assertThrows(
        ResponseStatusException.class, () -> service.evaluate(1L, evaluation(true, true, 10, 4)));
  }

  /** Deve rejeitar uma candidata que introduziu regressão apesar do ganho médio. */
  @Test
  void shouldRejectCandidateWithRegression() {
    when(repository.findById(1L)).thenReturn(Optional.of(frozen()));
    assertEquals("REJECTED", service.evaluate(1L, evaluation(false, true, 10, 5)).status());
  }

  /** Deve impedir promoção sem todos os gates locais. */
  @Test
  void shouldBlockPromotionBeforeEligibility() {
    when(repository.findById(1L)).thenReturn(Optional.of(frozen()));
    assertThrows(ResponseStatusException.class, () -> service.promote(1L));
    verifyNoInteractions(memoryService);
  }

  /** Deve confirmar a memória somente na promoção explícita posterior aos testes. */
  @Test
  void shouldPromoteAndConfirmMemoryAfterLocalGates() {
    GovernedAgentLearningExperiment value = frozen();
    value.setStatus("READY_FOR_PROMOTION");
    value.setRegressionPassed(true);
    value.setLocalValidationPassed(true);
    value.setDecisionEvidence("holdoutGain=7");
    when(repository.findById(1L)).thenReturn(Optional.of(value));
    assertEquals("PROMOTED", service.promote(1L).status());
    verify(memoryService)
        .feedback(
            eq("landing-generator"),
            eq(9L),
            argThat(
                (RegisterMemoryFeedbackRequest request) ->
                    "CONFIRMED".equals(request.outcome())
                        && request.evidence().contains("holdoutGain=7")));
  }

  /** Monta a entrada congelável usada pelos cenários. */
  private CreateLearningExperimentRequest createRequest() {
    return new CreateLearningExperimentRequest(
        "landing-generator",
        "EXPERIMENT",
        "88",
        9L,
        "candidate-v2",
        "baseline-v1",
        "[1,2,3,4,5,6,7,8,9,10]",
        "[11,12,13,14,15]",
        new BigDecimal("5.0"),
        new BigDecimal("0.20"));
  }

  /** Monta um experimento já congelado para avaliação. */
  private GovernedAgentLearningExperiment frozen() {
    GovernedAgentLearningExperiment value = new GovernedAgentLearningExperiment();
    value.setAgentKey("landing-generator");
    value.setScopeType("EXPERIMENT");
    value.setScopeId("88");
    value.setMemoryId(9L);
    value.setCandidateVersion("candidate-v2");
    value.setBaselineVersion("baseline-v1");
    value.setFrozenReplaySetJson("[1,2,3,4,5,6,7,8,9,10]");
    value.setHoldoutReplaySetJson("[11,12,13,14,15]");
    value.setMinimumGain(new BigDecimal("5.0"));
    value.setMaximumCostIncreaseRatio(new BigDecimal("0.20"));
    value.setStatus("FROZEN");
    value.setCreatedAt(Instant.parse("2026-08-10T12:00:00Z"));
    return value;
  }

  /** Monta resultados locais comparáveis. */
  private EvaluateLearningExperimentRequest evaluation(
      boolean regression, boolean local, int replayCases, int holdoutCases) {
    return new EvaluateLearningExperimentRequest(
        new BigDecimal("70"),
        new BigDecimal("77"),
        new BigDecimal("1.00"),
        new BigDecimal("1.10"),
        replayCases,
        holdoutCases,
        regression,
        local,
        false,
        false,
        false,
        "{\"score\":70}",
        "{\"score\":77}");
  }
}
