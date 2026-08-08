package com.marketinghub.growthoperator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsDetailedEventDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsEvidenceDto;
import com.marketinghub.experiment.video.service.ExperimentVideoPerformanceDashboardService;
import com.marketinghub.growthoperator.GrowthOperatorDecision;
import com.marketinghub.growthoperator.GrowthOperatorExecution;
import com.marketinghub.growthoperator.GrowthOperatorExecutionStatus;
import com.marketinghub.growthoperator.service.result.CompleteGrowthOperatorRequest;
import com.marketinghub.growthoperator.service.start.StartGrowthOperatorRequest;
import com.marketinghub.growthoperator.service.view.GrowthOperatorExecutionResponse;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanWeekObjective;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.repository.jpa.growthoperator.GrowthOperatorExecutionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanWeekObjectiveRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.VideoProjectStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: validar o contexto auditável entregue ao Operador de Crescimento. */
class GrowthOperatorServiceTest {

  /** Simula a persistencia e a releitura usadas para fixar a versao do agente na execucao. */
  private void mockVersionedSave(GrowthOperatorExecutionRepository repository) {
    GrowthOperatorExecution[] saved = new GrowthOperatorExecution[1];
    when(repository.save(any(GrowthOperatorExecution.class)))
        .thenAnswer(
            invocation -> {
              GrowthOperatorExecution execution = invocation.getArgument(0);
              if (execution.getId() == null) {
                execution.setId(100L);
              }
              saved[0] = execution;
              return execution;
            });
    when(repository.findById(any()))
        .thenAnswer(invocation -> java.util.Optional.ofNullable(saved[0]));
  }

  /** Confirma que o Operador recebe o contrato estrategico congelado do experimento. */
  @Test
  void shouldFreezeExperimentStrategicContractInEvidenceSnapshot() throws Exception {
    GrowthOperatorExecutionRepository repository = mock(GrowthOperatorExecutionRepository.class);
    CommercialPlanService planService = mock(CommercialPlanService.class);
    Experiment experiment =
        Experiment.builder()
            .id(82L)
            .name("Microamostra Nail Design")
            .commercialObjective(
                "Validar a microamostra. Continuar com 10% de briefings; ajustar sem solicitacoes; parar sem instrumentacao.")
            .hypothesis("A amostra personalizada reduz o risco percebido.")
            .primaryVariable("Microamostra gratuita")
            .primaryMetric("briefing_conversion_rate")
            .targetCvr(new BigDecimal("10.00"))
            .sampleSize(100)
            .build();
    CommercialPlan plan =
        CommercialPlan.builder()
            .id(2L)
            .commercialObjective("Gerar cinco vendas")
            .experiment(experiment)
            .build();
    when(planService.getPlan(2L)).thenReturn(plan);
    when(repository.findByCommercialPlanIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());
    mockVersionedSave(repository);
    ExperimentFunnelService funnelService = mock(ExperimentFunnelService.class);
    when(funnelService.buildDetailedAnalyticsEvidence(82L, 2000))
        .thenReturn(mock(ExperimentLandingAnalyticsEvidenceDto.class));
    GrowthOperatorService service =
        new GrowthOperatorService(
            repository,
            mock(com.marketinghub.repository.jpa.growthoperator.GrowthOperatorTaskRepository.class),
            planService,
            mock(CommercialPlanWeekObjectiveRepository.class),
            funnelService,
            mock(VideoProjectRepository.class),
            mock(ExperimentVideoPerformanceDashboardService.class),
            mock(com.marketinghub.experiment.service.ExperimentService.class),
            new ObjectMapper().findAndRegisterModules());

    service.start(2L, new StartGrowthOperatorRequest(1, null));

    ArgumentCaptor<GrowthOperatorExecution> captor =
        ArgumentCaptor.forClass(GrowthOperatorExecution.class);
    verify(repository).save(captor.capture());
    JsonNode contract =
        new ObjectMapper()
            .findAndRegisterModules()
            .readTree(captor.getValue().getEvidenceSnapshot())
            .path("experimentStrategicContract");
    assertThat(contract.path("source").asText()).isEqualTo("EXPERIMENT");
    assertThat(contract.path("experimentId").asLong()).isEqualTo(82L);
    assertThat(contract.path("objectiveHypothesisMetricsAndDecisionCriteria").asText())
        .contains("Continuar com 10%", "ajustar", "parar");
    assertThat(contract.path("primaryMetric").asText()).isEqualTo("briefing_conversion_rate");
    assertThat(contract.path("targetConversionRate").decimalValue()).isEqualByComparingTo("10.00");
    assertThat(contract.path("complete").asBoolean()).isTrue();
  }

