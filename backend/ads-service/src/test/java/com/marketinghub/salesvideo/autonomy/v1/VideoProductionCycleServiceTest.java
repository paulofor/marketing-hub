package com.marketinghub.salesvideo.autonomy.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskExecutionAuditRequest;
import com.marketinghub.agenttask.AgentTaskResponse;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CreateAgentTaskByAgentRequest;
import com.marketinghub.financialagent.service.FinancialAgentService;
import com.marketinghub.financialagent.service.StudioCostLedgerService;
import com.marketinghub.media.Asset;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.VideoCreditReservation;
import com.marketinghub.salesvideo.VideoProductionCycle;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.VideoProviderPreflight;
import com.marketinghub.salesvideo.dto.RequestSalesVideoPostProductionRequest;
import com.marketinghub.salesvideo.dto.RequestVideoRenderRequest;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto;
import com.marketinghub.salesvideo.mapper.VideoProjectResearchIntelligenceMapper;
import com.marketinghub.salesvideo.service.SalesVideoService;
import com.marketinghub.salesvideo.service.providerpreflight.VideoProviderFinancialPreflightService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: proteger o gate financeiro entre Plutus e Apolo. */
@ExtendWith(MockitoExtension.class)
class VideoProductionCycleServiceTest {
  @Mock private VideoProductionCycleRepository repository;
  @Mock private VideoProjectRepository projectRepository;
  @Mock private SalesVideoJobRepository jobRepository;
  @Mock private AgentTaskService taskService;
  @Mock private SalesVideoService salesVideoService;
  @Mock private FinancialAgentService financialAgentService;
  @Mock private StudioCostLedgerService studioCostLedgerService;
  @Mock private VideoProviderFinancialPreflightService providerPreflightService;
  private VideoProductionCycleService service;
  private final AtomicLong ids = new AtomicLong(10);

  /** Prepara persistência simulada sem consumir qualquer provider real. */
  @BeforeEach
  void setUp() {
    service =
        new VideoProductionCycleService(
            repository,
            projectRepository,
            jobRepository,
            taskService,
            salesVideoService,
            financialAgentService,
            studioCostLedgerService,
            providerPreflightService,
            new ObjectMapper().findAndRegisterModules());
    lenient()
        .when(studioCostLedgerService.cycleLedger(any()))
        .thenReturn(java.util.Map.of("segregated", true));
    lenient()
        .when(financialAgentService.intelligence(any()))
        .thenReturn(java.util.Map.of("coverage", "COMPLETE"));
    lenient().when(providerPreflightService.financialContext(any())).thenReturn(java.util.Map.of());
    lenient()
        .when(repository.save(any(VideoProductionCycle.class)))
        .thenAnswer(
            invocation -> {
              VideoProductionCycle cycle = invocation.getArgument(0);
              if (cycle.getId() == null) cycle.setId(ids.incrementAndGet());
              return cycle;
            });
  }

  /** Comprova que a abertura cria somente o preflight e não antecipa Plutus ou Apolo. */
  @Test
  void shouldOpenFinancialGateBeforeAnyProviderJob() {
    when(projectRepository.findById(7L)).thenReturn(Optional.of(project()));
    var result =
        service.create(
            new VideoProductionCycleContracts.CreateRequest(
                7L,
                new BigDecimal("12.50"),
                "DRAFT_INSTAGRAM",
                "Validar gancho",
                "Retencao superior",
                "usuario@mkt"));

    assertThat(result.status()).isEqualTo("PENDING_PROVIDER_PREFLIGHT");
    assertThat(result.learningObjective()).isEqualTo("Validar gancho");
    assertThat(result.successCriterion()).isEqualTo("Retencao superior");
    assertThat(result.financialSnapshot()).contains("incrementalLedger", "segregated");
    assertThat(result.salesVideoJobId()).isNull();
    verify(providerPreflightService).open(result.id(), "DRAFT_INSTAGRAM");
    verify(taskService, never()).createGateByAgent(any(), any());
    verify(salesVideoService, never()).requestRender(any(), any());
  }

