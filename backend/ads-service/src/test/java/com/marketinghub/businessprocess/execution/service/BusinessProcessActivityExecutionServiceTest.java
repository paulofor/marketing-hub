package com.marketinghub.businessprocess.execution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskActivityCoverage;
import com.marketinghub.agenttask.AgentTaskResponse;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.AgentTaskVisualEvidence;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.agenttask.CreateAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutionResult;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityExecutor;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityExecutionResult;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityExecutor;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.ProductOriginExecutionReferenceResolver;
import com.marketinghub.repository.jpa.agenttask.AgentTaskActivityCoverageRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: comprovar limite, versões e auditoria do histórico de execuções BPM. */
class BusinessProcessActivityExecutionServiceTest {
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final BusinessProcessActivityExecutionService service =
      new BusinessProcessActivityExecutionService(processes, tasks, new ObjectMapper());

  /** Lista dez tarefas do processo canônico e preserva a versão real de cada execução. */
  @Test
  void returnsTenRecentExecutionsAcrossProcessVersions() {
    when(processes.findById(37L)).thenReturn(Optional.of(selectedProcess()));
    when(tasks.findRecentActivityExecutions(
            eq("pde-opportunity-discovery"), eq("evidence"), any(Pageable.class)))
        .thenReturn(LongStream.rangeClosed(1, 11).mapToObj(this::executionTask).toList());

    var result = service.recentExecutions(37L, "evidence");

    assertThat(result.processName()).isEqualTo("Descoberta e priorização da oportunidade PDE");
    assertThat(result.selectedProcessVersionNumber()).isEqualTo(4);
    assertThat(result.selectedProcessStatus()).isEqualTo("RETIRED");
    assertThat(result.activityName()).isEqualTo("Comprovar dor e demanda");
    assertThat(result.activityOwnerName()).isEqualTo("Argos");
    assertThat(result.executions()).hasSize(10);
    var execution = result.executions().getFirst();
    assertThat(execution.processDefinitionId()).isEqualTo(22L);
    assertThat(execution.processVersionNumber()).isEqualTo(1);
    assertThat(execution.comments()).contains("APPROVE");
    assertThat(execution.modelCode()).isEqualTo("gpt-5.4-mini-2026-03-17");
    assertThat(execution.reasoningEffort()).isEqualTo("high");
    assertThat(execution.promptSent()).isEqualTo("Comprove a dor com fontes independentes.");
    assertThat(execution.estimatedCostUsd()).isEqualByComparingTo("0.01347240");
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(tasks)
        .findRecentActivityExecutions(
            eq("pde-opportunity-discovery"), eq("evidence"), pageable.capture());
    assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
  }

  /** Expõe pixels e interpretação emocional da mesma tentativa para auditoria na tela. */
  @Test
  void exposesPsiqueVisualEvidenceAndPurchaseEmotion() {
    when(processes.findById(37L)).thenReturn(Optional.of(selectedProcess()));
    AgentTask task = executionTask(258L);
    task.getAssignedAgent().setAgentKey("customer-agent");
    task.getAssignedAgent().setNickname("Psique");
    task.setResultJson(
        """
        {
          "visualAudit":{
            "captureSessionId":"capture-258","mobileFirst":true,
            "fullPageEvidenceIds":[901],"fullPageContinuity":"Jornada contínua",
            "overallAestheticAssessment":"Estética coerente","foldAnalyses":[]
          },
          "purchaseEmotion":{
            "acquisitionExpectation":"Espero ganhar controle",
            "acquisitionAnxiety":"Receio material genérico",
            "expectedPostDeliveryFeeling":"Imagino sentir alívio",
            "emotionalTension":"Desejo versus receio",
            "evidenceBoundary":"Simulação baseada nas provas"
          }
        }
        """);
    AgentTaskVisualEvidence fullPage = new AgentTaskVisualEvidence();
    fullPage.setId(901L);
    fullPage.setTask(task);
    fullPage.setCaptureSessionId("capture-258");
    fullPage.setEvidenceKey("page-1-full");
    fullPage.setEvidenceType("FULL_PAGE");
    fullPage.setDeviceProfile("IPHONE_15_PRO");
    fullPage.setPageNumber(1);
    fullPage.setViewportWidth(393);
    fullPage.setViewportHeight(852);
    fullPage.setPageHeightPx(1704);
    fullPage.setScrollY(0);
    fullPage.setSourceUrl("https://rigel.example/jornada");
    fullPage.setFinalUrl("https://rigel.example/jornada");
    fullPage.setContentType("image/png");
    fullPage.setSizeBytes(1200L);
    fullPage.setSha256("a".repeat(64));
    fullPage.setCapturedAt(Instant.parse("2026-08-29T10:00:00Z"));
    task.getVisualEvidence().add(fullPage);
    when(tasks.findRecentActivityExecutions(
            eq("pde-opportunity-discovery"), eq("evidence"), any(Pageable.class)))
        .thenReturn(List.of(task));

    var execution = service.recentExecutions(37L, "evidence").executions().getFirst();

    assertThat(execution.visualEvidence())
        .singleElement()
        .satisfies(
            evidence -> {
              assertThat(evidence.id()).isEqualTo(901L);
              assertThat(evidence.contentUrl())
                  .isEqualTo("/api/agent-tasks/258/visual-evidence/901/content");
            });
    assertThat(execution.visualAudit().path("captureSessionId").asText()).isEqualTo("capture-258");
    assertThat(execution.purchaseEmotion().path("acquisitionAnxiety").asText())
        .isEqualTo("Receio material genérico");
  }

  /** Usa a última atualização como término somente para uma tarefa terminal sem entrega. */
  @Test
  void exposesTerminalFailureTimeWithoutFinishingPendingTask() {
    when(processes.findById(37L)).thenReturn(Optional.of(selectedProcess()));
    AgentTask blocked = executionTask(1L);
    blocked.setStatus("BLOCKED");
    blocked.setDeliveredAt(null);
    AgentTask pending = executionTask(2L);
    pending.setStatus("PENDING");
    pending.setReceivedAt(null);
    pending.setDeliveredAt(null);
    when(tasks.findRecentActivityExecutions(anyString(), anyString(), any(Pageable.class)))
        .thenReturn(List.of(blocked, pending));

    var executions = service.recentExecutions(37L, "evidence").executions();

    assertThat(executions.get(0).finishedAt()).isEqualTo(blocked.getUpdatedAt());
    assertThat(executions.get(1).startedAt()).isNull();
    assertThat(executions.get(1).finishedAt()).isNull();
  }