  /** Confirma que o catalogo publico reflete todas as ferramentas MCP autorizadas. */
  @Test
  void shouldExposeAllReadOnlyMcpTools() {
    GrowthOperatorService service =
        new GrowthOperatorService(
            mock(GrowthOperatorExecutionRepository.class),
            mock(com.marketinghub.repository.jpa.growthoperator.GrowthOperatorTaskRepository.class),
            mock(CommercialPlanService.class),
            mock(CommercialPlanWeekObjectiveRepository.class),
            mock(ExperimentFunnelService.class),
            mock(VideoProjectRepository.class),
            mock(ExperimentVideoPerformanceDashboardService.class),
            mock(com.marketinghub.experiment.service.ExperimentService.class),
            new ObjectMapper());

    var tools = service.listMcpTools();

    assertThat(tools)
        .extracting(tool -> tool.name())
        .containsExactly(
            "consultar_planejamento",
            "consultar_funil",
            "consultar_sessoes",
            "consultar_campanhas",
            "consultar_memoria",
            "consultar_estrategia_videos",
            "consultar_pendencias",
            "solicitar_pausa_experimento",
            "solicitar_retomada_experimento");
    assertThat(tools.subList(0, 6)).allMatch(tool -> "SOMENTE_LEITURA".equals(tool.accessMode()));
  }

  /** Impede novo consumo automatico quando nenhuma evidencia operacional mudou. */
  @Test
  void shouldReuseLatestAutomaticCycleWhenEvidenceDidNotChange() {
    GrowthOperatorExecutionRepository repository = mock(GrowthOperatorExecutionRepository.class);
    var taskRepository =
        mock(com.marketinghub.repository.jpa.growthoperator.GrowthOperatorTaskRepository.class);
    CommercialPlanService planService = mock(CommercialPlanService.class);
    CommercialPlan plan = CommercialPlan.builder().id(2L).commercialObjective("Vender").build();
    when(planService.getPlan(2L)).thenReturn(plan);
    when(planService.synchronizeRunningExperiment(2L)).thenReturn(plan);
    when(repository.findFirstByCommercialPlanIdOrderByCreatedAtDesc(2L))
        .thenReturn(java.util.Optional.empty());
    when(repository.findByCommercialPlanIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());
    mockVersionedSave(repository);
    GrowthOperatorService service =
        new GrowthOperatorService(
            repository,
            taskRepository,
            planService,
            mock(CommercialPlanWeekObjectiveRepository.class),
            mock(ExperimentFunnelService.class),
            mock(VideoProjectRepository.class),
            mock(ExperimentVideoPerformanceDashboardService.class),
            mock(com.marketinghub.experiment.service.ExperimentService.class),
            new ObjectMapper().findAndRegisterModules());

    service.ensureAutomaticCycle(2L);
    ArgumentCaptor<GrowthOperatorExecution> captor =
        ArgumentCaptor.forClass(GrowthOperatorExecution.class);
    verify(repository).save(captor.capture());
    GrowthOperatorExecution latest = captor.getValue();
    latest.setCreatedAt(Instant.now().minusSeconds(3600));
    when(repository.findFirstByCommercialPlanIdOrderByCreatedAtDesc(2L))
        .thenReturn(java.util.Optional.of(latest));
    when(repository.findByCommercialPlanIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(latest));
    clearInvocations(repository);

    GrowthOperatorExecutionResponse response = service.ensureAutomaticCycle(2L);

    assertThat(response.evidenceFingerprint()).isEqualTo(latest.getEvidenceFingerprint());
    verify(repository, never()).save(any(GrowthOperatorExecution.class));
  }

