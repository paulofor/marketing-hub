package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskActivityCoverage;
import com.marketinghub.agenttask.AgentTaskResponse;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CreateAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.repository.jpa.agenttask.AgentTaskActivityCoverageRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: proteger o enfileiramento auditável da homologação do plano comercial. */
@ExtendWith(MockitoExtension.class)
class CommercialPlanJourneyHomologationServiceTest {
  @Mock private CommercialPlanService commercialPlanService;
  @Mock private CommercialPlanVersionService versionService;
  @Mock private BusinessProcessDefinitionRepository processRepository;
  @Mock private BusinessProcessActivityDefinitionRepository activityDefinitionRepository;
  @Mock private AgentTaskRepository taskRepository;
  @Mock private AgentTaskActivityCoverageRepository activityCoverageRepository;
  @Mock private AgentTaskService agentTaskService;

  /** Confirma que o experimento escolhido recebe uma homologação segregada e sem gasto. */
  @Test
  void requestsIsolatedJourneyHomologationForLinkedExperiment() {
    when(commercialPlanService.getPlan(2L))
        .thenReturn(
            CommercialPlan.builder()
                .id(2L)
                .successCriteria("Landing com quatro exemplos finais e três criativos aprovados")
                .stopCriteria("Parar antes de gasto ou publicação externa")
                .currentBlocker("Prova visual incompleta")
                .rootCause("Contrato sem critério observável")
                .nextAction("Dédalo itera na sandbox e Têmis revisa")
                .build());
    when(versionService.current(2L))
        .thenReturn(
            new CommercialPlanVersionDto(
                9L, 2L, 3, "{}", "teste", "versão de teste", Instant.now()));
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(18L);
    process.setProcessCode("landing-page-generation");
    when(processRepository.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            "landing-page-generation", "PUBLISHED"))
        .thenReturn(Optional.of(process));
    stubCompoundCoverage(process);
    var service =
        new CommercialPlanJourneyHomologationService(
            commercialPlanService,
            versionService,
            processRepository,
            activityDefinitionRepository,
            taskRepository,
            activityCoverageRepository,
            agentTaskService,
            new ObjectMapper());

    var result = service.request(2L, 88L);

