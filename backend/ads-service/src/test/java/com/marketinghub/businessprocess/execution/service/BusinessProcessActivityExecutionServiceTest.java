package com.marketinghub.businessprocess.execution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskActivityCoverage;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskActivityCoverageRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
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
    CommercialPlanRepository commercialPlans = mock(CommercialPlanRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    var productService =
        new BusinessProcessActivityExecutionService(
            processes,
            activityDefinitions,
            tasks,
            coverages,
            commercialPlans,
            null,
            products,
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
    when(commercialPlans.findByProductId(9L)).thenReturn(List.of(plan));

    BusinessProcessActivityDefinition select =
        activity(119L, landing, "select", "Selecionar provas");
    BusinessProcessActivityDefinition html = activity(122L, landing, "html", "Construir HTML");
    BusinessProcessActivityDefinition customer =
        activity(124L, landing, "customer", "Avaliar percepção da cliente");
    BusinessProcessActivityDefinition human =
        activity(126L, landing, "human", "Aprovação humana para publicar");
    when(activityDefinitions.findAllByProcessDefinitionIdOrderByIdAsc(18L))
        .thenReturn(List.of(select, html, customer, human));

    AgentTask compound = executionTask(243L);
    compound.setProcessDefinition(landing);
    compound.setProcessActivityId("html");
    compound.setProcessActivityName("Construir HTML");
    compound.setSourceReference("commercial-plan:4@v3:journey");
    compound.setEstimatedCostUsd(new BigDecimal("1.42804720"));
    AgentTask customerReview = executionTask(244L);
    customerReview.setProcessDefinition(landing);
    customerReview.setProcessActivityId("customer");
    customerReview.setProcessActivityName("Avaliar percepção da cliente");
    customerReview.setSourceReference("commercial-plan:4@v3:journey");
    customerReview.setEstimatedCostUsd(new BigDecimal("0.18957680"));
    AgentTask anotherProcessTask = executionTask(245L);
    anotherProcessTask.getProcessDefinition().setProcessCode("another-process");
    anotherProcessTask.setSourceReference("commercial-plan:4@v3");
    when(tasks.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc("commercial-plan:4@"))
        .thenReturn(List.of(compound, customerReview, anotherProcessTask));
    AgentTaskActivityCoverage compoundCoverage = new AgentTaskActivityCoverage();
    compoundCoverage.setAgentTask(compound);
    compoundCoverage.setActivityDefinition(select);
    when(coverages.findAllByAgentTaskIdIn(List.of(244L, 243L)))
        .thenReturn(List.of(compoundCoverage));

    var result = productService.productProcessExecutions(18L, 9L);

    assertThat(result.productInternalName()).isEqualTo("Rigel");
    assertThat(result.processName()).isEqualTo("Geração de landing page");
    assertThat(result.activityCount()).isEqualTo(4);
    assertThat(result.activitiesWithTasksCount()).isEqualTo(3);
    assertThat(result.uniqueTaskCount()).isEqualTo(2);
    assertThat(result.knownEstimatedCostUsd()).isEqualByComparingTo("1.61762400");
    assertThat(result.costCoverage()).isEqualTo("COMPLETE");
    assertThat(result.activities().get(0).activityId()).isEqualTo("select");
    assertThat(result.activities().get(0).tasks())
        .extracting(execution -> execution.taskId())
        .containsExactly(243L);
    assertThat(result.activities().get(1).tasks())
        .extracting(execution -> execution.taskId())
        .containsExactly(243L);
    assertThat(result.activities().get(2).tasks())
        .extracting(execution -> execution.taskId())
        .containsExactly(244L);
    assertThat(result.activities().get(3).tasks()).isEmpty();
    assertThat(result.activities().get(0).tasks().getFirst().productInternalName())
        .isEqualTo("Rigel");
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
}