  /** Impede que a propria criacao de pendencia dispare outro ciclo sem evidencia comercial nova. */
  @Test
  void shouldIgnoreOperatorTasksWhenDetectingCommercialEvidenceChanges() {
    GrowthOperatorExecutionRepository repository = mock(GrowthOperatorExecutionRepository.class);
    var taskRepository =
        mock(com.marketinghub.repository.jpa.growthoperator.GrowthOperatorTaskRepository.class);
    CommercialPlanService planService = mock(CommercialPlanService.class);
    CommercialPlan plan = CommercialPlan.builder().id(2L).commercialObjective("Vender").build();
    when(planService.getPlan(2L)).thenReturn(plan);
    when(planService.synchronizeRunningExperiment(2L)).thenReturn(plan);
    when(repository.findFirstByCommercialPlanIdOrderByCreatedAtDesc(2L))
        .thenReturn(java.util.Optional.empty());
    when(repository.findByCommercialPlanIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());
    when(taskRepository.findByCommercialPlanIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());
    mockVersionedSave(repository);
    GrowthOperatorService service =
        new GrowthOperatorService(
            repository,
            taskRepository,
            planService,
            mock(CommercialPlanWeekObjectiveRepository.class),
            mock(ExperimentFunnelService.class),
            mock(VideoProjectRepository.class),
            mock(ExperimentVideoPerformanceDashboardService.class),
            mock(com.marketinghub.experiment.service.ExperimentService.class),
            new ObjectMapper().findAndRegisterModules());

    service.ensureAutomaticCycle(2L);
    ArgumentCaptor<GrowthOperatorExecution> captor =
        ArgumentCaptor.forClass(GrowthOperatorExecution.class);
    verify(repository).save(captor.capture());
    GrowthOperatorExecution latest = captor.getValue();
    latest.setCreatedAt(Instant.now().minusSeconds(3600));
    when(repository.findFirstByCommercialPlanIdOrderByCreatedAtDesc(2L))
        .thenReturn(java.util.Optional.of(latest));
    when(repository.findByCommercialPlanIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(latest));
    var openTask = mock(com.marketinghub.growthoperator.GrowthOperatorTask.class);
    when(openTask.getId()).thenReturn(1L);
    when(openTask.getCommercialPlan()).thenReturn(plan);
    when(openTask.getSourceExecution()).thenReturn(latest);
    when(openTask.getActionText()).thenReturn("Auditar funil");
    when(openTask.getStatus())
        .thenReturn(com.marketinghub.growthoperator.GrowthOperatorTaskStatus.OPEN);
    when(taskRepository.findByCommercialPlanIdOrderByCreatedAtDesc(2L))
        .thenReturn(List.of(openTask));
    clearInvocations(repository);

    service.ensureAutomaticCycle(2L);

    verify(repository, never()).save(any(GrowthOperatorExecution.class));
  }