  /** Projeta prompt, parecer, modelo e duração reais da execução composta de Dédalo. */
  @Test
  void exposesTechnicalAuditForCoveredLandingActivities() {
    GeraLandingStageExecutionRepository landingExecutions =
        mock(GeraLandingStageExecutionRepository.class);
    var auditedService =
        new BusinessProcessActivityExecutionService(
            processes, tasks, landingExecutions, new ObjectMapper());
    BusinessProcessDefinition selected = selectedProcess();
    selected.setProcessCode("landing-page-generation");
    selected.setDiagramJson(
        "{\"nodes\":[{\"id\":\"select\",\"type\":\"TASK\","
            + "\"label\":\"Selecionar provas reais\",\"owner\":\"Dédalo\"}]}");
    when(processes.findById(37L)).thenReturn(Optional.of(selected));
    AgentTask task = executionTask(243L);
    task.getProcessDefinition().setProcessCode("landing-page-generation");
    task.getAssignedAgent().setAgentKey("landing-generator");
    task.getAssignedAgent().setNickname("Dédalo");
    task.setResultJson("{\"approvalRecommendation\":\"APPROVE_FOR_PUBLICATION\"}");
    task.setExecutionModelCode(null);
    task.setExecutionReasoningEffort(null);
    task.setExecutionPrompt(null);
    when(tasks.findRecentActivityExecutions(
            eq("landing-page-generation"), eq("select"), any(Pageable.class)))
        .thenReturn(List.of(task));
    GeraLandingStageExecution technical = new GeraLandingStageExecution();
    technical.setStageCode("landing-generation-agent-v1");
    technical.setStatus("CONCLUIDO");
    technical.setOpenAiModel("gpt-5.6-sol");
    technical.setExecutionReasoningEffort("high");
    technical.setPrompt("Prompt integral de Dédalo");
    technical.setModelResponse("{\"summary\":\"Provas selecionadas e landing construída\"}");
    technical.setProcessingStartedAt(Instant.parse("2026-08-27T03:26:45Z"));
    technical.setCompletedAt(Instant.parse("2026-08-27T03:35:14Z"));
    when(landingExecutions.findTop20ByStageCodeAndAutonomousCycleIdOrderByExecutionRequestedAtDesc(
            "landing-generation-agent-v1", "agent-task:243"))
        .thenReturn(List.of(technical));

    var execution = auditedService.recentExecutions(37L, "select").executions().getFirst();

    assertThat(execution.comments()).contains("Provas selecionadas");
    assertThat(execution.promptSent()).isEqualTo("Prompt integral de Dédalo");
    assertThat(execution.modelCode()).isEqualTo("gpt-5.6-sol");
    assertThat(execution.reasoningEffort()).isEqualTo("high");
    assertThat(execution.startedAt()).isEqualTo("2026-08-27T03:26:45Z");
    assertThat(execution.finishedAt()).isEqualTo("2026-08-27T03:35:14Z");
  }

  /** Agrupa tarefas do produto por atividade, preserva cobertura composta e não duplica custo. */
  @Test
  void returnsProductActivitiesAndUniqueTasksWithoutCrossProcessLeakage() {
    BusinessProcessActivityDefinitionRepository activityDefinitions =
        mock(BusinessProcessActivityDefinitionRepository.class);
    AgentTaskActivityCoverageRepository coverages = mock(AgentTaskActivityCoverageRepository.class);
    BusinessProcessActivityInstanceRepository instances =
        mock(BusinessProcessActivityInstanceRepository.class);
    CommercialPlanRepository commercialPlans = mock(CommercialPlanRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    var productService =
        new BusinessProcessActivityExecutionService(
            processes,
            activityDefinitions,
            tasks,
            coverages,
            instances,
            commercialPlans,
            null,
            products,
            null,
            null,
            new ObjectMapper());
    BusinessProcessDefinition landing = selectedProcess();
    landing.setId(18L);
    landing.setProcessCode("landing-page-generation");
    landing.setName("Geração de landing page");
    landing.setVersionNumber(4);
    landing.setStatus("PUBLISHED");
    when(processes.findById(18L)).thenReturn(Optional.of(landing));
    Product rigel = new Product();
    rigel.setId(9L);
    rigel.setName("Kit WhatsApp Pronto");
    rigel.setInternalName("Rigel");
    when(products.findById(9L)).thenReturn(Optional.of(rigel));
    CommercialPlan plan = new CommercialPlan();
    plan.setId(4L);
    plan.setName("Plano comercial do Rigel");
    CommercialPlan newerPlan = new CommercialPlan();
    newerPlan.setId(5L);
    newerPlan.setName("Próximo plano do Rigel");
    when(commercialPlans.findByProductId(9L)).thenReturn(List.of(newerPlan, plan));

    BusinessProcessActivityDefinition select =
        activity(119L, landing, "select", "Selecionar provas");
    BusinessProcessActivityDefinition html = activity(122L, landing, "html", "Construir HTML");
    BusinessProcessActivityDefinition customer =
        activity(124L, landing, "customer", "Avaliar percepção da cliente");
    BusinessProcessActivityDefinition technical =
        activity(123L, landing, "technical", "Validar técnica e fidelidade visual");
    BusinessProcessActivityDefinition human =
        activity(126L, landing, "human", "Aprovação humana para publicar");
    when(activityDefinitions.findAllByProcessDefinitionIdOrderByIdAsc(18L))
        .thenReturn(List.of(select, html, technical, customer, human));

    AgentTask compound = executionTask(243L);
    compound.setProcessDefinition(landing);
    compound.setProcessActivityId("html");
    compound.setProcessActivityName("Construir HTML");
    compound.setSourceReference("commercial-plan:4@v3:journey");
    compound.setEstimatedCostUsd(new BigDecimal("1.42804720"));
    compound.setActivityInstance(
        activityInstance(128L, html, "COMPLETED", true, null, compound.getUpdatedAt()));
    AgentTask customerReview = executionTask(244L);
    customerReview.setProcessDefinition(landing);
    customerReview.setProcessActivityId("customer");
    customerReview.setProcessActivityName("Avaliar percepção da cliente");
    customerReview.setSourceReference("commercial-plan:4@v3:journey");
    customerReview.setEstimatedCostUsd(new BigDecimal("0.18957680"));
    customerReview.setStatus("BLOCKED");
    customerReview.setExecutionError("Checkout canônico ausente na evidência de Psique.");
    customerReview.setActivityInstance(
        activityInstance(
            129L,
            customer,
            "BLOCKED",
            false,
            "Checkout canônico ausente na evidência de Psique.",
            customerReview.getUpdatedAt()));
    AgentTask oldTechnicalReview = executionTask(242L);
    oldTechnicalReview.setProcessDefinition(landing);
    oldTechnicalReview.setProcessActivityId("technical");
    oldTechnicalReview.setProcessActivityName("Validar técnica e fidelidade visual");
    oldTechnicalReview.setSourceReference("commercial-plan:4@v2:journey");
    oldTechnicalReview.setEstimatedCostUsd(BigDecimal.ZERO.setScale(8));
    oldTechnicalReview.setCostEstimationStatus("NOT_APPLICABLE");
    AgentTask anotherProcessTask = executionTask(245L);
    anotherProcessTask.getProcessDefinition().setProcessCode("another-process");
    anotherProcessTask.setSourceReference("commercial-plan:4@v3");
    when(tasks.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc("commercial-plan:4@"))
        .thenReturn(List.of(compound, customerReview, oldTechnicalReview, anotherProcessTask));
    BusinessProcessActivityInstance humanPending =
        activityInstance(
            130L, human, "PENDING", false, null, Instant.parse("2026-08-20T21:42:00Z"));
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceStartingWithOrderByCreatedAtDescIdDesc(
                "landing-page-generation", "commercial-plan:4@"))
        .thenReturn(
            List.of(
                compound.getActivityInstance(),
                customerReview.getActivityInstance(),
                humanPending));
    AgentTaskActivityCoverage compoundCoverage = new AgentTaskActivityCoverage();
    compoundCoverage.setAgentTask(compound);
    compoundCoverage.setActivityDefinition(select);
    when(coverages.findAllByAgentTaskIdIn(List.of(244L, 243L, 242L)))
        .thenReturn(List.of(compoundCoverage));

    var result = productService.productProcessExecutions(18L, 9L);

    assertThat(result.productInternalName()).isEqualTo("Rigel");
    assertThat(result.commercialPlanId()).isEqualTo(4L);
    assertThat(result.commercialPlanName()).isEqualTo("Plano comercial do Rigel");
    assertThat(result.processName()).isEqualTo("Geração de landing page");
    assertThat(result.currentExecutionReference()).isEqualTo("commercial-plan:4@v3:journey");
    assertThat(result.operationalState()).isEqualTo("BLOCKED");
    assertThat(result.objectiveAchieved()).isFalse();
    assertThat(result.selectedActivityCount()).isEqualTo(5);
    assertThat(result.completedActivityCount()).isEqualTo(2);
    assertThat(result.remainingActivityCount()).isEqualTo(3);
    assertThat(result.blockedActivityCount()).isOne();
    assertThat(result.currentActivityId()).isEqualTo("customer");
    assertThat(result.currentActivityName()).isEqualTo("Avaliar percepção da cliente");
    assertThat(result.currentActivityState()).isEqualTo("BLOCKED");
    assertThat(result.currentActivityStateReason()).contains("Checkout canônico ausente");
    assertThat(result.activityCount()).isEqualTo(5);
    assertThat(result.activitiesWithTasksCount()).isEqualTo(4);
    assertThat(result.uniqueTaskCount()).isEqualTo(3);
    assertThat(result.knownEstimatedCostUsd()).isEqualByComparingTo("1.61762400");
    assertThat(result.costCoverage()).isEqualTo("COMPLETE");
    assertThat(result.activities().get(0).activityId()).isEqualTo("select");
    assertThat(result.activities().get(0).operationalState()).isEqualTo("COMPLETED");
    assertThat(result.activities().get(0).objectiveAchieved()).isTrue();
    assertThat(result.activities().get(0).stateEvidence()).isEqualTo("COMPOSITE_TASK_COVERAGE");
    assertThat(result.activities().get(0).stateReason()).contains("tarefa composta #243");
    assertThat(result.activities().get(0).tasks())
        .extracting(execution -> execution.taskId())
        .containsExactly(243L);
    assertThat(result.activities().get(1).tasks())
        .extracting(execution -> execution.taskId())
        .containsExactly(243L);
    assertThat(result.activities().get(1).activityInstanceId()).isEqualTo(128L);
    assertThat(result.activities().get(1).stateEvidence()).isEqualTo("DIRECT");
    assertThat(result.activities().get(2).tasks())
        .extracting(execution -> execution.taskId())
        .containsExactly(242L);
    assertThat(result.activities().get(2).operationalState()).isEqualTo("NOT_STARTED");
    assertThat(result.activities().get(2).stateEvidence()).isEqualTo("NOT_RECORDED");
    assertThat(result.activities().get(3).tasks())
        .extracting(execution -> execution.taskId())
        .containsExactly(244L);
    assertThat(result.activities().get(3).operationalState()).isEqualTo("BLOCKED");
    assertThat(result.activities().get(3).activityInstanceId()).isEqualTo(129L);
    assertThat(result.activities().get(4).tasks()).isEmpty();
    assertThat(result.activities().get(4).operationalState()).isEqualTo("PENDING");
    assertThat(result.activities().get(4).activityInstanceId()).isEqualTo(130L);
    assertThat(result.activities().get(4).stateEvidence()).isEqualTo("DIRECT");
    assertThat(result.activities().get(0).tasks().getFirst().productInternalName())
        .isEqualTo("Rigel");
    assertThat(result.activities()).allMatch(activity -> activity.executionControl() != null);
  }