  /** Mantém o preflight isolado sem reserva, gate de Plutus ou job pago. */
  @Test
  void shouldCompleteProviderPreflightOnlyWithoutFinancialProgression() {
    when(projectRepository.findById(7L)).thenReturn(Optional.of(project()));
    var opened =
        service.createProviderPreflight(
            new VideoProductionCycleContracts.CreateRequest(
                7L,
                new BigDecimal("10.00"),
                "FINAL_CAMPAIGN",
                "Confirmar configuração Runway",
                "Saldo, quota e custo estimado visíveis",
                "usuario@mkt"));
    VideoProductionCycle cycle = cycle();
    cycle.setId(opened.id());
    cycle.setStatus("PENDING_PROVIDER_PREFLIGHT_ONLY");
    VideoProviderPreflight preflight = new VideoProviderPreflight();
    preflight.setStatus("READY");
    when(repository.findById(opened.id())).thenReturn(Optional.of(cycle));
    when(providerPreflightService.complete(any(), any())).thenReturn(preflight);

    var completed =
        service.completeProviderPreflight(
            opened.id(),
            org.mockito.Mockito.mock(VideoProviderPreflightContracts.ResultRequest.class));

    assertThat(opened.status()).isEqualTo("PENDING_PROVIDER_PREFLIGHT_ONLY");
    assertThat(completed.status()).isEqualTo("PROVIDER_PREFLIGHT_ONLY_COMPLETED");
    verify(providerPreflightService).open(opened.id(), "FINAL_CAMPAIGN");
    verify(providerPreflightService, never()).reserve(any());
    verify(taskService, never()).createGateByAgent(any(), any());
    verify(salesVideoService, never()).requestRender(any(), any());
  }

  /** Mantém falha de autenticação visível sem transformar preflight isolado em produção. */
  @Test
  void shouldBlockProviderPreflightOnlyWithoutFinancialProgression() {
    VideoProductionCycle cycle = cycle();
    cycle.setStatus("PENDING_PROVIDER_PREFLIGHT_ONLY");
    VideoProviderPreflight preflight = new VideoProviderPreflight();
    preflight.setStatus("BLOCKED");
    when(repository.findById(11L)).thenReturn(Optional.of(cycle));
    when(projectRepository.findById(7L)).thenReturn(Optional.of(project()));
    when(providerPreflightService.complete(any(), any())).thenReturn(preflight);

    var completed =
        service.completeProviderPreflight(
            11L, org.mockito.Mockito.mock(VideoProviderPreflightContracts.ResultRequest.class));

    assertThat(completed.status()).isEqualTo("PROVIDER_PREFLIGHT_ONLY_BLOCKED");
    verify(providerPreflightService, never()).reserve(any());
    verify(taskService, never()).createGateByAgent(any(), any());
    verify(salesVideoService, never()).requestRender(any(), any());
  }

  /** Comprova que um projeto legado usa custos não atribuídos sem falhar nem inventar plano. */
  @Test
  void shouldOpenLegacyProjectWithUnassignedFinancialSnapshot() {
    VideoProject legacy = project();
    legacy.setCommercialPlanId(null);
    when(projectRepository.findById(7L)).thenReturn(Optional.of(legacy));
    when(financialAgentService.unassignedStudioIntelligence(76L))
        .thenReturn(java.util.Map.of("coverage", "PARTIAL"));
    var result =
        service.create(
            new VideoProductionCycleContracts.CreateRequest(
                7L,
                new BigDecimal("40.00"),
                "FINAL_CAMPAIGN",
                "Validar prova",
                "Prova compreensivel",
                "usuario@mkt"));

    assertThat(result.commercialPlanId()).isNull();
    assertThat(result.financialSnapshot()).contains("PARTIAL");
    verify(financialAgentService).unassignedStudioIntelligence(76L);
    verify(salesVideoService, never()).requestRender(any(), any());
  }