  /** Recupera execucao sem heartbeat e cria novo ciclo com o experimento ativo reconciliado. */
  @Test
  void shouldRecoverStaleRunningExecutionForReconciledExperiment() throws Exception {
    GrowthOperatorExecutionRepository repository = mock(GrowthOperatorExecutionRepository.class);
    CommercialPlanService planService = mock(CommercialPlanService.class);
    Experiment runningExperiment =
        Experiment.builder()
            .id(85L)
            .name("Agenda Cheia")
            .status(com.marketinghub.experiment.ExperimentStatus.RUNNING)
            .build();
    CommercialPlan plan =
        CommercialPlan.builder()
            .id(2L)
            .commercialObjective("Gerar cinco vendas")
            .experiment(runningExperiment)
            .build();
    when(planService.getPlan(2L)).thenReturn(plan);
    when(planService.synchronizeRunningExperiment(2L)).thenReturn(plan);
    GrowthOperatorExecution stale = new GrowthOperatorExecution();
    stale.setId(84L);
    stale.setCommercialPlan(plan);
    stale.setStatus(GrowthOperatorExecutionStatus.RUNNING);
    stale.setStartedAt(Instant.now().minusSeconds(600));
    stale.setCreatedAt(Instant.now().minusSeconds(600));
    stale.setCycleNumber(5);
    stale.setAutomaticCycle(true);
    when(repository.findFirstByCommercialPlanIdOrderByCreatedAtDesc(2L))
        .thenReturn(java.util.Optional.of(stale));
    when(repository.findByCommercialPlanIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(stale));
    when(repository.countRecentActiveTelemetry(any(), any())).thenReturn(0L);
    mockVersionedSave(repository);
    GrowthOperatorService service =
        new GrowthOperatorService(
            repository,
            mock(com.marketinghub.repository.jpa.growthoperator.GrowthOperatorTaskRepository.class),
            planService,
            mock(CommercialPlanWeekObjectiveRepository.class),
            mock(ExperimentFunnelService.class),
            mock(VideoProjectRepository.class),
            mock(ExperimentVideoPerformanceDashboardService.class),
            mock(com.marketinghub.experiment.service.ExperimentService.class),
            new ObjectMapper().findAndRegisterModules());

    GrowthOperatorExecutionResponse response = service.ensureAutomaticCycle(2L);

    assertThat(stale.getStatus()).isEqualTo(GrowthOperatorExecutionStatus.FAILED);
    assertThat(stale.getErrorMessage()).contains("nenhum heartbeat vivo");
    assertThat(response.status()).isEqualTo(GrowthOperatorExecutionStatus.PENDING);
    assertThat(
            new ObjectMapper().readTree(response.evidenceSnapshot()).path("experimentId").asLong())
        .isEqualTo(85L);
  }

  /** Confirma que o novo ciclo recebe conclusoes e resultados observados de todo o historico. */
  @Test
  void shouldFreezeConsolidatedPlanningMemory() throws Exception {
    GrowthOperatorExecutionRepository repository = mock(GrowthOperatorExecutionRepository.class);
    CommercialPlanService planService = mock(CommercialPlanService.class);
    ExperimentFunnelService funnelService = mock(ExperimentFunnelService.class);
    CommercialPlan plan = CommercialPlan.builder().id(2L).build();
    GrowthOperatorExecution completed = new GrowthOperatorExecution();
    completed.setCycleNumber(3);
    completed.setStatus(GrowthOperatorExecutionStatus.COMPLETED);
    completed.setRecommendedAction("Validar CTA em uma sessao humana");
    completed.setDiagnosisJson(
        "{\"rootCause\":\"amostra humana insuficiente\",\"evidence\":[\"1 sessao humana\"]}");
    completed.setDailyReport("Relatorio executivo do ciclo tres.");
    completed.setEvidenceSnapshot(
        "{\"actualCost\":12.13,\"actualRevenue\":0,\"blocker\":\"INSTRUMENTACAO\"}");
    completed.setCreatedAt(Instant.parse("2026-08-04T10:00:00Z"));
    completed.setFinishedAt(Instant.parse("2026-08-04T10:02:00Z"));
    when(planService.getPlan(2L)).thenReturn(plan);
    when(repository.findByCommercialPlanIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(completed));
    mockVersionedSave(repository);
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    GrowthOperatorService service =
        new GrowthOperatorService(
            repository,
            mock(com.marketinghub.repository.jpa.growthoperator.GrowthOperatorTaskRepository.class),
            planService,
            mock(CommercialPlanWeekObjectiveRepository.class),
            funnelService,
            mock(VideoProjectRepository.class),
            mock(ExperimentVideoPerformanceDashboardService.class),
            mock(com.marketinghub.experiment.service.ExperimentService.class),
            objectMapper);

    service.start(2L, new StartGrowthOperatorRequest(1, "Reavaliar o gargalo"));

    ArgumentCaptor<GrowthOperatorExecution> captor =
        ArgumentCaptor.forClass(GrowthOperatorExecution.class);
    verify(repository).save(captor.capture());
    JsonNode snapshot = objectMapper.readTree(captor.getValue().getEvidenceSnapshot());
    assertThat(snapshot.at("/consolidatedMemory/totalCycles").asInt()).isEqualTo(1);
    assertThat(snapshot.at("/consolidatedMemory/completedCycles").asInt()).isEqualTo(1);
    assertThat(snapshot.at("/consolidatedMemory/timeline/0/conclusion").asText())
        .isEqualTo("amostra humana insuficiente");
    assertThat(
            snapshot
                .at("/consolidatedMemory/timeline/0/recommendedActionNotConfirmedAsExecuted")
                .asText())
        .isEqualTo("Validar CTA em uma sessao humana");
    assertThat(
            snapshot
                .at("/consolidatedMemory/timeline/0/observedPlanMetrics/actualCost")
                .decimalValue())
        .isEqualByComparingTo("12.13");
  }