  /** Mostra no produto as atividades da execução independente que materializou Mira. */
  @Test
  void returnsOriginExecutionActivitiesForMaterializedProduct() {
    BusinessProcessActivityDefinitionRepository activityDefinitions =
        mock(BusinessProcessActivityDefinitionRepository.class);
    AgentTaskActivityCoverageRepository coverages = mock(AgentTaskActivityCoverageRepository.class);
    BusinessProcessActivityInstanceRepository instances =
        mock(BusinessProcessActivityInstanceRepository.class);
    CommercialPlanRepository commercialPlans = mock(CommercialPlanRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    ProductOriginExecutionReferenceResolver origins =
        mock(ProductOriginExecutionReferenceResolver.class);
    var executionService =
        new BusinessProcessActivityExecutionService(
            processes,
            activityDefinitions,
            tasks,
            coverages,
            instances,
            commercialPlans,
            null,
            products,
            experiments,
            origins,
            null,
            new ObjectMapper(),
            List.of(),
            List.of(),
            List.of());
    BusinessProcessDefinition planning = selectedProcess();
    planning.setId(67L);
    planning.setProcessCode("pde-commercial-plan-offer");
    planning.setName("Estratégia, economia e protótipo privado do PDE");
    planning.setVersionNumber(6);
    planning.setStatus("PUBLISHED");
    BusinessProcessActivityDefinition architecture =
        activity(711L, planning, "productArchitecture", "Desenhar arquitetura do produto");
    Product mira = Product.builder().id(10L).internalName("Mira").build();
    mira.setAutomaticExecutionEnabled(true);
    AgentTask dedalo = executionTask(331L);
    dedalo.setProcessDefinition(planning);
    dedalo.setProcessActivityId("productArchitecture");
    dedalo.setProcessActivityName("Desenhar arquitetura do produto");
    dedalo.setSourceReference("product-discovery-cycle:64");
    BusinessProcessActivityInstance completed =
        activityInstance(
            182L, architecture, "COMPLETED", true, null, Instant.parse("2026-09-02T22:02:10Z"));
    completed.setSourceReference("product-discovery-cycle:64");
    dedalo.setActivityInstance(completed);
    when(processes.findById(67L)).thenReturn(Optional.of(planning));
    when(products.findById(10L)).thenReturn(Optional.of(mira));
    when(commercialPlans.findByProductId(10L)).thenReturn(List.of());
    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(10L)).thenReturn(List.of());
    when(origins.resolve(10L)).thenReturn(Optional.of("product-discovery-cycle:64"));
    when(activityDefinitions.findAllByProcessDefinitionIdOrderByIdAsc(67L))
        .thenReturn(List.of(architecture));
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("product-discovery-cycle:64"))
        .thenReturn(List.of(dedalo));
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                "pde-commercial-plan-offer", "product-discovery-cycle:64"))
        .thenReturn(List.of(completed));

    var result = executionService.productProcessExecutions(67L, 10L);

    assertThat(result.productInternalName()).isEqualTo("Mira");
    assertThat(result.currentExecutionReference()).isEqualTo("product-discovery-cycle:64");
    assertThat(result.operationalState()).isEqualTo("COMPLETED");
    assertThat(result.objectiveAchieved()).isTrue();
    assertThat(result.uniqueTaskCount()).isOne();
    assertThat(result.activities())
        .singleElement()
        .satisfies(
            activity -> {
              assertThat(activity.activityId()).isEqualTo("productArchitecture");
              assertThat(activity.tasks())
                  .extracting(execution -> execution.taskId())
                  .containsExactly(331L);
            });
    verify(tasks).findBySourceReferenceOrderByCreatedAtAscIdAsc("product-discovery-cycle:64");
  }

  /** Expõe o comando backend bloqueado sem quebrar a tela de produto ainda sem experimento. */
  @Test
  void explainsBackendActivityWhenProductHasNoExperiment() {
    BusinessProcessActivityDefinitionRepository activityDefinitions =
        mock(BusinessProcessActivityDefinitionRepository.class);
    AgentTaskActivityCoverageRepository coverages = mock(AgentTaskActivityCoverageRepository.class);
    BusinessProcessActivityInstanceRepository instances =
        mock(BusinessProcessActivityInstanceRepository.class);
    CommercialPlanRepository commercialPlans = mock(CommercialPlanRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    AgentTaskService agentTasks = mock(AgentTaskService.class);
    BackendProductProcessActivityExecutor backendExecutor =
        mock(BackendProductProcessActivityExecutor.class);
    var executionService =
        new BusinessProcessActivityExecutionService(
            processes,
            activityDefinitions,
            tasks,
            coverages,
            instances,
            commercialPlans,
            null,
            products,
            experiments,
            agentTasks,
            new ObjectMapper(),
            List.of(backendExecutor),
            List.of());
    BusinessProcessDefinition process = selectedProcess();
    process.setId(56L);
    process.setStatus("PUBLISHED");
    process.setProcessCode("pde-commercial-homologation-activation");
    BusinessProcessActivityDefinition preflight =
        activity(589L, process, "preflight", "Executar preflight técnico");
    preflight.setOwnerName("Backend");
    Product product = Product.builder().id(9L).internalName("Rigel").build();
    product.setAutomaticExecutionEnabled(true);
    when(processes.findById(56L)).thenReturn(Optional.of(process));
    when(products.findById(9L)).thenReturn(Optional.of(product));
    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(9L)).thenReturn(List.of());
    when(commercialPlans.findByProductId(9L)).thenReturn(List.of());
    when(activityDefinitions.findAllByProcessDefinitionIdOrderByIdAsc(56L))
        .thenReturn(List.of(preflight));
    when(backendExecutor.supports(process, preflight)).thenReturn(true);

    var result = executionService.productProcessExecutions(56L, 9L);

    assertThat(result.activities())
        .singleElement()
        .satisfies(
            activity -> {
              assertThat(activity.executionControl().executorType()).isEqualTo("BACKEND");
              assertThat(activity.executionControl().actionAvailable()).isFalse();
              assertThat(activity.executionControl().availabilityReason())
                  .contains("não possui experimento");
            });
    verify(backendExecutor, never()).readiness(any(), any(), any(), any());
  }

  /** Projeta e despacha a aprovação humana pelo mesmo contrato canônico da atividade. */
  @Test
  void exposesAndExecutesHumanApprovalContract() {
    BusinessProcessActivityDefinitionRepository activityDefinitions =
        mock(BusinessProcessActivityDefinitionRepository.class);
    AgentTaskActivityCoverageRepository coverages = mock(AgentTaskActivityCoverageRepository.class);
    BusinessProcessActivityInstanceRepository instances =
        mock(BusinessProcessActivityInstanceRepository.class);
    CommercialPlanRepository commercialPlans = mock(CommercialPlanRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    AgentTaskService agentTasks = mock(AgentTaskService.class);
    HumanProductProcessActivityExecutor humanExecutor =
        mock(HumanProductProcessActivityExecutor.class);
    var executionService =
        new BusinessProcessActivityExecutionService(
            processes,
            activityDefinitions,
            tasks,
            coverages,
            instances,
            commercialPlans,
            null,
            products,
            experiments,
            agentTasks,
            new ObjectMapper(),
            List.of(),
            List.of(humanExecutor),
            List.of());
    BusinessProcessDefinition process = selectedProcess();
    process.setId(56L);
    process.setStatus("PUBLISHED");
    process.setProcessCode("pde-commercial-homologation-activation");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"authorization\",\"type\":\"TASK\",\"label\":\"Autorizar ativação\"}]}");
    BusinessProcessActivityDefinition authorization =
        activity(590L, process, "authorization", "Autorizar ativação e orçamento");
    authorization.setOwnerName("Operador humano");
    Product product = Product.builder().id(9L).internalName("Rigel").build();
    product.setAutomaticExecutionEnabled(true);
    Experiment experiment = new Experiment();
    experiment.setId(89L);
    experiment.setProduct(product);
    ProductProcessActivityExecutionRequest request =
        new ProductProcessActivityExecutionRequest(
            "APPROVE",
            null,
            null,
            null,
            "CONFIRM:pde-commercial-homologation-activation:authorization");
    HumanProductProcessActivityReadiness readiness =
        new HumanProductProcessActivityReadiness(
            true,
            "Pronta para decisão.",
            "Autorizar ativação",
            "Registra uma decisão auditável.",
            "Confirmar ativação",
            "Confirmo a ativação dentro do teto.",
            request.confirmationToken(),
            "EXPERIMENT_ACTIVATION",
            89L,
            List.of(),
            HumanProductProcessActivityReadiness.REVIEW_AND_ACCEPT,
            "experiment:89; experiment-run:12; commercial-plan:4");
    when(processes.findById(56L)).thenReturn(Optional.of(process));
    when(products.findById(9L)).thenReturn(Optional.of(product));
    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(9L)).thenReturn(List.of(experiment));
    when(commercialPlans.findByProductId(9L)).thenReturn(List.of());
    when(activityDefinitions.findAllByProcessDefinitionIdOrderByIdAsc(56L))
        .thenReturn(List.of(authorization));
    when(activityDefinitions.findByProcessDefinitionIdAndActivityId(56L, "authorization"))
        .thenReturn(Optional.of(authorization));
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:89"))
        .thenReturn(List.of());
    when(humanExecutor.supports(process, authorization)).thenReturn(true);
    when(humanExecutor.readiness(process, authorization, product, "experiment:89"))
        .thenReturn(readiness);
    when(humanExecutor.execute(process, authorization, product, "experiment:89", request))
        .thenReturn(
            new HumanProductProcessActivityExecutionResult(
                "experiment:89", "COMPLETED", true, "Decisão registrada."));

    var history = executionService.productProcessExecutions(56L, 9L);
    var result =
        executionService.requestProductActivityExecution(56L, 9L, "authorization", request);

    assertThat(history.activities())
        .singleElement()
        .satisfies(
            activity -> {
              assertThat(activity.executionControl().executorType()).isEqualTo("HUMAN");
              assertThat(activity.executionControl().interactionType()).isEqualTo("APPROVAL");
              assertThat(activity.executionControl().actionAvailable()).isTrue();
              assertThat(activity.executionControl().decisionMode()).isEqualTo("REVIEW_AND_ACCEPT");
              assertThat(activity.executionControl().auditEvidenceReference())
                  .isEqualTo("experiment:89; experiment-run:12; commercial-plan:4");
            });
    assertThat(result.operationalState()).isEqualTo("COMPLETED");
    verify(humanExecutor).execute(process, authorization, product, "experiment:89", request);
    verifyNoInteractions(agentTasks);
  }

  /** Abre Psique e Têmis juntas na mesma ocorrência e referência do experimento do produto. */
  @Test
  void requestsEveryResponsibleAgentTaskForProductActivity() {
    BusinessProcessActivityDefinitionRepository activityDefinitions =
        mock(BusinessProcessActivityDefinitionRepository.class);
    AgentTaskActivityCoverageRepository coverages = mock(AgentTaskActivityCoverageRepository.class);
    BusinessProcessActivityInstanceRepository instances =
        mock(BusinessProcessActivityInstanceRepository.class);
    CommercialPlanRepository commercialPlans = mock(CommercialPlanRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    AgentTaskService agentTasks = mock(AgentTaskService.class);
    var executionService =
        new BusinessProcessActivityExecutionService(
            processes,
            activityDefinitions,
            tasks,
            coverages,
            instances,
            commercialPlans,
            null,
            products,
            experiments,
            agentTasks,
            new ObjectMapper());
    BusinessProcessDefinition process = selectedProcess();
    process.setId(45L);
    process.setStatus("PUBLISHED");
    process.setProcessCode("pde-commercial-homologation-activation");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"pdeGate\",\"type\":\"TASK\","
            + "\"label\":\"Validar fatos, controle e valor do PDE\","
            + "\"description\":\"Comprovar o valor do PDE.\","
            + "\"responsibleAgentKeys\":[\"customer-agent\",\"meta-ad-approver\"]}]}");
    Product vega = new Product();
    vega.setId(4L);
    vega.setName("Método MUSA 7 Dias");
    vega.setInternalName("Vega");
    vega.setAutomaticExecutionEnabled(true);
    Experiment experiment = new Experiment();
    experiment.setId(90L);
    experiment.setProduct(vega);
    when(processes.findById(45L)).thenReturn(Optional.of(process));
    when(products.findById(4L)).thenReturn(Optional.of(vega));
    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(4L)).thenReturn(List.of(experiment));
    BusinessProcessActivityDefinition pdeGate =
        activity(151L, process, "pdeGate", "Validar fatos, controle e valor do PDE");
    pdeGate.setDefinitionJson(
        "{\"responsibleAgentKeys\":[\"customer-agent\",\"meta-ad-approver\"]}");
    when(activityDefinitions.findByProcessDefinitionIdAndActivityId(45L, "pdeGate"))
        .thenReturn(Optional.of(pdeGate));
    when(agentTasks.retryBlockedByHumanOrRefreshPending(any(CreateAgentTaskRequest.class)))
        .thenReturn(mock(AgentTaskResponse.class));

    var result = executionService.requestProductActivityExecution(45L, 4L, "pdeGate");

    assertThat(result.sourceReference()).isEqualTo("experiment:90");
    assertThat(result.tasks()).hasSize(2);
    ArgumentCaptor<CreateAgentTaskRequest> requests =
        ArgumentCaptor.forClass(CreateAgentTaskRequest.class);
    verify(agentTasks, times(2)).retryBlockedByHumanOrRefreshPending(requests.capture());
    assertThat(requests.getAllValues())
        .extracting(CreateAgentTaskRequest::assignedAgentKey)
        .containsExactly("customer-agent", "meta-ad-approver");
    assertThat(requests.getAllValues())
        .allSatisfy(
            request -> {
              assertThat(request.sourceReference()).isEqualTo("experiment:90");
              assertThat(request.processDefinitionId()).isEqualTo(45L);
              assertThat(request.processActivityId()).isEqualTo("pdeGate");
              assertThat(request.title()).contains("Vega");
            });
  }

  /** Inicia a construção privada pelo próprio produto antes de existir experimento comercial. */
  @Test
  void requestsPrivateConstructionWithProductContextBeforeExperiment() {
    BusinessProcessActivityDefinitionRepository activityDefinitions =
        mock(BusinessProcessActivityDefinitionRepository.class);
    AgentTaskActivityCoverageRepository coverages = mock(AgentTaskActivityCoverageRepository.class);
    BusinessProcessActivityInstanceRepository instances =
        mock(BusinessProcessActivityInstanceRepository.class);
    CommercialPlanRepository commercialPlans = mock(CommercialPlanRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    AgentTaskService agentTasks = mock(AgentTaskService.class);
    var executionService =
        new BusinessProcessActivityExecutionService(
            processes,
            activityDefinitions,
            tasks,
            coverages,
            instances,
            commercialPlans,
            null,
            products,
            experiments,
            agentTasks,
            new ObjectMapper());
    BusinessProcessDefinition process = selectedProcess();
    process.setId(66L);
    process.setStatus("PUBLISHED");
    process.setProcessCode("pde-construction-approval");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"journey\",\"type\":\"TASK\","
            + "\"label\":\"Construir jornada de valor\","
            + "\"description\":\"Materializar o protótipo privado.\","
            + "\"responsibleAgentKeys\":[\"landing-generator\"]}]}");
    Product product = Product.builder().id(901L).internalName("PDE privado").build();
    product.setAutomaticExecutionEnabled(true);
    product.setValidationDefinitionVersion("PDE_PRIVATE_VALIDATION_V1");
    product.setPdeExperienceJson(
        "{\"contractVersion\":\"PDE_HARNESS_PLAN_V1\","
            + "\"experienceVersion\":\"private-validation-v1\"}");
    BusinessProcessActivityDefinition journey =
        activity(601L, process, "journey", "Construir jornada de valor");
    journey.setDefinitionJson("{\"responsibleAgentKeys\":[\"landing-generator\"]}");
    when(processes.findById(66L)).thenReturn(Optional.of(process));
    when(products.findById(901L)).thenReturn(Optional.of(product));
    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(901L)).thenReturn(List.of());
    when(commercialPlans.findByProductId(901L)).thenReturn(List.of());
    when(activityDefinitions.findAllByProcessDefinitionIdOrderByIdAsc(66L))
        .thenReturn(List.of(journey));
    when(activityDefinitions.findByProcessDefinitionIdAndActivityId(66L, "journey"))
        .thenReturn(Optional.of(journey));
    when(agentTasks.retryBlockedByHumanOrRefreshPending(any(CreateAgentTaskRequest.class)))
        .thenReturn(mock(AgentTaskResponse.class));

    var history = executionService.productProcessExecutions(66L, 901L);
    var result = executionService.requestProductActivityExecution(66L, 901L, "journey");

    assertThat(history.activities())
        .singleElement()
        .satisfies(
            activity -> {
              assertThat(activity.executionRequestAvailable()).isTrue();
              assertThat(activity.executionRequestReason()).contains("pronta");
            });
    assertThat(result.sourceReference()).isEqualTo("product:901@private-validation-v1");
    ArgumentCaptor<CreateAgentTaskRequest> request =
        ArgumentCaptor.forClass(CreateAgentTaskRequest.class);
    verify(agentTasks).retryBlockedByHumanOrRefreshPending(request.capture());
    assertThat(request.getValue().assignedAgentKey()).isEqualTo("landing-generator");
    assertThat(request.getValue().sourceReference()).isEqualTo("product:901@private-validation-v1");
  }

  /** Executa uma atividade determinística no backend e preserva a referência do ciclo atual. */
  @Test
  void requestsBackendOwnedProductActivityWithoutCreatingAgentTask() {
    BusinessProcessActivityDefinitionRepository activityDefinitions =
        mock(BusinessProcessActivityDefinitionRepository.class);
    AgentTaskActivityCoverageRepository coverages = mock(AgentTaskActivityCoverageRepository.class);
    BusinessProcessActivityInstanceRepository instances =
        mock(BusinessProcessActivityInstanceRepository.class);
    CommercialPlanRepository commercialPlans = mock(CommercialPlanRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    AgentTaskService agentTasks = mock(AgentTaskService.class);
    BackendProductProcessActivityExecutor backendExecutor =
        mock(BackendProductProcessActivityExecutor.class);
    var executionService =
        new BusinessProcessActivityExecutionService(
            processes,
            activityDefinitions,
            tasks,
            coverages,
            instances,
            commercialPlans,
            null,
            products,
            experiments,
            agentTasks,
            new ObjectMapper(),
            List.of(backendExecutor),
            List.of());
    BusinessProcessDefinition process = selectedProcess();
    process.setId(55L);
    process.setStatus("PUBLISHED");
    process.setProcessCode("pde-communication-sales-journey");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"integration\",\"type\":\"TASK\","
            + "\"label\":\"Integrar canal, checkout, acesso e eventos\","
            + "\"description\":\"Preparar a jornada.\"}]}");
    BusinessProcessActivityDefinition integration =
        activity(175L, process, "integration", "Integrar canal, checkout, acesso e eventos");
    integration.setDefinitionJson("{\"responsibleAgentKeys\":[]}");
    Product rigel = Product.builder().id(9L).internalName("Rigel").build();
    Experiment experiment = new Experiment();
    experiment.setId(89L);
    experiment.setProduct(rigel);
    AgentTask previous = executionTask(248L);
    previous.setProcessDefinition(process);
    previous.setSourceReference("commercial-plan:4@v3:journey");
    when(processes.findById(55L)).thenReturn(Optional.of(process));
    when(products.findById(9L)).thenReturn(Optional.of(rigel));
    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(9L)).thenReturn(List.of(experiment));
    when(activityDefinitions.findByProcessDefinitionIdAndActivityId(55L, "integration"))
        .thenReturn(Optional.of(integration));
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:89"))
        .thenReturn(List.of());
    CommercialPlan plan = new CommercialPlan();
    plan.setId(4L);
    when(commercialPlans.findByProductId(9L)).thenReturn(List.of(plan));
    when(tasks.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc("commercial-plan:4@"))
        .thenReturn(List.of(previous));
    when(backendExecutor.supports(process, integration)).thenReturn(true);
    when(backendExecutor.readiness(process, integration, rigel, "commercial-plan:4@v3:journey"))
        .thenReturn(new BackendProductProcessActivityReadiness(true, "Pronta."));
    when(backendExecutor.execute(process, integration, rigel, "commercial-plan:4@v3:journey"))
        .thenReturn(
            new BackendProductProcessActivityExecutionResult(
                "commercial-plan:4@v3:journey", "COMPLETED", true, "Integração concluída."));

    var result = executionService.requestProductActivityExecution(55L, 9L, "integration");

    assertThat(result.sourceReference()).isEqualTo("commercial-plan:4@v3:journey");
    assertThat(result.operationalState()).isEqualTo("COMPLETED");
    assertThat(result.objectiveAchieved()).isTrue();
    assertThat(result.message()).isEqualTo("Integração concluída.");
    assertThat(result.tasks()).isEmpty();
    verifyNoInteractions(agentTasks);
  }

  /**
   * Mantém Íris bloqueada na tela até Plutus concluir e permite nova tentativa sem apagar a falha.
   */
  @Test
  void alignsIrisButtonWithCrossProcessReadinessAndAllowsBlockedRetry() {
    BusinessProcessActivityDefinitionRepository activityDefinitions =
        mock(BusinessProcessActivityDefinitionRepository.class);
    AgentTaskActivityCoverageRepository coverages = mock(AgentTaskActivityCoverageRepository.class);
    BusinessProcessActivityInstanceRepository instances =
        mock(BusinessProcessActivityInstanceRepository.class);
    CommercialPlanRepository commercialPlans = mock(CommercialPlanRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    AgentTaskService agentTasks = mock(AgentTaskService.class);
    AgentProductProcessActivityReadinessProvider readinessProvider =
        mock(AgentProductProcessActivityReadinessProvider.class);
    var executionService =
        new BusinessProcessActivityExecutionService(
            processes,
            activityDefinitions,
            tasks,
            coverages,
            instances,
            commercialPlans,
            null,
            products,
            experiments,
            agentTasks,
            new ObjectMapper(),
            List.of(),
            List.of(readinessProvider));
    BusinessProcessDefinition process = selectedProcess();
    process.setId(63L);
    process.setStatus("PUBLISHED");
    process.setProcessCode("pde-communication-sales-journey");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"communicationContract\",\"type\":\"TASK\","
            + "\"label\":\"Materializar contrato de comunicação\","
            + "\"description\":\"Transformar estratégia e produto em comunicação.\","
            + "\"responsibleAgentKeys\":[\"communication-director\"]}]}");
    BusinessProcessActivityDefinition communicationContract =
        activity(201L, process, "communicationContract", "Materializar contrato de comunicação");
    communicationContract.setDefinitionJson(
        "{\"responsibleAgentKeys\":[\"communication-director\"]}");
    Product rigel = Product.builder().id(9L).internalName("Rigel").build();
    rigel.setAutomaticExecutionEnabled(true);
    Experiment experiment = new Experiment();
    experiment.setId(89L);
    experiment.setProduct(rigel);
    CommercialPlan plan = CommercialPlan.builder().id(4L).name("Agenda Cheia").build();
    AgentTask blockedTask = executionTask(252L);
    blockedTask.setProcessDefinition(process);
    blockedTask.setProcessActivityId("communicationContract");
    blockedTask.setProcessActivityName("Materializar contrato de comunicação");
    blockedTask.setSourceReference("experiment:89");
    blockedTask.setStatus("BLOCKED");
    blockedTask.setAssignedAgent(
        Agent.builder().agentKey("communication-director").nickname("Íris").build());
    BusinessProcessActivityInstance blockedInstance =
        activityInstance(
            138L,
            communicationContract,
            "BLOCKED",
            false,
            "Parecer econômico ausente.",
            blockedTask.getUpdatedAt());
    blockedInstance.setSourceReference("experiment:89");
    blockedTask.setActivityInstance(blockedInstance);
    when(processes.findById(63L)).thenReturn(Optional.of(process));
    when(products.findById(9L)).thenReturn(Optional.of(rigel));
    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(9L)).thenReturn(List.of(experiment));
    when(commercialPlans.findByProductId(9L)).thenReturn(List.of(plan));
    when(activityDefinitions.findAllByProcessDefinitionIdOrderByIdAsc(63L))
        .thenReturn(List.of(communicationContract));
    when(activityDefinitions.findByProcessDefinitionIdAndActivityId(63L, "communicationContract"))
        .thenReturn(Optional.of(communicationContract));
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:89"))
        .thenReturn(List.of(blockedTask));
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                "pde-communication-sales-journey", "experiment:89"))
        .thenReturn(List.of(blockedInstance));
    when(readinessProvider.supports(any(), any())).thenReturn(true);
    when(readinessProvider.readiness(any(), any(), any(), eq("experiment:89")))
        .thenReturn(
            new AgentProductProcessActivityReadiness(
                false, "Antes de executar Íris, conclua Plutus."));

    var blocked = executionService.productProcessExecutions(63L, 9L);

    verify(readinessProvider).supports(any(), any());
    verify(readinessProvider).readiness(any(), any(), any(), eq("experiment:89"));
    assertThat(blocked.activities().getFirst().executionRequestAvailable()).isFalse();
    assertThat(blocked.activities().getFirst().executionRequestReason()).contains("Plutus");
    assertThatThrownBy(
            () ->
                executionService.requestProductActivityExecution(63L, 9L, "communicationContract"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Plutus");
    verifyNoInteractions(agentTasks);

    when(readinessProvider.readiness(any(), any(), any(), eq("experiment:89")))
        .thenReturn(
            new AgentProductProcessActivityReadiness(
                true, "Estratégia, economia, PDE e provas estão prontos para Íris."));
    when(agentTasks.retryBlockedByHumanOrRefreshPending(any(CreateAgentTaskRequest.class)))
        .thenReturn(mock(AgentTaskResponse.class));

    var ready = executionService.productProcessExecutions(63L, 9L);
    var request =
        executionService.requestProductActivityExecution(63L, 9L, "communicationContract");

    assertThat(ready.activities().getFirst().executionRequestAvailable()).isTrue();
    assertThat(request.tasks()).hasSize(1);
    verify(agentTasks).retryBlockedByHumanOrRefreshPending(any(CreateAgentTaskRequest.class));
  }

  /**
   * Libera reinício de tarefa bloqueada sem exigir um gate especializado e preserva a tentativa.
   */
  @Test
  void exposesGenericBlockedTaskRestartAndUsesAuditableRetry() {
    BusinessProcessActivityDefinitionRepository activityDefinitions =
        mock(BusinessProcessActivityDefinitionRepository.class);
    AgentTaskActivityCoverageRepository coverages = mock(AgentTaskActivityCoverageRepository.class);
    BusinessProcessActivityInstanceRepository instances =
        mock(BusinessProcessActivityInstanceRepository.class);
    CommercialPlanRepository commercialPlans = mock(CommercialPlanRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    AgentTaskService agentTasks = mock(AgentTaskService.class);
    var executionService =
        new BusinessProcessActivityExecutionService(
            processes,
            activityDefinitions,
            tasks,
            coverages,
            instances,
            commercialPlans,
            null,
            products,
            experiments,
            agentTasks,
            new ObjectMapper());
    BusinessProcessDefinition process = selectedProcess();
    process.setId(56L);
    process.setStatus("PUBLISHED");
    process.setProcessCode("pde-commercial-homologation-activation");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"humanExperienceReview\",\"type\":\"TASK\","
            + "\"label\":\"Validar experiência humana da jornada\","
            + "\"description\":\"Validar clareza, desejo e confiança.\","
            + "\"responsibleAgentKeys\":[\"customer-agent\"]}]}");
    BusinessProcessActivityDefinition activity =
        activity(587L, process, "humanExperienceReview", "Validar experiência humana da jornada");
    activity.setDefinitionJson("{\"responsibleAgentKeys\":[\"customer-agent\"]}");
    Product rigel = Product.builder().id(9L).internalName("Rigel").build();
    rigel.setAutomaticExecutionEnabled(true);
    Experiment experiment = new Experiment();
    experiment.setId(89L);
    experiment.setProduct(rigel);
    AgentTask blockedTask = executionTask(254L);
    blockedTask.setProcessDefinition(process);
    blockedTask.setProcessActivityId("humanExperienceReview");
    blockedTask.setProcessActivityName("Validar experiência humana da jornada");
    blockedTask.setSourceReference("experiment:89");
    blockedTask.setStatus("BLOCKED");
    blockedTask.setAssignedAgent(
        Agent.builder().agentKey("customer-agent").nickname("Psique").build());
    BusinessProcessActivityInstance blockedInstance =
        activityInstance(
            139L,
            activity,
            "BLOCKED",
            false,
            "SHA-256 divergente para a prova comercial.",
            blockedTask.getUpdatedAt());
    blockedInstance.setSourceReference("experiment:89");
    blockedTask.setActivityInstance(blockedInstance);
    when(processes.findById(56L)).thenReturn(Optional.of(process));
    when(products.findById(9L)).thenReturn(Optional.of(rigel));
    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(9L)).thenReturn(List.of(experiment));
    when(commercialPlans.findByProductId(9L)).thenReturn(List.of());
    when(activityDefinitions.findAllByProcessDefinitionIdOrderByIdAsc(56L))
        .thenReturn(List.of(activity));
    when(activityDefinitions.findByProcessDefinitionIdAndActivityId(56L, "humanExperienceReview"))
        .thenReturn(Optional.of(activity));
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:89"))
        .thenReturn(List.of(blockedTask));
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                "pde-commercial-homologation-activation", "experiment:89"))
        .thenReturn(List.of(blockedInstance));
    when(agentTasks.retryBlockedByHumanOrRefreshPending(any(CreateAgentTaskRequest.class)))
        .thenReturn(mock(AgentTaskResponse.class));

    var history = executionService.productProcessExecutions(56L, 9L);
    var request =
        executionService.requestProductActivityExecution(56L, 9L, "humanExperienceReview");

    assertThat(history.activities().getFirst().executionRequestAvailable()).isTrue();
    assertThat(history.activities().getFirst().executionRequestReason())
        .contains("tentativa bloqueada será preservada");
    assertThat(request.tasks()).hasSize(1);
    ArgumentCaptor<CreateAgentTaskRequest> retryRequest =
        ArgumentCaptor.forClass(CreateAgentTaskRequest.class);
    verify(agentTasks).retryBlockedByHumanOrRefreshPending(retryRequest.capture());
    assertThat(retryRequest.getValue().sourceReference()).isEqualTo("experiment:89");
    assertThat(retryRequest.getValue().processActivityId()).isEqualTo("humanExperienceReview");

    blockedTask.setStatus("PENDING");
    blockedInstance.setStatus("PENDING");
    assertThatThrownBy(
            () ->
                executionService.requestProductActivityExecution(56L, 9L, "humanExperienceReview"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("execução ativa");

    blockedTask.setStatus("COMPLETED");
    blockedInstance.setStatus("COMPLETED");
    blockedInstance.setObjectiveAchieved(true);
    assertThatThrownBy(
            () ->
                executionService.requestProductActivityExecution(56L, 9L, "humanExperienceReview"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("objetivo da atividade já foi atingido");
    verifyNoMoreInteractions(agentTasks);
  }

  /** Impede novas tarefas quando o produto está administrativamente em STOP. */
  @Test
  void rejectsActivityRequestForStoppedProduct() {
    ProductRepository products = mock(ProductRepository.class);
    ExperimentRepository experiments = mock(ExperimentRepository.class);
    AgentTaskService agentTasks = mock(AgentTaskService.class);
    var executionService =
        new BusinessProcessActivityExecutionService(
            processes,
            mock(BusinessProcessActivityDefinitionRepository.class),
            tasks,
            mock(AgentTaskActivityCoverageRepository.class),
            mock(BusinessProcessActivityInstanceRepository.class),
            mock(CommercialPlanRepository.class),
            null,
            products,
            experiments,
            agentTasks,
            new ObjectMapper());
    BusinessProcessDefinition process = selectedProcess();
    process.setStatus("PUBLISHED");
    Product stopped = new Product();
    stopped.setId(4L);
    stopped.setAutomaticExecutionEnabled(false);
    when(processes.findById(37L)).thenReturn(Optional.of(process));
    when(products.findById(4L)).thenReturn(Optional.of(stopped));

    assertThatThrownBy(() -> executionService.requestProductActivityExecution(37L, 4L, "evidence"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("produto está em STOP");
    verifyNoInteractions(agentTasks);
  }

  /** Bloqueia o uso de um gate como se ele possuísse tarefas executáveis. */
  @Test
  void rejectsNonTaskActivity() {
    when(processes.findById(37L)).thenReturn(Optional.of(selectedProcess()));

    assertThatThrownBy(() -> service.recentExecutions(37L, "gate"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Atividade não encontrada");
    verify(tasks, never()).findRecentActivityExecutions(anyString(), anyString(), any());
  }

  /** Monta a versão selecionada pelo usuário com atividade e gate distintos. */
  private BusinessProcessDefinition selectedProcess() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(37L);
    process.setProcessCode("pde-opportunity-discovery");
    process.setName("Descoberta e priorização da oportunidade PDE");
    process.setVersionNumber(4);
    process.setStatus("RETIRED");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"evidence\",\"type\":\"TASK\","
            + "\"label\":\"Comprovar dor e demanda\",\"owner\":\"Argos\"},"
            + "{\"id\":\"gate\",\"type\":\"GATEWAY\",\"label\":\"Aprovada?\"}]}");
    return process;
  }

  /** Monta uma atividade relacional da versão selecionada para o agrupamento do produto. */
  private BusinessProcessActivityDefinition activity(
      long id, BusinessProcessDefinition process, String activityId, String name) {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setId(id);
    activity.setProcessDefinition(process);
    activity.setActivityId(activityId);
    activity.setName(name);
    activity.setObjective("Objetivo de " + name);
    activity.setOwnerName("Responsável");
    return activity;
  }

  /** Monta uma tarefa da v1 para comprovar a consulta histórica iniciada na v4. */
  private AgentTask executionTask(long id) {
    BusinessProcessDefinition originalProcess = new BusinessProcessDefinition();
    originalProcess.setId(22L);
    originalProcess.setVersionNumber(1);
    Agent agent = new Agent();
    agent.setAgentKey("market-radar");
    agent.setNickname("Argos");
    AgentTask task = new AgentTask();
    task.setId(id);
    task.setProcessDefinition(originalProcess);
    task.setAssignedAgent(agent);
    task.setTitle("Comprovar dor e demanda · rodada " + id);
    task.setStatus("COMPLETED");
    task.setSourceReference("pde-opportunity:round-" + id);
    task.setResultJson("{\"decision\":\"APPROVE\"}");
    task.setEvidenceJson("{\"sources\":2}");
    task.setInputTokens(2834L);
    task.setCachedInputTokens(2304L);
    task.setOutputTokens(5861L);
    task.setEstimatedCostUsd(new BigDecimal("0.01347240"));
    task.setCostEstimationStatus("ESTIMATED");
    task.setCreatedAt(Instant.parse("2026-08-20T21:40:00Z"));
    task.setReceivedAt(Instant.parse("2026-08-20T21:40:24Z"));
    task.setDeliveredAt(Instant.parse("2026-08-20T21:41:24Z"));
    task.setUpdatedAt(Instant.parse("2026-08-20T21:41:24Z"));
    task.setExecutionModelCode("gpt-5.4-mini-2026-03-17");
    task.setExecutionReasoningEffort("high");
    task.setExecutionPrompt("Comprove a dor com fontes independentes.");
    return task;
  }

  /** Monta a ocorrência canônica usada para comprovar autoridade e causa persistida do estado. */
  private BusinessProcessActivityInstance activityInstance(
      long id,
      BusinessProcessActivityDefinition activity,
      String status,
      boolean objectiveAchieved,
      String blockedReason,
      Instant updatedAt) {
    BusinessProcessActivityInstance instance = new BusinessProcessActivityInstance();
    instance.setId(id);
    instance.setActivityDefinition(activity);
    instance.setSourceReference("commercial-plan:4@v3:journey");
    instance.setOccurrenceNumber(1);
    instance.setStatus(status);
    instance.setEnteredAt(Instant.parse("2026-08-27T03:26:19Z"));
    instance.setObjectiveAchieved(objectiveAchieved);
    instance.setBlockedReason(blockedReason);
    instance.setCostCoverage("COMPLETE");
    instance.setEvidenceQuality("DIRECT");
    instance.setCreatedAt(Instant.parse("2026-08-27T03:26:19Z"));
    instance.setUpdatedAt(updatedAt);
    return instance;
  }
}