  /** Cria o gate de Plutus somente depois de o executor devolver um preflight utilizável. */
  @Test
  void shouldOpenPlutusGateOnlyAfterReadyProviderPreflight() {
    VideoProductionCycle cycle = cycle();
    cycle.setStatus("PENDING_PROVIDER_PREFLIGHT");
    VideoProviderPreflight preflight = new VideoProviderPreflight();
    preflight.setStatus("READY");
    when(repository.findById(11L)).thenReturn(Optional.of(cycle));
    when(projectRepository.findById(7L)).thenReturn(Optional.of(project()));
    when(providerPreflightService.complete(any(), any())).thenReturn(preflight);
    when(taskService.createGateByAgent(any(), any())).thenReturn(financialGateTask());

    var result =
        service.completeProviderPreflight(
            11L, org.mockito.Mockito.mock(VideoProviderPreflightContracts.ResultRequest.class));

    assertThat(result.status()).isEqualTo("PENDING_FINANCIAL_REVIEW");
    assertThat(result.agentTaskId()).isEqualTo(99L);
    ArgumentCaptor<CreateAgentTaskByAgentRequest> task =
        ArgumentCaptor.forClass(CreateAgentTaskByAgentRequest.class);
    verify(taskService)
        .createGateByAgent(
            task.capture(),
            org.mockito.ArgumentMatchers.eq("VIDEO_PROVIDER_COST_BENEFIT_APPROVAL"));
    assertThat(task.getValue().requestedByAgentKey()).isEqualTo("videomaker");
    assertThat(task.getValue().assignedAgentKey()).isEqualTo("financial-agent");
    verify(providerPreflightService).reserve(cycle);
    verify(salesVideoService, never()).requestRender(any(), any());
  }

  /** Mantém bloqueio técnico visível quando saldo, quota ou dry run não forem utilizáveis. */
  @Test
  void shouldNotOpenPlutusGateAfterBlockedProviderPreflight() {
    VideoProductionCycle cycle = cycle();
    cycle.setStatus("PENDING_PROVIDER_PREFLIGHT");
    VideoProviderPreflight preflight = new VideoProviderPreflight();
    preflight.setStatus("BLOCKED");
    when(repository.findById(11L)).thenReturn(Optional.of(cycle));
    when(projectRepository.findById(7L)).thenReturn(Optional.of(project()));
    when(providerPreflightService.complete(any(), any())).thenReturn(preflight);

    var result =
        service.completeProviderPreflight(
            11L, org.mockito.Mockito.mock(VideoProviderPreflightContracts.ResultRequest.class));

    assertThat(result.status()).isEqualTo("PROVIDER_PREFLIGHT_BLOCKED");
    verify(taskService, never()).createGateByAgent(any(), any());
    verify(salesVideoService, never()).requestRender(any(), any());
  }