  /** Confirma que eventos detalhados de sessão entram no snapshot do ciclo. */
  @Test
  void shouldFreezeDetailedSessionEvidence() throws Exception {
    GrowthOperatorExecutionRepository repository = mock(GrowthOperatorExecutionRepository.class);
    CommercialPlanService planService = mock(CommercialPlanService.class);
    ExperimentFunnelService funnelService = mock(ExperimentFunnelService.class);
    Experiment experiment = new Experiment();
    experiment.setId(81L);
    CommercialPlan plan = CommercialPlan.builder().id(2L).experiment(experiment).build();
    when(planService.getPlan(2L)).thenReturn(plan);
    when(repository.findByCommercialPlanIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());
    mockVersionedSave(repository);
    var event =
        new ExperimentLandingAnalyticsDetailedEventDto(
            10L,
            "visitor-a1b2c3",
            "session-d4e5f6",
            "checkout_click",
            "offer",
            Instant.parse("2026-08-04T10:00:00Z"),
            Map.of("deviceType", "mobile"));
    when(funnelService.buildDetailedAnalyticsEvidence(81L, 2000))
        .thenReturn(
            new ExperimentLandingAnalyticsEvidenceDto(81L, 1, 1, false, null, List.of(event)));
    when(funnelService.buildDetailedPdeAnalyticsEvidence(81L))
        .thenReturn(Map.of("available", false));
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    GrowthOperatorService service =
        new GrowthOperatorService(
            repository,
            mock(com.marketinghub.repository.jpa.growthoperator.GrowthOperatorTaskRepository.class),
            planService,
            mock(CommercialPlanWeekObjectiveRepository.class),
            funnelService,
            mock(VideoProjectRepository.class),
            mock(ExperimentVideoPerformanceDashboardService.class),
            mock(com.marketinghub.experiment.service.ExperimentService.class),
            objectMapper);

    service.start(2L, new StartGrowthOperatorRequest(1, "Diagnosticar funil"));

    ArgumentCaptor<GrowthOperatorExecution> captor =
        ArgumentCaptor.forClass(GrowthOperatorExecution.class);
    verify(repository).save(captor.capture());
    var snapshot = objectMapper.readTree(captor.getValue().getEvidenceSnapshot());
    assertThat(snapshot.at("/experimentId").asLong()).isEqualTo(81L);
    assertThat(snapshot.at("/sessionIntelligence/landingAnalytics/includedEvents").asInt())
        .isEqualTo(1);
    assertThat(
            snapshot
                .at("/sessionIntelligence/landingAnalytics/detailedEvents/0/anonymousSessionId")
                .asText())
        .isEqualTo("session-d4e5f6");
  }

