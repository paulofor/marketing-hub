package com.marketinghub.agentlearning.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agentmemory.service.AgentMemoryService;
import com.marketinghub.agentmemory.service.retrieveMemory.MemoryResponse;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.dto.CreativeAgentReviewResultRequest;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;
import com.marketinghub.repository.jpa.agentlearning.TemisVisualLearningAssetHistory;
import com.marketinghub.repository.jpa.agentlearning.TemisVisualLearningCaseRepository;
import com.marketinghub.repository.jpa.agentlearning.TemisVisualLearningRunRepository;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanImageStudioJobRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: provar amostra 10+5, ausência de efeitos e promoção externa de Têmis. */
class TemisVisualLearningServiceTest {
  private TemisVisualLearningCaseRepository cases;
  private TemisVisualLearningRunRepository runs;
  private CommercialPlanRepository plans;
  private CommercialPlanImageStudioJobRepository imageJobs;
  private CreativeRepository creatives;
  private TemisVisualPlaybookService playbooks;
  private AgentMemoryService memories;
  private GovernedAgentLearningService governance;
  private TemisVisualLearningService service;

  /** Prepara repositórios e serviços isolados para o replay local. */
  @BeforeEach
  void setUp() {
    cases = mock(TemisVisualLearningCaseRepository.class);
    runs = mock(TemisVisualLearningRunRepository.class);
    plans = mock(CommercialPlanRepository.class);
    imageJobs = mock(CommercialPlanImageStudioJobRepository.class);
    creatives = mock(CreativeRepository.class);
    playbooks = mock(TemisVisualPlaybookService.class);
    memories = mock(AgentMemoryService.class);
    governance = mock(GovernedAgentLearningService.class);
    service =
        new TemisVisualLearningService(
            cases,
            runs,
            plans,
            imageJobs,
            creatives,
            playbooks,
            memories,
            governance,
            new ObjectMapper());
    AtomicLong ids = new AtomicLong(100);
    when(cases.save(any()))
        .thenAnswer(
            invocation -> {
              TemisVisualLearningCase value = invocation.getArgument(0);
              if (value.getId() == null) value.setId(ids.incrementAndGet());
              return value;
            });
    when(runs.save(any()))
        .thenAnswer(
            invocation -> {
              TemisVisualLearningRun value = invocation.getArgument(0);
              if (value.getId() == null) value.setId(9L);
              return value;
            });
  }

