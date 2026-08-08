package com.marketinghub.agentorchestration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agentorchestration.AgentOrchestrationCase;
import com.marketinghub.agentorchestration.AgentOrchestrationStatus;
import com.marketinghub.agentorchestration.AgentTaskState;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecution;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecutionStatus;
import com.marketinghub.growthoperator.GrowthOperatorExecution;
import com.marketinghub.growthoperator.GrowthOperatorExecutionStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.repository.jpa.agentorchestration.AgentOrchestrationCaseRepository;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistExecutionRepository;
import com.marketinghub.repository.jpa.growthoperator.GrowthOperatorExecutionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar a coordenacao deterministica e segura dos agentes comerciais. */
class AgentOrchestrationServiceTest {
  private AgentOrchestrationCaseRepository cases;
  private CommercialPlanService plans;
  private ExperimentStrategistExecutionRepository strategistExecutions;
  private GrowthOperatorExecutionRepository growthExecutions;
  private CreativeRepository creatives;
  private AgentOrchestrationService service;
  private CommercialPlan plan;

  /** Prepara um planejamento isolado e repositorios simulados. */
  @BeforeEach
  void setUp() {
    cases = mock(AgentOrchestrationCaseRepository.class);
    plans = mock(CommercialPlanService.class);
    strategistExecutions = mock(ExperimentStrategistExecutionRepository.class);
    growthExecutions = mock(GrowthOperatorExecutionRepository.class);
    creatives = mock(CreativeRepository.class);
    service =
        new AgentOrchestrationService(
            cases, plans, strategistExecutions, growthExecutions, creatives, new ObjectMapper());
    Experiment experiment = new Experiment();
    experiment.setId(85L);
    plan = CommercialPlan.builder().id(2L).experiment(experiment).build();
    when(plans.getPlan(2L)).thenReturn(plan);
    when(cases.findByCommercialPlanIdAndExperimentId(2L, 85L)).thenReturn(Optional.empty());
    when(cases.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(strategistExecutions.findFirstByCommercialPlanIdOrderByCreatedAtDesc(2L))
        .thenReturn(Optional.empty());
    when(growthExecutions.findFirstByCommercialPlanIdOrderByCreatedAtDesc(2L))
        .thenReturn(Optional.empty());
    when(creatives.findFirstByExperimentIdOrderByIdDesc(85L)).thenReturn(Optional.empty());
  }

  /** Exige contexto de experimento antes de coordenar agentes. */
  @Test
  void blocksPlanWithoutExperiment() {
    when(plans.getPlan(2L)).thenReturn(CommercialPlan.builder().id(2L).build());

    assertThatThrownBy(() -> service.synchronize(2L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("sem experimento");
  }

  /** Registra todas as contribuicoes ausentes como obrigatorias. */
  @Test
  void exposesMissingAgentWork() {
    var result = service.synchronize(2L);

    assertThat(result.status()).isEqualTo(AgentOrchestrationStatus.WAITING_FOR_AGENTS);
    assertThat(result.strategistState()).isEqualTo(AgentTaskState.REQUIRED);
    assertThat(result.growthOperatorState()).isEqualTo(AgentTaskState.REQUIRED);
    assertThat(result.adSpecialistState()).isEqualTo(AgentTaskState.REQUIRED);
    assertThat(result.humanApprovalRequired()).isTrue();
  }

  /** Libera apenas decisao humana quando os tres pareceres estao concluidos. */
  @Test
  void becomesReadyWithoutExecutingCommercialAction() {
    ExperimentStrategistExecution strategist =
        strategist(11L, ExperimentStrategistExecutionStatus.COMPLETED);
    GrowthOperatorExecution operator = operator(12L, GrowthOperatorExecutionStatus.COMPLETED, 85L);
    Creative creative = creative(13L, CreativeAgentReviewStatus.APPROVED);
    when(strategistExecutions.findFirstByCommercialPlanIdOrderByCreatedAtDesc(2L))
        .thenReturn(Optional.of(strategist));
    when(growthExecutions.findFirstByCommercialPlanIdOrderByCreatedAtDesc(2L))
        .thenReturn(Optional.of(operator));
    when(creatives.findFirstByExperimentIdOrderByIdDesc(85L)).thenReturn(Optional.of(creative));

    var result = service.synchronize(2L);

    assertThat(result.status()).isEqualTo(AgentOrchestrationStatus.READY_FOR_HUMAN_DECISION);
    assertThat(result.blocker()).isNull();
    assertThat(result.evidenceSnapshot()).contains("\"humanApprovalRequired\":true");
  }

  /** Bloqueia o caso quando o criativo foi reprovado pelo especialista. */
  @Test
  void blocksRejectedCreative() {
    when(creatives.findFirstByExperimentIdOrderByIdDesc(85L))
        .thenReturn(Optional.of(creative(13L, CreativeAgentReviewStatus.REJECTED)));

    var result = service.synchronize(2L);

    assertThat(result.status()).isEqualTo(AgentOrchestrationStatus.BLOCKED);
    assertThat(result.blocker()).contains("nao passou");
  }

  /** Bloqueia diagnostico congelado com identificador de outro experimento. */
  @Test
  void blocksGrowthOperatorFromAnotherExperiment() {
    GrowthOperatorExecution operator = operator(12L, GrowthOperatorExecutionStatus.COMPLETED, 84L);
    when(growthExecutions.findFirstByCommercialPlanIdOrderByCreatedAtDesc(2L))
        .thenReturn(Optional.of(operator));

    var result = service.synchronize(2L);

    assertThat(result.growthOperatorState()).isEqualTo(AgentTaskState.BLOCKED);
    assertThat(result.blocker()).contains("experimento diferente");
  }

  /** Reutiliza o mesmo caso para impedir coordenacoes duplicadas. */
  @Test
  void reconcilesExistingCaseIdempotently() {
    AgentOrchestrationCase existing = new AgentOrchestrationCase();
    existing.setId(99L);
    when(cases.findByCommercialPlanIdAndExperimentId(2L, 85L)).thenReturn(Optional.of(existing));

    var result = service.synchronize(2L);

    assertThat(result.id()).isEqualTo(99L);
    verify(cases).save(existing);
  }

  /** Cria uma execucao estrategica simulada. */
  private ExperimentStrategistExecution strategist(
      Long id, ExperimentStrategistExecutionStatus status) {
    ExperimentStrategistExecution value = new ExperimentStrategistExecution();
    value.setId(id);
    value.setCommercialPlan(plan);
    value.setStatus(status);
    return value;
  }

  /** Cria uma execucao do Operador com snapshot imutavel do experimento. */
  private GrowthOperatorExecution operator(
      Long id, GrowthOperatorExecutionStatus status, Long experimentId) {
    GrowthOperatorExecution value = new GrowthOperatorExecution();
    value.setId(id);
    value.setCommercialPlan(plan);
    value.setStatus(status);
    value.setEvidenceSnapshot("{\"experimentId\":" + experimentId + "}");
    return value;
  }

  /** Cria um criativo simulado com parecer tecnico. */
  private Creative creative(Long id, CreativeAgentReviewStatus status) {
    Creative value = new Creative();
    value.setId(id);
    value.setAgentReviewStatus(status);
    return value;
  }
}