  /** Confirma que o agente pode atualizar a leitura detalhada pela API do planejamento. */
  @Test
  void shouldExposeDetailedSessionIntelligenceForDirectApiConsumption() {
    GrowthOperatorExecutionRepository repository = mock(GrowthOperatorExecutionRepository.class);
    CommercialPlanService planService = mock(CommercialPlanService.class);
    ExperimentFunnelService funnelService = mock(ExperimentFunnelService.class);
    Experiment experiment = new Experiment();
    experiment.setId(81L);
    CommercialPlan plan = CommercialPlan.builder().id(2L).experiment(experiment).build();
    when(planService.getPlan(2L)).thenReturn(plan);
    when(funnelService.buildDetailedAnalyticsEvidence(81L, 2000))
        .thenReturn(new ExperimentLandingAnalyticsEvidenceDto(81L, 0, 0, false, null, List.of()));
    when(funnelService.buildPersonalizedSampleDeliveryEvidence(81L))
        .thenReturn(Map.of("deliveredEmails", 1L, "openedEmails", 1L));
    when(funnelService.buildDetailedPdeAnalyticsEvidence(81L))
        .thenReturn(Map.of("available", false));
    GrowthOperatorService service =
        new GrowthOperatorService(
            repository,
            mock(com.marketinghub.repository.jpa.growthoperator.GrowthOperatorTaskRepository.class),
            planService,
            mock(CommercialPlanWeekObjectiveRepository.class),
            funnelService,
            mock(VideoProjectRepository.class),
            mock(ExperimentVideoPerformanceDashboardService.class),
            mock(com.marketinghub.experiment.service.ExperimentService.class),
            new ObjectMapper().findAndRegisterModules());

    Map<String, Object> result = service.sessionIntelligence(2L, 5000);

    assertThat(result.get("planId")).isEqualTo(2L);
    assertThat(result.get("experimentId")).isEqualTo(81L);
    assertThat(result.get("appliedEventLimit")).isEqualTo(2000);
    assertThat(result.get("personalizedSampleDelivery"))
        .isEqualTo(Map.of("deliveredEmails", 1L, "openedEmails", 1L));
    verify(funnelService).buildDetailedAnalyticsEvidence(81L, 2000);
    verify(funnelService).buildPersonalizedSampleDeliveryEvidence(81L);
  }

  /** Confirma que o agente recebe estrategia e custos sem depender da tela do Estudio. */
  @Test
  void shouldExposeVideoStrategyIntelligenceForDailyLearning() {
    GrowthOperatorExecutionRepository repository = mock(GrowthOperatorExecutionRepository.class);
    CommercialPlanService planService = mock(CommercialPlanService.class);
    ExperimentFunnelService funnelService = mock(ExperimentFunnelService.class);
    VideoProjectRepository videoRepository = mock(VideoProjectRepository.class);
    ExperimentVideoPerformanceDashboardService videoPerformanceService =
        mock(ExperimentVideoPerformanceDashboardService.class);
    Experiment experiment = new Experiment();
    experiment.setId(81L);
    CommercialPlan plan =
        CommercialPlan.builder()
            .id(2L)
            .experiment(experiment)
            .actualCampaignCost(new BigDecimal("53.32"))
            .actualTotalCost(new BigDecimal("56.02"))
            .actualRevenue(BigDecimal.ZERO)
            .build();
    VideoProject project =
        VideoProject.builder()
            .id(1L)
            .experimentId(81L)
            .title("MUSA - qualificacao pela dor")
            .strategyGroupKey("musa-two-video-funnel-v1")
            .strategyRole("CAMPAIGN_QUALIFICATION")
            .commercialHypothesis("Qualificar pela identificacao com a dor")
            .learningDecision("COLLECTING")
            .status(VideoProjectStatus.READY_FOR_SCRIPT)
            .build();
    when(planService.getPlan(2L)).thenReturn(plan);
    when(videoRepository.findByExperimentIdOrderByUpdatedAtDesc(81L)).thenReturn(List.of(project));
    GrowthOperatorService service =
        new GrowthOperatorService(
            repository,
            mock(com.marketinghub.repository.jpa.growthoperator.GrowthOperatorTaskRepository.class),
            planService,
            mock(CommercialPlanWeekObjectiveRepository.class),
            funnelService,
            videoRepository,
            videoPerformanceService,
            mock(com.marketinghub.experiment.service.ExperimentService.class),
            new ObjectMapper().findAndRegisterModules());

    Map<String, Object> result = service.videoStrategyIntelligence(2L);

    assertThat(result.get("strategyCount")).isEqualTo(1);
    assertThat(result.get("commercialCost")).isEqualTo(new BigDecimal("53.32"));
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> strategies = (List<Map<String, Object>>) result.get("strategies");
    assertThat(strategies.get(0).get("strategyGroupKey")).isEqualTo("musa-two-video-funnel-v1");
    assertThat(strategies.get(0).get("learningDecision")).isEqualTo("COLLECTING");
  }

