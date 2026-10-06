package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecution;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecutionStatus;
import com.marketinghub.financialagent.FinancialAgentExecution;
import com.marketinghub.financialagent.FinancialAgentExecutionStatus;
import com.marketinghub.financialagent.service.FinancialAgentExecutionResponse;
import com.marketinghub.financialagent.service.FinancialAgentService;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.planning.service.CommercialPlanVersionService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistExecutionRepository;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: impedir inferências repetidas e recuperação fora do contexto autorizado. */
class ProcessRunAssumptionRecoveryTest {
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final ExperimentStrategistExecutionRepository proposals =
      mock(ExperimentStrategistExecutionRepository.class);
  private final FinancialAgentExecutionRepository financial =
      mock(FinancialAgentExecutionRepository.class);
  private final CommercialPlanVersionService versions = mock(CommercialPlanVersionService.class);
  private final FinancialAgentService plutus = mock(FinancialAgentService.class);
  private final ProcessRunAssumptionRecovery recovery =
      new ProcessRunAssumptionRecovery(plans, experiments, proposals, financial, versions, plutus);
  private ProcessRun run;
  private Product product;
  private Experiment experiment;
  private ExperimentStrategistExecution proposal;

  /** Configura identidades sintéticas independentes do caso observado em produção. */
  @BeforeEach
  void setup() {
    product = new Product();
    product.setId(81001L);
    product.setAutomaticExecutionEnabled(true);
    experiment = new Experiment();
    experiment.setId(81097L);
    experiment.setProduct(product);
    experiment.setStatus(ExperimentStatus.PLANNED);
    var plan = new CommercialPlan();
    plan.setId(81034L);
    plan.setExperiment(experiment);
    run = new ProcessRun();
    run.setProductId(product.getId());
    run.setSourceReference("experiment:" + experiment.getId());
    run.setStatus("WAITING_INPUT");
    run.setCurrentActivityId("communicationContract");
    run.setCreatedAt(Instant.parse("2026-10-01T00:00:00Z"));
    proposal = new ExperimentStrategistExecution();
    proposal.setId(81019L);
    proposal.setCommercialPlan(plan);
    proposal.setStatus(ExperimentStrategistExecutionStatus.COMPLETED);
    proposal.setCreatedAt(run.getCreatedAt().plusSeconds(20));
    proposal.setEvidenceSnapshot("{\"commercialPlan\":{\"id\":81034,\"version\":4}}");
    proposal.setRecommendationJson("{\"proposedAssumptions\":{\"offerPriceBrl\":79}}");
    when(experiments.findById(experiment.getId())).thenReturn(Optional.of(experiment));
    when(plans.findByExperimentReference(experiment.getId())).thenReturn(List.of(plan));
    when(proposals.findFirstByCommercialPlanIdAndAuthorityModeOrderByCreatedAtDescIdDesc(
            plan.getId(), "COMMERCIAL_ASSUMPTIONS_PROPOSAL"))
        .thenReturn(Optional.of(proposal));
    when(versions.current(plan.getId()))
        .thenReturn(
            new CommercialPlanVersionDto(
                1L, plan.getId(), 4, "{}", "QA", "QA", run.getCreatedAt()));
    var response = mock(FinancialAgentExecutionResponse.class);
    when(response.id()).thenReturn(81071L);
    when(response.agentTaskId()).thenReturn(81589L);
    when(plutus.startAssumptionValidation(
            plan.getId(), proposal.getId(), proposal.getRecommendationJson()))
        .thenReturn(response);
  }

  /** Reutiliza os bytes persistidos de Atena, sem nova pesquisa ou promoção de gate. */
  @Test
  void recoversOnlyMissingHandoff() {
    assertThat(recovery.recover(run, "pde-communication-sales-journey"))
        .contains(new ProcessRunAssumptionRecovery.Recovered(81019L, 81071L, 81589L));
    verify(plutus).startAssumptionValidation(81034L, 81019L, proposal.getRecommendationJson());
    verify(proposals, never()).save(any());
  }

  /** Uma avaliação existente em qualquer estado continua sendo o único resultado da proposta. */
  @ParameterizedTest
  @ValueSource(strings = {"PENDING", "RUNNING", "COMPLETED", "FAILED"})
  void neverRepeatsExistingFinancialExecution(String status) {
    var existing = new FinancialAgentExecution();
    existing.setStatus(FinancialAgentExecutionStatus.valueOf(status));
    existing.setReconciliationJson("{\"decision\":\"REJECT\"}");
    when(financial.findByStrategistExecutionId(proposal.getId())).thenReturn(Optional.of(existing));
    assertThat(recovery.recover(run, "pde-communication-sales-journey")).isEmpty();
    verifyNoInteractions(plutus);
  }

  /** Pausa, encerramento e falha não são autorização de novas tarefas. */
  @ParameterizedTest
  @ValueSource(strings = {"PAUSED", "PAUSING", "COMPLETED", "CLOSED", "ERROR", "WAITING_HUMAN"})
  void preservesExecutionAuthority(String status) {
    run.setStatus(status);
    assertThat(recovery.recover(run, "pde-communication-sales-journey")).isEmpty();
    verifyNoInteractions(plutus, proposals);
  }

  /** A recuperação não migra uma proposta antiga nem a aplica a uma versão nova. */
  @Test
  void rejectsOldProposalAndChangedVersion() {
    proposal.setCreatedAt(run.getCreatedAt().minusSeconds(1));
    assertThat(recovery.recover(run, "pde-communication-sales-journey")).isEmpty();
    proposal.setCreatedAt(run.getCreatedAt().plusSeconds(1));
    proposal.setEvidenceSnapshot("{\"commercialPlan\":{\"id\":81034,\"version\":3}}");
    assertThat(recovery.recover(run, "pde-communication-sales-journey")).isEmpty();
    verifyNoInteractions(plutus);
  }

  /** Preserva STOP, propriedade do produto, encerramento e vínculos comerciais ambíguos. */
  @Test
  void refusesUnsafeOrAmbiguousScope() {
    product.setAutomaticExecutionEnabled(false);
    assertThat(recovery.recover(run, "pde-communication-sales-journey")).isEmpty();
    product.setAutomaticExecutionEnabled(true);
    product.setId(99999L);
    assertThat(recovery.recover(run, "pde-communication-sales-journey")).isEmpty();
    product.setId(run.getProductId());
    experiment.setStatus(ExperimentStatus.INVALIDATED);
    assertThat(recovery.recover(run, "pde-communication-sales-journey")).isEmpty();
    experiment.setStatus(ExperimentStatus.PLANNED);
    when(plans.findByExperimentReference(experiment.getId()))
        .thenReturn(List.of(proposal.getCommercialPlan(), new CommercialPlan()));
    assertThat(recovery.recover(run, "pde-communication-sales-journey")).isEmpty();
    verifyNoInteractions(plutus);
  }

  /** Uma fonte incompleta ou ainda em andamento não é promovida a proposta concluída. */
  @Test
  void waitsForCompletePaidSource() {
    proposal.setStatus(ExperimentStrategistExecutionStatus.RUNNING);
    assertThat(recovery.recover(run, "pde-communication-sales-journey")).isEmpty();
    proposal.setStatus(ExperimentStrategistExecutionStatus.COMPLETED);
    proposal.setRecommendationJson(null);
    assertThat(recovery.recover(run, "pde-communication-sales-journey")).isEmpty();
    verifyNoInteractions(plutus);
  }
}
