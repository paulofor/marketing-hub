package com.marketinghub.creative.convergence.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.CreateAgentTaskByAgentRequest;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.dto.CreativeAgentReviewResultRequest;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.geralanding.agent.v1.LandingGenerationAgentExecutionService;
import com.marketinghub.repository.jpa.creative.convergence.CreativeConvergenceCycleRepository;
import com.marketinghub.repository.jpa.creative.convergence.CreativeConvergenceTaskRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a delegação causal entre Têmis, Dédalo e a fila de landing. */
class CreativeConvergenceServiceTest {

  /** Uma falha da landing deve criar tarefa visível e execução autônoma correlacionadas. */
  @Test
  void delegatesLandingCorrectionToDedaloAndItsCanonicalQueue() {
    CreativeConvergenceCycleRepository cycles = mock(CreativeConvergenceCycleRepository.class);
    CreativeConvergenceTaskRepository tasks = mock(CreativeConvergenceTaskRepository.class);
    AgentTaskService agentTasks = mock(AgentTaskService.class);
    LandingGenerationAgentExecutionService landing =
        mock(LandingGenerationAgentExecutionService.class);
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

    new CreativeConvergenceService(cycles, tasks, agentTasks, landing)
        .registerReview(creative, review);

    verify(agentTasks)
        .createOperationalDelegationIfAbsent(any(CreateAgentTaskByAgentRequest.class));
    verify(landing)
        .enqueueCreativeConvergenceCorrection(
            eq(88L),
            eq("creative-convergence:14:landing"),
            eq("PRODUCT_PROOF_MISSING"),
            contains("Mostrar posts"),
            contains("Desktop e mobile"));
  }
}