  /** Confirma que o snapshot separa teto mensal, semana vigente e primeiro gate preventivo. */
  @Test
  void shouldFreezeWeeklyGoalsAndSpendGovernance() throws Exception {
    GrowthOperatorExecutionRepository repository = mock(GrowthOperatorExecutionRepository.class);
    CommercialPlanService planService = mock(CommercialPlanService.class);
    CommercialPlanWeekObjectiveRepository objectiveRepository =
        mock(CommercialPlanWeekObjectiveRepository.class);
    CommercialPlan plan =
        CommercialPlan.builder()
            .id(2L)
            .deadline(java.time.LocalDate.of(2026, 8, 31))
            .maxBudget(new BigDecimal("400"))
            .actualTotalCost(new BigDecimal("80"))
            .actualRevenue(BigDecimal.ZERO)
            .stopCriteria("Revisar em R$ 75; bloquear em R$ 175 sem venda.")
            .build();
    when(planService.getPlan(2L)).thenReturn(plan);
    when(repository.findByCommercialPlanIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());
    mockVersionedSave(repository);
    when(objectiveRepository.findByPlanIdAndWeekNumberOrderBySequenceOrderAsc(2L, 1))
        .thenReturn(
            List.of(
                CommercialPlanWeekObjective.builder()
                    .objectiveText("Gerar cinco vendas ate 09/08")
                    .build()));
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    GrowthOperatorService service =
        new GrowthOperatorService(
            repository,
            mock(com.marketinghub.repository.jpa.growthoperator.GrowthOperatorTaskRepository.class),
            planService,
            objectiveRepository,
            mock(ExperimentFunnelService.class),
            mock(VideoProjectRepository.class),
            mock(ExperimentVideoPerformanceDashboardService.class),
            mock(com.marketinghub.experiment.service.ExperimentService.class),
            objectMapper);

    service.start(2L, new StartGrowthOperatorRequest(1, "Cumprir meta semanal"));

    ArgumentCaptor<GrowthOperatorExecution> captor =
        ArgumentCaptor.forClass(GrowthOperatorExecution.class);
    verify(repository).save(captor.capture());
    JsonNode snapshot = objectMapper.readTree(captor.getValue().getEvidenceSnapshot());
    assertThat(snapshot.at("/maxBudget").decimalValue()).isEqualByComparingTo("400");
    assertThat(snapshot.at("/currentWeek/objectives/0").asText())
        .isEqualTo("Gerar cinco vendas ate 09/08");
    assertThat(snapshot.at("/spendGovernance/preventiveReviewGate").decimalValue())
        .isEqualByComparingTo("75");
    assertThat(snapshot.at("/spendGovernance/preventiveGateReachedWithoutRevenue").asBoolean())
        .isTrue();
  }