  /** Comprova que somente a identidade técnica de Plutus decide o gate. */
  @Test
  void shouldRejectDecisionFromAnotherAgent() {
    assertThatThrownBy(
            () -> service.decide(11L, financialDecision("APPROVED", "parecer", "videomaker")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("403");
    verify(salesVideoService, never()).requestRender(any(), any());
  }

  /** Comprova que uma reprovação financeira termina sem gerar vídeo. */
  @Test
  void shouldBlockRejectedCycleWithoutProviderJob() {
    VideoProductionCycle cycle = cycle();
    when(repository.findById(11L)).thenReturn(Optional.of(cycle));
    when(projectRepository.findById(7L)).thenReturn(Optional.of(project()));
    when(providerPreflightService.hasActiveReservation(11L)).thenReturn(true);

    var result =
        service.decide(
            11L,
            financialDecision("REJECTED", "Custo acima do limite aprovado.", "financial-agent"));

    assertThat(result.status()).isEqualTo("FINANCIAL_BLOCKED");
    assertThat(result.knownCostUsd()).isEqualByComparingTo(BigDecimal.ZERO);
    verify(providerPreflightService).releaseUnusedReservation(11L);
    verify(salesVideoService, never()).requestRender(any(), any());
  }

  /** Comprova que Apolo repete o payload validado pelo router após a reserva de créditos. */
  @Test
  void shouldQueueApprovedCycleWithSeedanceInsteadOfLegacyLuma() {
    VideoProductionCycle cycle = cycle();
    VideoProject project = project();
    project.setTargetDurationSeconds(60);
    project.setProviderPlan("LUMA_RAY_3_2 como principal no plano legado.");
    SalesVideoJobDto job = new SalesVideoJobDto();
    job.setId(321L);
    when(repository.findById(11L)).thenReturn(Optional.of(cycle));
    when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
    when(salesVideoService.requestRender(any(), any())).thenReturn(job);
    VideoCreditReservation reservation = activeReservation();
    when(providerPreflightService.reserve(cycle)).thenReturn(reservation);
    when(providerPreflightService.requireActiveReservation(11L)).thenReturn(reservation);
    when(providerPreflightService.payloadSha256(11L)).thenReturn("abc123");
    when(providerPreflightService.routerConfigId(11L)).thenReturn("final-campaign");
    when(providerPreflightService.executionRequests(11L))
        .thenReturn("[{\"configId\":\"final-campaign\",\"input\":{}}]");
    when(providerPreflightService.selectedRoutes(11L))
        .thenReturn("[{\"routerConfigId\":\"final-campaign\",\"priceCeilingCredits\":500}]");
    service.setResearchIntelligenceMapper(
        new VideoProjectResearchIntelligenceMapper(
            new com.marketinghub.researchintelligence.v1.service.ResearchIntelligenceService()));

    var result =
        service.decide(
            11L,
            financialDecision("APPROVED", "Teto e ledger incremental válidos.", "financial-agent"));

    ArgumentCaptor<RequestVideoRenderRequest> render =
        ArgumentCaptor.forClass(RequestVideoRenderRequest.class);
    verify(salesVideoService).requestRender(org.mockito.ArgumentMatchers.eq(13L), render.capture());
    assertThat(render.getValue().getProviderName()).isEqualTo("RUNWAY_ROUTER");
    assertThat(render.getValue().getTargetDurationSeconds()).isEqualTo(10);
    assertThat(render.getValue().getMetadataJson())
        .contains(
            "\"providerClipDurationSeconds\":15",
            "\"sceneCount\":4",
            "\"cutCount\":15",
            "\"providerCreditReservationId\":77",
            "\"providerReservedCredits\":500",
            "\"providerReservationExpiresAt\"",
            "\"providerPreflightPayloadSha256\":\"abc123\"",
            "\"runwayRouterConfigId\":\"final-campaign\"",
            "\"runwaySelectedRoutesJson\"",
            "\"text_rendering\":\"DETERMINISTIC_OVERLAY\"",
            "\"contractVersion\":\"HARNESS_RESEARCH_INTELLIGENCE_V1\"",
            "\"agentKey\":\"videomaker\"",
            "\"sourceSha256\"");
    assertThat(result.status()).isEqualTo("QUEUED_FOR_APOLLO");
    assertThat(result.salesVideoJobId()).isEqualTo(321L);
  }

  /** Enfileira Product UGC como tomada única e preserva copy, direitos e gates de Apolo. */
  @Test
  void shouldQueuePremiumProductUgcWithDeterministicFinalization() {
    VideoProductionCycle cycle = cycle();
    cycle.setExperimentId(91L);
    VideoProject project = productUgcProject();
    SalesVideoJobDto job = new SalesVideoJobDto();
    job.setId(391L);
    VideoCreditReservation reservation = activeReservation();
    reservation.setReservedCredits(new BigDecimal("648"));
    reservation.setReservedCostUsd(new BigDecimal("6.48"));
    when(repository.findById(11L)).thenReturn(Optional.of(cycle));
    when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
    when(salesVideoService.requestRender(any(), any())).thenReturn(job);
    when(providerPreflightService.reserve(cycle)).thenReturn(reservation);
    when(providerPreflightService.requireActiveReservation(11L)).thenReturn(reservation);
    when(providerPreflightService.payloadSha256(11L)).thenReturn("ugc-hash");
    when(providerPreflightService.routerConfigId(11L)).thenReturn("product_ugc@2026-06");
    when(providerPreflightService.executionRequests(11L)).thenReturn("[{\"version\":\"2026-06\"}]");
    when(providerPreflightService.selectedRoutes(11L))
        .thenReturn("[{\"batchRouteId\":\"RUNWAY_PRODUCT_UGC:product_ugc@2026-06\"}]");
    service.setResearchIntelligenceMapper(
        new VideoProjectResearchIntelligenceMapper(
            new com.marketinghub.researchintelligence.v1.service.ResearchIntelligenceService()));

    VideoProductionCycleContracts.Response result =
        service.decide(
            11L,
            financialDecision("APPROVED", "Tarifa pinada e saldo válidos.", "financial-agent"));

    ArgumentCaptor<RequestVideoRenderRequest> render =
        ArgumentCaptor.forClass(RequestVideoRenderRequest.class);
    verify(salesVideoService).requestRender(org.mockito.ArgumentMatchers.eq(13L), render.capture());
    assertThat(render.getValue().getProviderName()).isEqualTo("RUNWAY_PRODUCT_UGC");
    assertThat(render.getValue().getTargetDurationSeconds()).isEqualTo(15);
    assertThat(render.getValue().getMetadataJson())
        .contains(
            "\"experimentId\":91",
            "RUNWAY_PRODUCT_UGC_WITH_DETERMINISTIC_POST_PRODUCTION",
            "\"continuousTakeRequired\":true",
            "\"captionMustMatchNarration\":true",
            "\"forbidMirrorOrReflection\":true",
            "\"assemblyRequired\":false",
            "\"productIsDigitalExperience\":true",
            "\"requiredReviewers\":[\"Psique\",\"Temis\",\"HUMAN\"]",
            "\"contractVersion\":\"HARNESS_RESEARCH_INTELLIGENCE_V1\"",
            "\"collection\":\"video\"",
            "\"collection\":\"prazer-audio-visual\"",
            "Você se arruma, mas ainda sente que falta presença? Faça o diagnóstico gratuito.");
    assertThat(result.generationClipCount()).isEqualTo(1);
    assertThat(result.editCutCount()).isEqualTo(2);
  }

  /** Bloqueia Product UGC sem referências e direitos antes de abrir o preflight. */
  @Test
  void shouldRejectProductUgcWithoutGovernedReferences() {
    VideoProject project = productUgcProject();
    project.setPerformanceRightsEvidence(null);
    when(projectRepository.findById(7L)).thenReturn(Optional.of(project));

    assertThatThrownBy(
            () ->
                service.create(
                    new VideoProductionCycleContracts.CreateRequest(
                        7L,
                        new BigDecimal("7.00"),
                        "FINAL_CAMPAIGN",
                        "Validar UGC",
                        "Ativo estável",
                        "usuario@mkt")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("direitos auditáveis");
    verify(providerPreflightService, never()).open(any(), any());
  }

  /** Bloqueia um job pago falho para impedir nova geração sem preflight e reserva novos. */
  @Test
  void shouldBlockFailedLegacyJobInsteadOfCreatingAnotherPaidRender() {
    VideoProductionCycle cycle = cycle();
    cycle.setStatus("QUEUED_FOR_APOLLO");
    cycle.setFinancialDecision("APPROVED");
    cycle.setSalesVideoJobId(20536L);
    SalesVideoJob failed = new SalesVideoJob();
    failed.setId(20536L);
    failed.setStatus(SalesVideoStatus.VIDEO_FAILED);
    failed.setProviderName("LUMA_RAY_3_2");
    failed.setFailureCode("PROVIDER_PAYMENT_REQUIRED");
    failed.setFailureDetail("Provider respondeu HTTP 402.");
    failed.setFinishedAt(Instant.parse("2026-08-13T10:00:00Z"));
    when(repository.findByStatusAndFinancialDecisionOrderByCreatedAtAsc(
            "QUEUED_FOR_APOLLO", "APPROVED"))
        .thenReturn(java.util.List.of(cycle));
    when(jobRepository.findById(20536L)).thenReturn(Optional.of(failed));

    service.reconcileApolloQueue();

    verify(salesVideoService, never()).requestRender(any(), any());
    assertThat(cycle.getStatus()).isEqualTo("APOLLO_BLOCKED");
    assertThat(cycle.getSalesVideoJobId()).isEqualTo(20536L);
    assertThat(cycle.getLastFailedJobId()).isEqualTo(20536L);
    assertThat(cycle.getLastApolloFailureCode()).isEqualTo("PROVIDER_PAYMENT_REQUIRED");
    assertThat(cycle.getLastApolloFailureDetail()).isEqualTo("Provider respondeu HTTP 402.");
    assertThat(cycle.getLastApolloFailureAt()).isEqualTo("2026-08-13T10:00:00Z");
  }

  /** Bloqueia o ciclo após rejeição de créditos para impedir novas tentativas a cada polling. */
  @Test
  void shouldBlockAutomaticReplacementAfterInsufficientCredits() {
    VideoProductionCycle cycle = cycle();
    cycle.setStatus("QUEUED_FOR_APOLLO");
    cycle.setFinancialDecision("APPROVED");
    cycle.setSalesVideoJobId(21125L);
    SalesVideoJob failed = new SalesVideoJob();
    failed.setId(21125L);
    failed.setStatus(SalesVideoStatus.VIDEO_FAILED);
    failed.setFailureCode("PROVIDER_RENDER_FAILED");
    failed.setFailureDetail("retryable=false; You do not have enough credits to run this task");
    when(repository.findByStatusAndFinancialDecisionOrderByCreatedAtAsc(
            "QUEUED_FOR_APOLLO", "APPROVED"))
        .thenReturn(java.util.List.of(cycle));
    when(jobRepository.findById(21125L)).thenReturn(Optional.of(failed));

    service.reconcileApolloQueue();

    assertThat(cycle.getStatus()).isEqualTo("APOLLO_BLOCKED");
    assertThat(cycle.getLastFailedJobId()).isEqualTo(21125L);
    verify(salesVideoService, never()).requestRender(any(), any());
  }

  /** Bloqueia ciclo legado sem job em vez de lançá-lo sem preflight para o executor. */
  @Test
  void shouldBlockLegacyQueuedCycleWithoutActiveReservation() {
    VideoProductionCycle cycle = cycle();
    cycle.setStatus("QUEUED_FOR_APOLLO");
    cycle.setFinancialDecision("APPROVED");
    when(repository.findByStatusAndFinancialDecisionOrderByCreatedAtAsc(
            "QUEUED_FOR_APOLLO", "APPROVED"))
        .thenReturn(java.util.List.of(cycle));
    when(providerPreflightService.hasActiveReservation(11L)).thenReturn(false);

    service.reconcileApolloQueue();

    assertThat(cycle.getStatus()).isEqualTo("APOLLO_BLOCKED");
    assertThat(cycle.getLastApolloFailureCode()).isEqualTo("PROVIDER_PREFLIGHT_REQUIRED");
    verify(salesVideoService, never()).requestRender(any(), any());
  }

  /** Reaproveita montagem existente em pós-produção sem pagar por outra geração. */
  @Test
  void shouldReuseFailedRenderAssetBeforeAnyPaidReplacement() {
    VideoProductionCycle cycle = cycle();
    cycle.setStatus("QUEUED_FOR_APOLLO");
    cycle.setFinancialDecision("APPROVED");
    cycle.setSalesVideoJobId(21105L);
    SalesVideoJob failed = new SalesVideoJob();
    failed.setId(21105L);
    failed.setStatus(SalesVideoStatus.VIDEO_FAILED);
    failed.setFailureCode("RENDER_DURATION_SHORT");
    failed.setAsset(Asset.builder().id(2420L).build());
    VideoProject project = project();
    project.setCaptionPlan("Presença elegante em sete dias.");
    SalesVideoJobDto postProduction = new SalesVideoJobDto();
    postProduction.setId(21107L);
    when(repository.findByStatusAndFinancialDecisionOrderByCreatedAtAsc(
            "QUEUED_FOR_APOLLO", "APPROVED"))
        .thenReturn(java.util.List.of(cycle));
    when(jobRepository.findById(21105L)).thenReturn(Optional.of(failed));
    when(jobRepository.findFirstByRetryOfJob_IdOrderByRequestedAtDesc(21105L))
        .thenReturn(Optional.empty());
    when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
    when(salesVideoService.requestPostProduction(any(), any())).thenReturn(postProduction);

    service.reconcileApolloQueue();

    ArgumentCaptor<RequestSalesVideoPostProductionRequest> post =
        ArgumentCaptor.forClass(RequestSalesVideoPostProductionRequest.class);
    verify(salesVideoService)
        .requestPostProduction(org.mockito.ArgumentMatchers.eq(21105L), post.capture());
    assertThat(post.getValue().getCaptionText()).isEqualTo("Presença elegante em sete dias.");
    assertThat(cycle.getStatus()).isEqualTo("REUSING_APOLLO_MATERIAL");
    assertThat(cycle.getSalesVideoJobId()).isEqualTo(21107L);
    verify(salesVideoService, never()).requestRender(any(), any());
  }

  /** Reutiliza o job de pós-produção existente sem duplicar encaminhamento nem geração paga. */
  @Test
  void shouldReuseExistingPostProductionJobIdempotently() {
    VideoProductionCycle cycle = cycle();
    cycle.setStatus("QUEUED_FOR_APOLLO");
    cycle.setFinancialDecision("APPROVED");
    cycle.setSalesVideoJobId(21105L);
    SalesVideoJob failed = new SalesVideoJob();
    failed.setId(21105L);
    failed.setStatus(SalesVideoStatus.VIDEO_FAILED);
    failed.setFailureCode("RENDER_DURATION_SHORT");
    failed.setAsset(Asset.builder().id(2420L).build());
    SalesVideoJob existingPostProduction = new SalesVideoJob();
    existingPostProduction.setId(21107L);
    VideoProject project = project();
    project.setCaptionPlan("Presença elegante em sete dias.");
    when(repository.findByStatusAndFinancialDecisionOrderByCreatedAtAsc(
            "QUEUED_FOR_APOLLO", "APPROVED"))
        .thenReturn(java.util.List.of(cycle));
    when(jobRepository.findById(21105L)).thenReturn(Optional.of(failed));
    when(jobRepository.findFirstByRetryOfJob_IdOrderByRequestedAtDesc(21105L))
        .thenReturn(Optional.of(existingPostProduction));
    when(projectRepository.findById(7L)).thenReturn(Optional.of(project));

    service.reconcileApolloQueue();

    assertThat(cycle.getSalesVideoJobId()).isEqualTo(21107L);
    assertThat(cycle.getStatus()).isEqualTo("REUSING_APOLLO_MATERIAL");
    verify(salesVideoService, never()).requestPostProduction(any(), any());
    verify(salesVideoService, never()).requestRender(any(), any());
  }

  /** Persiste o prompt e a resposta de Plutus sem decidir nem enfileirar Apolo. */
  @Test
  void shouldAuditPlutusReviewBeforeFinancialDecision() {
    VideoProductionCycle cycle = cycle();
    cycle.setAgentTaskId(99L);
    when(repository.findById(11L)).thenReturn(Optional.of(cycle));
    AgentTaskExecutionAuditRequest audit =
        new AgentTaskExecutionAuditRequest(
            "MODEL",
            "gpt-5.6-sol",
            "high",
            "agente\n\natividade",
            "agente",
            "atividade",
            java.util.List.of());

    service.auditFinancialReview(
        11L,
        new VideoProductionCycleContracts.FinancialReviewAuditRequest(
            "{\"decision\":\"APPROVED\"}", audit, java.util.List.of()));

    verify(taskService)
        .recordPendingGateModelResult(
            "financial-agent", 99L, "{\"decision\":\"APPROVED\"}", audit, java.util.List.of());
    verify(salesVideoService, never()).requestRender(any(), any());
  }

  /** Cria o projeto mínimo de teste com perfil operacional. */
  private VideoProject project() {
    return VideoProject.builder()
        .id(7L)
        .productId(76L)
        .commercialPlanId(5L)
        .experimentId(88L)
        .salesVideoProfileId(13L)
        .title("MUSA v7")
        .targetDurationSeconds(15)
        .build();
  }

  /** Cria um projeto premium reutilizável com as duas referências governadas. */
  private VideoProject productUgcProject() {
    VideoProject project = project();
    project.setExperimentId(91L);
    project.setProviderPlan(
        "Provider escolhido no Estudio: Runway Product UGC Premium (RUNWAY_PRODUCT_UGC).");
    project.setCharacterPerformanceType("image");
    project.setCharacterPerformanceUri("https://assets.example/apresentadora.png");
    project.setReferencePerformanceUri("https://assets.example/musa-pde.png");
    project.setPerformanceConsentEvidence("consentimento-91");
    project.setPerformanceRightsEvidence("direitos-91");
    project.setCaptionPlan(
        "Você se arruma, mas ainda sente que falta presença? | Faça o diagnóstico gratuito.");
    project.setCtaText("Faça o diagnóstico gratuito");
    project.setSoundtrackPlan("Trilha leve");
    return project;
  }

  /** Cria um ciclo pendente sem qualquer consumo. */
  private VideoProductionCycle cycle() {
    VideoProductionCycle cycle = new VideoProductionCycle();
    cycle.setId(11L);
    cycle.setVideoProjectId(7L);
    cycle.setProductId(76L);
    cycle.setCommercialPlanId(5L);
    cycle.setStatus("PENDING_FINANCIAL_REVIEW");
    cycle.setBudgetLimitUsd(new BigDecimal("12.50"));
    cycle.setKnownCostUsd(BigDecimal.ZERO);
    cycle.setCreatedAt(Instant.now());
    cycle.setUpdatedAt(Instant.now());
    return cycle;
  }

  /** Cria uma decisão financeira com os campos recomendados do preflight preenchidos. */
  private VideoProductionCycleContracts.FinancialDecisionRequest financialDecision(
      String decision, String reason, String agentKey) {
    return new VideoProductionCycleContracts.FinancialDecisionRequest(
        decision,
        reason,
        agentKey,
        "Runway",
        "router/final-campaign",
        new BigDecimal("4.00"),
        "Dry run oficial e teto do ciclo.",
        "NO_PURCHASE",
        BigDecimal.ZERO,
        null);
  }

  /** Cria uma reserva ativa usada para comprovar o bloqueio anterior à fila de Apolo. */
  private VideoCreditReservation activeReservation() {
    VideoCreditReservation reservation = new VideoCreditReservation();
    reservation.setId(77L);
    reservation.setStatus("RESERVED");
    reservation.setReservedCredits(new BigDecimal("500"));
    reservation.setReservedCostUsd(new BigDecimal("5.00"));
    reservation.setExpiresAt(Instant.now().plusSeconds(600));
    return reservation;
  }

  /** Cria a tarefa de gate devolvida pela mesa de agentes após o preflight. */
  private AgentTaskResponse financialGateTask() {
    return new AgentTaskResponse(
        99L,
        4L,
        "financial-agent",
        "Plutus",
        "AGENT",
        8L,
        "videomaker",
        "Apolo",
        "Avaliar",
        "Ciclo",
        "HIGH",
        "PENDING",
        "cycle",
        "GATE_DECISION",
        "VIDEO_PROVIDER_COST_BENEFIT_APPROVAL",
        "PENDING",
        null,
        null,
        Instant.now(),
        Instant.now());
  }
}