    assertThat(result.planId()).isEqualTo(2L);
    assertThat(result.experimentId()).isEqualTo(88L);
    assertThat(result.status()).isEqualTo("INICIADO");
    verify(commercialPlanService).requireExperiment(2L, 88L);
    ArgumentCaptor<CreateAgentTaskRequest> tasks =
        ArgumentCaptor.forClass(CreateAgentTaskRequest.class);
    verify(agentTaskService, times(3)).createByHumanIfAbsent(tasks.capture());
    assertThat(tasks.getAllValues())
        .extracting(CreateAgentTaskRequest::assignedAgentKey)
        .containsExactly("landing-generator", "customer-agent", "meta-ad-approver");
    assertThat(tasks.getAllValues())
        .extracting(CreateAgentTaskRequest::processActivityId)
        .containsExactly("html", "customer", "commercial");
    assertThat(tasks.getAllValues())
        .allSatisfy(
            task -> {
              assertThat(task.sourceReference()).isEqualTo("commercial-plan:2@v3:journey");
              assertThat(task.processDefinitionId()).isEqualTo(18L);
              assertThat(task.title()).contains("Experimento #88");
            });
    assertThat(tasks.getAllValues().getFirst().description())
        .contains("\"mediaSpendAuthorized\":false")
        .contains("BPM_TASK_RETRY_WITH_PERSISTED_CAUSE")
        .contains("quatro exemplos finais")
        .contains("Dédalo itera na sandbox")
        .contains("checkout canônico preservado")
        .contains("mesmo pacote criativo aprovado")
        .contains("Psique e Têmis")
        .contains("pertencem ao subprocesso seguinte")
        .doesNotContain("pagamento de teste");
    ArgumentCaptor<AgentTaskActivityCoverage> coverage =
        ArgumentCaptor.forClass(AgentTaskActivityCoverage.class);
    verify(activityCoverageRepository, times(3)).save(coverage.capture());
    assertThat(coverage.getAllValues())
        .extracting(item -> item.getActivityDefinition().getActivityId())
        .containsExactly("select", "strategy", "compose");
    assertThat(coverage.getAllValues())
        .allSatisfy(item -> assertThat(item.getCoverageSource()).isEqualTo("COMPOUND_EXECUTION"));
  }

  /** Abre uma execução completa nova quando a tentativa anterior terminou bloqueada. */
  @Test
  void requestsNewJourneyAttemptAfterFunctionalBlock() {
    when(commercialPlanService.getPlan(2L)).thenReturn(CommercialPlan.builder().id(2L).build());
    when(versionService.current(2L))
        .thenReturn(
            new CommercialPlanVersionDto(
                9L, 2L, 3, "{}", "teste", "versão de teste", Instant.now()));
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(18L);
    process.setProcessCode("landing-page-generation");
    when(processRepository.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            "landing-page-generation", "PUBLISHED"))
        .thenReturn(Optional.of(process));
    AgentTask blocked = org.mockito.Mockito.mock(AgentTask.class);
    when(blocked.getSourceReference()).thenReturn("commercial-plan:2@v3:journey");
    when(blocked.getProcessDefinition()).thenReturn(process);
    when(blocked.getStatus()).thenReturn("BLOCKED");
    when(blocked.getProcessActivityId()).thenReturn("customer");
    when(blocked.getExecutionError()).thenReturn("Psique encontrou promessa ambígua de aplicação.");
    when(blocked.getResultJson())
        .thenReturn(
            "{\"decision\":\"ADJUST\",\"requiredChanges\":[\"Explicitar uso manual pela cliente\"]}");
    when(taskRepository.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
            "commercial-plan:2@v3:journey"))
        .thenReturn(java.util.List.of(blocked));
    stubCompoundCoverage(process);
    var service =
        new CommercialPlanJourneyHomologationService(
            commercialPlanService,
            versionService,
            processRepository,
            activityDefinitionRepository,
            taskRepository,
            activityCoverageRepository,
            agentTaskService,
            new ObjectMapper());

    service.request(2L, 88L);

    ArgumentCaptor<CreateAgentTaskRequest> tasks =
        ArgumentCaptor.forClass(CreateAgentTaskRequest.class);
    verify(agentTaskService, times(3)).createByHumanIfAbsent(tasks.capture());
    assertThat(tasks.getAllValues())
        .extracting(CreateAgentTaskRequest::sourceReference)
        .containsOnly("commercial-plan:2@v3:journey:attempt:2");
    assertThat(tasks.getAllValues().getFirst().description())
        .contains(
            "\"journeyAttempt\":2",
            "Psique encontrou promessa ambígua de aplicação",
            "Explicitar uso manual pela cliente");
  }

  /** Reutiliza a tentativa ainda ativa e impede a abertura acidental de outra execução. */
  @Test
  void keepsTheLatestJourneyAttemptWhileItIsActive() {
    when(commercialPlanService.getPlan(2L)).thenReturn(CommercialPlan.builder().id(2L).build());
    when(versionService.current(2L))
        .thenReturn(
            new CommercialPlanVersionDto(
                9L, 2L, 3, "{}", "teste", "versão de teste", Instant.now()));
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(18L);
    process.setProcessCode("landing-page-generation");
    when(processRepository.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            "landing-page-generation", "PUBLISHED"))
        .thenReturn(Optional.of(process));
    AgentTask active = org.mockito.Mockito.mock(AgentTask.class);
    when(active.getSourceReference()).thenReturn("commercial-plan:2@v3:journey:attempt:2");
    when(active.getProcessDefinition()).thenReturn(process);
    when(active.getStatus()).thenReturn("PENDING");
    when(taskRepository.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
            "commercial-plan:2@v3:journey"))
        .thenReturn(java.util.List.of(active));
    stubCompoundCoverage(process);
    var service =
        new CommercialPlanJourneyHomologationService(
            commercialPlanService,
            versionService,
            processRepository,
            activityDefinitionRepository,
            taskRepository,
            activityCoverageRepository,
            agentTaskService,
            new ObjectMapper());

    service.request(2L, 88L);

    ArgumentCaptor<CreateAgentTaskRequest> tasks =
        ArgumentCaptor.forClass(CreateAgentTaskRequest.class);
    verify(agentTaskService, times(3)).createByHumanIfAbsent(tasks.capture());
    assertThat(tasks.getAllValues())
        .extracting(CreateAgentTaskRequest::sourceReference)
        .containsOnly("commercial-plan:2@v3:journey:attempt:2");
    assertThat(tasks.getAllValues().getFirst().description()).contains("\"journeyAttempt\":2");
  }

  /** Prepara a tarefa composta e as identidades relacionais cobertas pelo Dédalo. */
  private void stubCompoundCoverage(BusinessProcessDefinition process) {
    AgentTaskResponse response = org.mockito.Mockito.mock(AgentTaskResponse.class);
    AgentTask persistedTask = org.mockito.Mockito.mock(AgentTask.class);
    when(response.id()).thenReturn(243L);
    when(agentTaskService.createByHumanIfAbsent(any())).thenReturn(response);
    when(taskRepository.findById(243L)).thenReturn(Optional.of(persistedTask));
    when(activityDefinitionRepository.findByProcessDefinitionIdAndActivityId(
            org.mockito.ArgumentMatchers.eq(process.getId()), any()))
        .thenAnswer(
            invocation -> {
              BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
              String activityId = invocation.getArgument(1);
              activity.setId((long) activityId.hashCode() & 0x7fffffffL);
              activity.setActivityId(activityId);
              return Optional.of(activity);
            });
  }
}
