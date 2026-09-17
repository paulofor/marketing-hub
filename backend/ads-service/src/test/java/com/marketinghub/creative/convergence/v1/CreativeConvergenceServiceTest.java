package com.marketinghub.creative.convergence.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CreateAgentTaskByAgentRequest;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.dto.CreativeAgentReviewResultRequest;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.planning.service.CommercialPlanVersionService;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.creative.convergence.CreativeConvergenceCycleRepository;
import com.marketinghub.repository.jpa.creative.convergence.CreativeConvergenceTaskRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: validar a delegação causal entre Têmis, Íris e a fila BPM de landing. */
class CreativeConvergenceServiceTest {

  /**
   * Preserva a decisão e a correção verificável quando um sucessor ainda não possui plano próprio.
   */
  @Test
  void preservesLandingCorrectionWithoutCrossingToPredecessorPlan() {
    CreativeConvergenceCycleRepository cycles = mock(CreativeConvergenceCycleRepository.class);
    CreativeConvergenceTaskRepository tasks = mock(CreativeConvergenceTaskRepository.class);
    AgentTaskService agentTasks = mock(AgentTaskService.class);
    BusinessProcessDefinitionRepository processes = mock(BusinessProcessDefinitionRepository.class);
    CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
    CommercialPlanVersionService versions = mock(CommercialPlanVersionService.class);
    when(cycles.findFirstByRootCreativeIdAndStatusOrderByIdDesc(
            529L, ConvergenceCycleStatus.ACTIVE))
        .thenReturn(Optional.empty());
    when(cycles.save(any(CreativeConvergenceCycle.class)))
        .thenAnswer(
            invocation -> {
              CreativeConvergenceCycle cycle = invocation.getArgument(0);
              if (cycle.getId() == null) cycle.setId(88L);
              return cycle;
            });
    when(tasks.findByCycleIdOrderByIdAsc(88L)).thenReturn(List.of());
    when(tasks.existsByCycleIdAndFingerprint(eq(88L), any())).thenReturn(false);
    when(plans.findByExperimentReference(92L)).thenReturn(List.of());

    Experiment experiment = new Experiment();
    experiment.setId(92L);
    Creative creative = new Creative();
    creative.setId(529L);
    creative.setExperiment(experiment);
    CreativeAgentReviewResultRequest review = mock(CreativeAgentReviewResultRequest.class);
    when(review.decision()).thenReturn(CreativeAgentReviewStatus.ADJUST);
    when(review.costUsd()).thenReturn(BigDecimal.ZERO);
    when(review.correctionTargets())
        .thenReturn(
            List.of(
                new CreativeAgentReviewResultRequest.ConvergenceCorrectionTarget(
                    "LANDING",
                    "DESTINATION_UNAVAILABLE",
                    "Disponibilizar a experiência comercial exata do experimento.",
                    "A URL pública responde com oferta, checkout e versão correspondentes.")));

    new CreativeConvergenceService(cycles, tasks, agentTasks, processes, plans, versions)
        .registerReview(creative, review);

    ArgumentCaptor<CreativeConvergenceTask> task =
        ArgumentCaptor.forClass(CreativeConvergenceTask.class);
    verify(tasks).save(task.capture());
    assertThat(task.getValue().getCreativeId()).isEqualTo(529L);
    assertThat(task.getValue().getTarget()).isEqualTo(ConvergenceTaskTarget.LANDING);
    assertThat(task.getValue().getIssueCode()).isEqualTo("DESTINATION_UNAVAILABLE");
    verify(agentTasks, never()).createOperationalDelegationIfAbsent(any());
    verify(processes, never())
        .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(any(), any());
  }

  /** Uma falha da landing deve abrir as etapas regulares de Íris na mesma execução. */
  @Test
  void delegatesLandingCorrectionToIrisCanonicalActivities() {
    CreativeConvergenceCycleRepository cycles = mock(CreativeConvergenceCycleRepository.class);
    CreativeConvergenceTaskRepository tasks = mock(CreativeConvergenceTaskRepository.class);
    AgentTaskService agentTasks = mock(AgentTaskService.class);
    BusinessProcessDefinitionRepository processes = mock(BusinessProcessDefinitionRepository.class);
    CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
    CommercialPlanVersionService versions = mock(CommercialPlanVersionService.class);
    when(cycles.findFirstByRootCreativeIdAndStatusOrderByIdDesc(
            326L, ConvergenceCycleStatus.ACTIVE))
        .thenReturn(Optional.empty());
    when(cycles.save(any(CreativeConvergenceCycle.class)))
        .thenAnswer(
            invocation -> {
              CreativeConvergenceCycle cycle = invocation.getArgument(0);
              if (cycle.getId() == null) cycle.setId(14L);
              return cycle;
            });
    when(tasks.findByCycleIdOrderByIdAsc(14L)).thenReturn(List.of());
    when(tasks.existsByCycleIdAndFingerprint(eq(14L), any())).thenReturn(false);
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(18L);
    process.setProcessCode("landing-page-generation");
    process.setStatus("PUBLISHED");
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            "landing-page-generation", "PUBLISHED"))
        .thenReturn(Optional.of(process));
    when(plans.findByExperimentReference(88L))
        .thenReturn(List.of(CommercialPlan.builder().id(4L).build()));
    when(versions.current(4L))
        .thenReturn(
            new CommercialPlanVersionDto(
                7L, 4L, 3, "{}", "teste", "versão de teste", Instant.now()));

    Experiment experiment = new Experiment();
    experiment.setId(88L);
    Creative creative = new Creative();
    creative.setId(326L);
    creative.setExperiment(experiment);
    CreativeAgentReviewResultRequest review = mock(CreativeAgentReviewResultRequest.class);
    when(review.decision()).thenReturn(CreativeAgentReviewStatus.REJECTED);
    when(review.costUsd()).thenReturn(BigDecimal.ZERO);
    when(review.correctionTargets())
        .thenReturn(
            List.of(
                new CreativeAgentReviewResultRequest.ConvergenceCorrectionTarget(
                    "LANDING",
                    "PRODUCT_PROOF_MISSING",
                    "Mostrar posts, stories e legendas digitais personalizados.",
                    "Desktop e mobile exibem exemplos reais antes do primeiro CTA.")));

    new CreativeConvergenceService(cycles, tasks, agentTasks, processes, plans, versions)
        .registerReview(creative, review);

    ArgumentCaptor<CreateAgentTaskByAgentRequest> delegations =
        ArgumentCaptor.forClass(CreateAgentTaskByAgentRequest.class);
    verify(agentTasks, times(4)).createOperationalDelegationIfAbsent(delegations.capture());
    assertThat(delegations.getAllValues())
        .extracting(CreateAgentTaskByAgentRequest::assignedAgentKey)
        .containsOnly("communication-director");
    assertThat(delegations.getAllValues())
        .extracting(CreateAgentTaskByAgentRequest::processActivityId)
        .containsExactly("select", "strategy", "compose", "html");
    assertThat(delegations.getAllValues())
        .allSatisfy(
            request -> {
              assertThat(request.sourceReference())
                  .isEqualTo("commercial-plan:4@v3:convergence:14");
              assertThat(request.processDefinitionId()).isEqualTo(18L);
              assertThat(request.exceptional()).isFalse();
              assertThat(request.description()).contains("Mostrar posts", "Desktop e mobile");
            });
  }
}