  /** Congela exatamente dez replays e cinco holdouts após um parecer independente. */
  @Test
  void freezesTenReplayAndFiveHoldoutCases() {
    CommercialPlan plan = new CommercialPlan();
    plan.setId(2L);
    Experiment experiment = new Experiment();
    experiment.setId(88L);
    plan.setExperiment(experiment);
    Creative creative = new Creative();
    creative.setId(522L);
    creative.setVersionNumber(1);
    creative.setFormat("FEED");
    creative.setExperiment(experiment);
    when(cases.findBySourceTypeAndSourceId(TemisVisualLearningSourceType.CREATIVE, 522L))
        .thenReturn(Optional.empty());
    when(plans.findByExperimentReference(88L)).thenReturn(List.of(plan));
    when(playbooks.contextKey(plan, "FEED", List.of("ADS"), "2048x2048"))
        .thenReturn("agenda-cheia-feed");
    when(cases.findByContextKeyAndPlaybookVersionAndLearningRunIdIsNullOrderByIdAsc(
            "agenda-cheia-feed", "temis-visual-playbook-v1"))
        .thenReturn(IntStream.rangeClosed(1, 15).mapToObj(this::learningCase).toList());

    service.recordCreativeReview(creative, approvedReview());

    verify(runs)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                run ->
                    run.getStatus() == TemisVisualLearningRunStatus.PENDING
                        && run.getReplayCaseIdsJson().contains("10")
                        && run.getHoldoutCaseIdsJson().contains("15")));
    verify(cases).saveAll(any());
  }

  /** Recusa replay que tentou chamar provider, autorizar gasto ou publicar. */
  @Test
  void rejectsReplayWithExternalEffectBeforeCreatingMemory() {
    TemisVisualLearningRun run = processingRun();
    when(runs.findById(9L)).thenReturn(Optional.of(run));
    var unsafe =
        new TemisVisualLearningResultRequest(
            "reviewer-9",
            List.of("Preservar entregável real"),
            List.of("Evitar mockup genérico"),
            BigDecimal.valueOf(70),
            BigDecimal.valueOf(80),
            BigDecimal.valueOf(70),
            BigDecimal.valueOf(80),
            true,
            true,
            true,
            false,
            false,
            new ObjectMapper().createArrayNode(),
            "{}",
            "{}");

    assertThatThrownBy(() -> service.complete(9L, unsafe))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("provider");
    verify(memories, never()).register(any(), any());
  }

  /** Recalcula os quinze casos e libera somente uma candidata com ganho real no holdout. */
  @Test
  void completesReplayWithDeterministicScoresAndReadyCandidate() {
    TemisVisualLearningRun run = processingRun();
    List<TemisVisualLearningCase> sample =
        IntStream.rangeClosed(1, 15).mapToObj(this::learningCase).toList();
    when(runs.findById(9L)).thenReturn(Optional.of(run));
    when(cases.findAllById(any())).thenReturn(sample);
    when(memories.register(any(), any()))
        .thenReturn(
            new MemoryResponse(
                55L,
                "CANDIDATE",
                "PLAYBOOK_VISUAL",
                "conteúdo",
                "evidência",
                "temis-visual-learning-run/9",
                "temis-visual-learning/reviewer-9",
                new BigDecimal("0.80"),
                null,
                0,
                Instant.now()));
    when(governance.create(any())).thenReturn(learningResponse(77L, "FROZEN"));
    when(governance.evaluate(any(), any()))
        .thenReturn(learningResponse(77L, "READY_FOR_PROMOTION"));
    ObjectMapper mapper = new ObjectMapper();
    var assessments = mapper.createArrayNode();
    sample.forEach(
        value -> {
          var assessment = assessments.addObject();
          assessment.put("caseId", value.getId());
          assessment.put("set", value.getId() <= 10 ? "REPLAY" : "HOLDOUT");
          assessment.put("actualDecision", value.isApproved() ? "APPROVED" : "BLOCKED");
          assessment.put("candidateWouldPreventRecurrence", !value.isApproved());
          assessment.put("candidatePreservesApproved", value.isApproved());
        });
    var request =
        new TemisVisualLearningResultRequest(
            "reviewer-9",
            List.of("Preservar o entregável real"),
            List.of("Evitar fotografia genérica"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            true,
            true,
            false,
            false,
            false,
            assessments,
            "{}",
            "{}");

    var result = service.complete(9L, request);

    assertThat(result.status()).isEqualTo(TemisVisualLearningRunStatus.READY_FOR_PROMOTION);
    verify(governance)
        .evaluate(
            org.mockito.ArgumentMatchers.eq(77L),
            org.mockito.ArgumentMatchers.argThat(
                evaluation ->
                    evaluation.baselineHoldoutScore().compareTo(new BigDecimal("40.0000")) == 0
                        && evaluation.candidateHoldoutScore().compareTo(new BigDecimal("100.0000"))
                            == 0
                        && evaluation.replayCaseCount() == 10
                        && evaluation.holdoutCaseCount() == 5
                        && evaluation.regressionPassed()
                        && evaluation.localValidationPassed()
                        && !evaluation.externalProviderCalled()));
  }

  /** Exige decisão explícita posterior para promover a candidata pronta. */
  @Test
  void promotesOnlyReadyRunThroughGovernance() {
    TemisVisualLearningRun run = processingRun();
    run.setStatus(TemisVisualLearningRunStatus.READY_FOR_PROMOTION);
    run.setLearningExperimentId(77L);
    when(runs.findById(9L)).thenReturn(Optional.of(run));

    var result = service.promote(9L);

    assertThat(result.status()).isEqualTo(TemisVisualLearningRunStatus.PROMOTED);
    verify(governance).promote(77L);
  }

  /** Incorpora os pareceres antigos sem reexecutar geração ou revisão externa. */
  @Test
  void backfillsHistoricalAssetReviewsIdempotently() {
    CommercialPlan plan = new CommercialPlan();
    plan.setId(2L);
    Experiment experiment = new Experiment();
    experiment.setId(88L);
    plan.setExperiment(experiment);
    List<TemisVisualLearningAssetHistory> history =
        IntStream.rangeClosed(1, 15)
            .mapToObj(
                id ->
                    new TemisVisualLearningAssetHistory(
                        (long) id,
                        2L,
                        id % 2 == 0 ? "Story " + id : "Post " + id,
                        id,
                        id >= 14
                            ? CommercialPlanVisualAssetStatus.APPROVED
                            : CommercialPlanVisualAssetStatus.DRAFT,
                        id >= 14
                            ? CommercialPlanVisualAssetReviewStatus.APPROVED
                            : CommercialPlanVisualAssetReviewStatus.ADJUST,
                        "reviewer-" + id,
                        "{\"summary\":\"parecer histórico\"}",
                        "{}",
                        "{\"qualityScore\":95,\"deliveryFidelityScore\":95,"
                            + "\"commercialReuseScore\":95,\"issues\":[\"erro\"]}",
                        100L + id,
                        "[\"DELIVERY\",\"ADS\"]",
                        id % 2 == 0 ? "1152x2048" : "2048x2048",
                        "temis-visual-playbook-v1",
                        "agenda-cheia-feed",
                        new BigDecimal("0.18")))
            .toList();
    when(plans.findByExperimentReference(88L)).thenReturn(List.of(plan));
    when(imageJobs.findVisualLearningHistoryByPlanId(2L)).thenReturn(history);
    when(creatives.findVisualLearningHistoryByExperimentId(88L)).thenReturn(List.of());
    when(cases.count()).thenReturn(0L, 15L);
    when(runs.count()).thenReturn(0L, 0L);

    var result = service.backfillExperiment(88L);

    assertThat(result.ingestedCases()).isEqualTo(15);
    assertThat(result.scannedAssets()).isEqualTo(15);
    verify(cases, times(15)).save(any());
    verify(memories, never()).register(any(), any());
  }

  /** Cria um caso histórico homogêneo para o snapshot congelado. */
  private TemisVisualLearningCase learningCase(int id) {
    TemisVisualLearningCase value = new TemisVisualLearningCase();
    value.setId((long) id);
    value.setSourceType(TemisVisualLearningSourceType.CREATIVE);
    value.setSourceId((long) id);
    value.setContextKey("agenda-cheia-feed");
    value.setPlaybookVersion("temis-visual-playbook-v1");
    value.setPlacement("FEED");
    value.setFormat("FEED");
    value.setAttemptNumber(id);
    value.setApproved(id >= 14);
    value.setQualityScore(BigDecimal.valueOf(id >= 14 ? 95 : 60));
    value.setCostUsd(new BigDecimal("0.18"));
    value.setIssueCodesJson(id >= 14 ? "[]" : "[\"GENERIC_NAIL_PHOTO\"]");
    value.setEvidenceJson("{\"source\":\"experiment-88\"}");
    value.setCreatedAt(Instant.now());
    return value;
  }

  /** Cria o parecer comercial mínimo aprovado. */
  private CreativeAgentReviewResultRequest approvedReview() {
    return new CreativeAgentReviewResultRequest(
        CreativeAgentReviewStatus.APPROVED,
        95,
        95,
        95,
        95,
        95,
        "copy",
        "premium",
        "continuidade",
        "Produto real inequívoco",
        "[]",
        "[]",
        "codex",
        "{}",
        "{}",
        10,
        10,
        BigDecimal.ZERO,
        null,
        null,
        null,
        null,
        null,
        null,
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }

  /** Cria uma execução reservada com os quinze IDs congelados. */
  private TemisVisualLearningRun processingRun() {
    TemisVisualLearningRun run = new TemisVisualLearningRun();
    run.setId(9L);
    run.setContextKey("agenda-cheia-feed");
    run.setStatus(TemisVisualLearningRunStatus.PROCESSING);
    run.setBaselineVersion("temis-visual-playbook-v1");
    run.setCandidateVersion("temis-visual-1-15");
    run.setReplayCaseIdsJson("[1,2,3,4,5,6,7,8,9,10]");
    run.setHoldoutCaseIdsJson("[11,12,13,14,15]");
    run.setInputJson("{\"cases\":[]}");
    run.setProducerExecutionId("reviewer-9");
    run.setCreatedAt(Instant.now());
    run.setUpdatedAt(Instant.now());
    return run;
  }

  /** Cria a resposta mínima da governança usada no ciclo de promoção. */
  private LearningExperimentResponse learningResponse(Long id, String status) {
    return new LearningExperimentResponse(
        id,
        "meta-ad-approver",
        "VISUAL_CONTEXT",
        "agenda-cheia-feed",
        "temis-visual-1-15",
        "temis-visual-playbook-v1",
        status,
        55L,
        "{}",
        "{}",
        null,
        new BigDecimal("5.00"),
        BigDecimal.ZERO,
        true,
        true,
        Instant.now(),
        Instant.now(),
        null);
  }
}