  /** Confirma que o backend nao aceita continuidade apos o primeiro gate sem receita. */
  @Test
  void shouldBlockContinueAtPreventiveGateWithoutRevenue() {
    GrowthOperatorExecutionRepository repository = mock(GrowthOperatorExecutionRepository.class);
    CommercialPlanService planService = mock(CommercialPlanService.class);
    CommercialPlan plan =
        CommercialPlan.builder()
            .id(2L)
            .actualTotalCost(new BigDecimal("80"))
            .actualRevenue(BigDecimal.ZERO)
            .stopCriteria("Revisar em R$ 75; bloquear em R$ 175 sem venda.")
            .build();
    GrowthOperatorExecution execution = new GrowthOperatorExecution();
    execution.setCommercialPlan(plan);
    execution.setAuthorityMode("READ_ONLY_DIAGNOSIS");
    when(repository.findById(10L)).thenReturn(java.util.Optional.of(execution));
    when(planService.getPlan(2L)).thenReturn(plan);
    when(repository.save(any(GrowthOperatorExecution.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    GrowthOperatorService service =
        new GrowthOperatorService(
            repository,
            mock(com.marketinghub.repository.jpa.growthoperator.GrowthOperatorTaskRepository.class),
            planService,
            mock(CommercialPlanWeekObjectiveRepository.class),
            mock(ExperimentFunnelService.class),
            mock(VideoProjectRepository.class),
            mock(ExperimentVideoPerformanceDashboardService.class),
            mock(com.marketinghub.experiment.service.ExperimentService.class),
            new ObjectMapper());

    GrowthOperatorExecutionResponse response =
        service.complete(
            10L,
            new CompleteGrowthOperatorRequest(
                "[]",
                "{}",
                "{}",
                "[]",
                GrowthOperatorDecision.CONTINUE,
                "Manter campanha",
                "relatorio",
                "gpt-5.6-sol",
                1L,
                1L,
                BigDecimal.ZERO));

    assertThat(response.recommendedDecision()).isEqualTo(GrowthOperatorDecision.WAIT_FOR_APPROVAL);
    assertThat(response.recommendedAction()).contains("Gate preventivo atingido");
  }

  /** Confirma que a pausa somente e delegada ao servico de experimento apos o gate comprovado. */
  @Test
  void shouldPauseRunningExperimentAtPreventiveGateWithoutRevenue() {
    CommercialPlanService planService = mock(CommercialPlanService.class);
    com.marketinghub.experiment.service.ExperimentService experimentService =
        mock(com.marketinghub.experiment.service.ExperimentService.class);
    Experiment experiment = new Experiment();
    experiment.setId(81L);
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.RUNNING);
    CommercialPlan plan =
        CommercialPlan.builder()
            .id(2L)
            .experiment(experiment)
            .actualTotalCost(new BigDecimal("80"))
            .actualRevenue(BigDecimal.ZERO)
            .stopCriteria("Revisar em R$ 75; bloquear em R$ 175 sem venda.")
            .build();
    when(planService.getPlan(2L)).thenReturn(plan);
    GrowthOperatorService service =
        new GrowthOperatorService(
            mock(GrowthOperatorExecutionRepository.class),
            mock(com.marketinghub.repository.jpa.growthoperator.GrowthOperatorTaskRepository.class),
            planService,
            mock(CommercialPlanWeekObjectiveRepository.class),
            mock(ExperimentFunnelService.class),
            mock(VideoProjectRepository.class),
            mock(ExperimentVideoPerformanceDashboardService.class),
            experimentService,
            new ObjectMapper());

    Map<String, Object> result =
        service.requestPreventivePause(
            2L,
            new com.marketinghub.growthoperator.service.action
                .GrowthOperatorExperimentActionRequest(
                "Gate preventivo atingido sem vendas", List.of("Custo R$ 80", "Receita R$ 0")));

    assertThat(result.get("executed")).isEqualTo(true);
    verify(experimentService)
        .pauseByGrowthOperator(
            org.mockito.ArgumentMatchers.eq(81L),
            org.mockito.ArgumentMatchers.contains("Custo R$ 80"));
  }

  /** Confirma que a retomada fica pendente e registra auditoria sem reativar. */
  @Test
  void shouldKeepResumeWaitingForHumanApproval() {
    CommercialPlanService planService = mock(CommercialPlanService.class);
    com.marketinghub.experiment.service.ExperimentService experimentService =
        mock(com.marketinghub.experiment.service.ExperimentService.class);
    Experiment experiment = new Experiment();
    experiment.setId(81L);
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.PAUSED);
    when(planService.getPlan(2L))
        .thenReturn(CommercialPlan.builder().id(2L).experiment(experiment).build());
    GrowthOperatorService service =
        new GrowthOperatorService(
            mock(GrowthOperatorExecutionRepository.class),
            mock(com.marketinghub.repository.jpa.growthoperator.GrowthOperatorTaskRepository.class),
            planService,
            mock(CommercialPlanWeekObjectiveRepository.class),
            mock(ExperimentFunnelService.class),
            mock(VideoProjectRepository.class),
            mock(ExperimentVideoPerformanceDashboardService.class),
            experimentService,
            new ObjectMapper());

    Map<String, Object> result =
        service.requestExperimentResume(
            2L,
            new com.marketinghub.growthoperator.service.action
                .GrowthOperatorExperimentActionRequest(
                "Instrumentacao corrigida e validada", List.of("Teste ponta a ponta aprovado")));

    assertThat(result.get("executed")).isEqualTo(false);
    assertThat(result.get("status")).isEqualTo("WAITING_HUMAN_APPROVAL");
    verify(experimentService)
        .requestResumeApprovalByGrowthOperator(
            org.mockito.ArgumentMatchers.eq(81L),
            org.mockito.ArgumentMatchers.contains("Teste ponta a ponta aprovado"));
  }
}
