package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CreateAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.time.Instant;
import java.util.List;
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
  @Mock private AgentTaskRepository taskRepository;
  @Mock private CommercialPlanLandingReviewResumeService reviewResumeService;
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
                .nextAction("Íris materializa na sandbox e Têmis revisa")
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
    var service =
        new CommercialPlanJourneyHomologationService(
            commercialPlanService,
            versionService,
            processRepository,
            taskRepository,
            reviewResumeService,
            agentTaskService,
            new ObjectMapper());

    var result = service.request(2L, 88L);

    assertThat(result.planId()).isEqualTo(2L);
    assertThat(result.experimentId()).isEqualTo(88L);
    assertThat(result.status()).isEqualTo("INICIADO");
    verify(commercialPlanService).requireExperiment(2L, 88L);
    ArgumentCaptor<CreateAgentTaskRequest> tasks =
        ArgumentCaptor.forClass(CreateAgentTaskRequest.class);
    verify(agentTaskService, times(6)).createByHumanIfAbsent(tasks.capture());
    assertThat(tasks.getAllValues())
        .extracting(CreateAgentTaskRequest::assignedAgentKey)
        .containsExactly(
            "communication-director",
            "communication-director",
            "communication-director",
            "communication-director",
            "customer-agent",
            "meta-ad-approver");
    assertThat(tasks.getAllValues())
        .extracting(CreateAgentTaskRequest::processActivityId)
        .containsExactly("select", "strategy", "compose", "html", "customer", "commercial");
    assertThat(tasks.getAllValues())
        .allSatisfy(
            task -> {
              assertThat(task.sourceReference())
                  .isEqualTo("commercial-plan:2@v3:journey:experiment-88");
              assertThat(task.processDefinitionId()).isEqualTo(18L);
              assertThat(task.title()).contains("Experimento #88");
            });
    assertThat(tasks.getAllValues().getFirst().description())
        .contains("\"mediaSpendAuthorized\":false")
        .contains("BPM_TASK_RETRY_WITH_PERSISTED_CAUSE")
        .contains("quatro exemplos finais")
        .contains("Íris materializa na sandbox")
        .contains("checkout canônico preservado")
        .contains("mesmo pacote criativo aprovado")
        .contains("Psique e Têmis")
        .contains("pertencem ao subprocesso seguinte")
        .doesNotContain("pagamento de teste");
    assertThat(tasks.getAllValues().get(1).description())
        .contains("sem redefinir posicionamento, oferta, preço ou produto");
    assertThat(tasks.getAllValues().get(2).description()).contains("provas reais selecionadas");
    assertThat(tasks.getAllValues().get(3).description()).contains("Quality Review independente");
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
    when(blocked.getSourceReference()).thenReturn("commercial-plan:2@v3:journey:experiment-88");
    when(blocked.getProcessDefinition()).thenReturn(process);
    when(blocked.getAssignedAgent())
        .thenReturn(Agent.builder().id(2L).agentKey("customer-agent").nickname("Psique").build());
    when(blocked.getStatus()).thenReturn("BLOCKED");
    when(blocked.getProcessActivityId()).thenReturn("customer");
    when(blocked.getExecutionError()).thenReturn("Psique encontrou promessa ambígua de aplicação.");
    when(blocked.getResultJson())
        .thenReturn(
            "{\"decision\":\"ADJUST\",\"requiredChanges\":[\"Explicitar uso manual pela cliente\"]}");
    when(taskRepository.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
            "commercial-plan:2@v3:journey:experiment-88"))
        .thenReturn(java.util.List.of(blocked));
    var service =
        new CommercialPlanJourneyHomologationService(
            commercialPlanService,
            versionService,
            processRepository,
            taskRepository,
            reviewResumeService,
            agentTaskService,
            new ObjectMapper());

    service.request(2L, 88L);

    ArgumentCaptor<CreateAgentTaskRequest> tasks =
        ArgumentCaptor.forClass(CreateAgentTaskRequest.class);
    verify(agentTaskService, times(6)).createByHumanIfAbsent(tasks.capture());
    assertThat(tasks.getAllValues())
        .extracting(CreateAgentTaskRequest::sourceReference)
        .containsOnly("commercial-plan:2@v3:journey:experiment-88:attempt:2");
    assertThat(tasks.getAllValues().getFirst().description())
        .contains(
            "\"journeyAttempt\":2",
            "Psique encontrou promessa ambígua de aplicação",
            "Explicitar uso manual pela cliente");
  }

  /** Retoma Psique e Têmis sem refazer o HTML que permanece idêntico ao aprovado. */
  @Test
  void resumesReviewersWhenOnlyTheCheckoutEvidenceTransportWasBlocked() {
    when(commercialPlanService.getPlan(4L)).thenReturn(CommercialPlan.builder().id(4L).build());
    when(versionService.current(4L))
        .thenReturn(
            new CommercialPlanVersionDto(
                9L, 4L, 3, "{}", "teste", "versão de teste", Instant.now()));
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(18L);
    process.setProcessCode("landing-page-generation");
    when(processRepository.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            "landing-page-generation", "PUBLISHED"))
        .thenReturn(Optional.of(process));
    Agent dedalo = Agent.builder().id(7L).agentKey("landing-generator").nickname("Dédalo").build();
    Agent psique = Agent.builder().id(2L).agentKey("customer-agent").nickname("Psique").build();
    Agent themis = Agent.builder().id(6L).agentKey("meta-ad-approver").nickname("Têmis").build();
    AgentTask landing = task(243L, dedalo, process, "html", "COMPLETED");
    landing.setEvidenceJson("{\"checkoutUrl\":null}");
    AgentTask customer = task(244L, psique, process, "customer", "BLOCKED");
    customer.setExecutionError("Psique bloqueou o avanço por checkout não comprovado.");
    customer.setResultJson(
        "{\"decision\":\"BLOCKED\",\"requiredChanges\":[\"Persistir checkoutUrl no campo auditável\",\"Fornecer evidência auditável do checkout\"]}");
    AgentTask commercial = task(245L, themis, process, "commercial", "PENDING");
    when(taskRepository.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
            "commercial-plan:4@v3:journey:experiment-89"))
        .thenReturn(List.of(commercial, customer, landing));
    when(reviewResumeService.buildResumeBrief(
            org.mockito.ArgumentMatchers.eq(4L),
            org.mockito.ArgumentMatchers.eq(89L),
            org.mockito.ArgumentMatchers.eq(1),
            any(),
            any()))
        .thenReturn(
            Optional.of(
                "{\"resumeMode\":\"REUSE_APPROVED_LANDING_WITH_FRESH_CANONICAL_EVIDENCE\","
                    + "\"checkoutContract\":{\"validationStatus\":"
                    + "\"VALIDATED_FROM_PERSISTED_CANONICAL_BINDING\"},"
                    + "\"qualityReviewExecutionId\":\"quality-review-89\","
                    + "\"landingRegenerationAuthorized\":false}"));
    var service =
        new CommercialPlanJourneyHomologationService(
            commercialPlanService,
            versionService,
            processRepository,
            taskRepository,
            reviewResumeService,
            agentTaskService,
            new ObjectMapper());

    var result = service.request(4L, 89L);

    assertThat(result.status()).isEqualTo("REVIEW_RESUMED");
    ArgumentCaptor<CreateAgentTaskRequest> reviews =
        ArgumentCaptor.forClass(CreateAgentTaskRequest.class);
    verify(agentTaskService, times(2)).retryBlockedByHumanOrRefreshPending(reviews.capture());
    assertThat(reviews.getAllValues())
        .extracting(CreateAgentTaskRequest::assignedAgentKey)
        .containsExactly("customer-agent", "meta-ad-approver");
    assertThat(reviews.getAllValues())
        .allSatisfy(
            review ->
                assertThat(review.description())
                    .contains(
                        "REUSE_APPROVED_LANDING_WITH_FRESH_CANONICAL_EVIDENCE",
                        "VALIDATED_FROM_PERSISTED_CANONICAL_BINDING",
                        "quality-review-89",
                        "\"landingRegenerationAuthorized\":false"));
    verify(agentTaskService, never()).createByHumanIfAbsent(any());
  }

  /** Mantém a retomada idempotente enquanto Psique ou Têmis ainda processam o parecer. */
  @Test
  void keepsReviewResumeActiveWithoutOpeningLandingTask() {
    when(commercialPlanService.getPlan(4L)).thenReturn(CommercialPlan.builder().id(4L).build());
    when(versionService.current(4L))
        .thenReturn(
            new CommercialPlanVersionDto(
                9L, 4L, 3, "{}", "teste", "versão de teste", Instant.now()));
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(18L);
    process.setProcessCode("landing-page-generation");
    when(processRepository.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            "landing-page-generation", "PUBLISHED"))
        .thenReturn(Optional.of(process));
    AgentTask resumed =
        task(
            246L,
            Agent.builder().id(2L).agentKey("customer-agent").nickname("Psique").build(),
            process,
            "customer",
            "PENDING");
    resumed.setDescription(
        "{\"resumeMode\":\"REUSE_APPROVED_LANDING_WITH_FRESH_CANONICAL_EVIDENCE\"}");
    when(taskRepository.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
            "commercial-plan:4@v3:journey:experiment-89"))
        .thenReturn(List.of(resumed));
    var service =
        new CommercialPlanJourneyHomologationService(
            commercialPlanService,
            versionService,
            processRepository,
            taskRepository,
            reviewResumeService,
            agentTaskService,
            new ObjectMapper());

    var result = service.request(4L, 89L);

    assertThat(result.status()).isEqualTo("REVIEW_RESUMED");
    verify(agentTaskService, never()).createByHumanIfAbsent(any());
    verify(agentTaskService, never()).retryBlockedByHumanOrRefreshPending(any());
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
    when(active.getSourceReference())
        .thenReturn("commercial-plan:2@v3:journey:experiment-88:attempt:2");
    when(active.getProcessDefinition()).thenReturn(process);
    when(active.getAssignedAgent())
        .thenReturn(Agent.builder().id(2L).agentKey("customer-agent").nickname("Psique").build());
    when(active.getStatus()).thenReturn("PENDING");
    when(taskRepository.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
            "commercial-plan:2@v3:journey:experiment-88"))
        .thenReturn(java.util.List.of(active));
    var service =
        new CommercialPlanJourneyHomologationService(
            commercialPlanService,
            versionService,
            processRepository,
            taskRepository,
            reviewResumeService,
            agentTaskService,
            new ObjectMapper());

    service.request(2L, 88L);

    ArgumentCaptor<CreateAgentTaskRequest> tasks =
        ArgumentCaptor.forClass(CreateAgentTaskRequest.class);
    verify(agentTaskService, times(6)).createByHumanIfAbsent(tasks.capture());
    assertThat(tasks.getAllValues())
        .extracting(CreateAgentTaskRequest::sourceReference)
        .containsOnly("commercial-plan:2@v3:journey:experiment-88:attempt:2");
    assertThat(tasks.getAllValues().getFirst().description()).contains("\"journeyAttempt\":2");
  }

  /** Monta uma tarefa persistida suficiente para simular uma tentativa da jornada. */
  private AgentTask task(
      Long id, Agent agent, BusinessProcessDefinition process, String activityId, String status) {
    AgentTask task = new AgentTask();
    task.setId(id);
    task.setAssignedAgent(agent);
    task.setProcessDefinition(process);
    task.setProcessActivityId(activityId);
    task.setSourceReference("commercial-plan:4@v3:journey:experiment-89");
    task.setStatus(status);
    return task;
  }
}
