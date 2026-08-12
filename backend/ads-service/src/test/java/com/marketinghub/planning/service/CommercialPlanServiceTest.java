package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanRecommendation;
import com.marketinghub.planning.CommercialPlanSimulation;
import com.marketinghub.planning.CommercialPlanStatus;
import com.marketinghub.planning.dto.CreateCommercialPlanRequest;
import com.marketinghub.planning.dto.CreateCommercialPlanSimulationRequest;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanMilestoneRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanSimulationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar as regras centrais do servico de planejamento comercial. */
@ExtendWith(MockitoExtension.class)
class CommercialPlanServiceTest {
  @Mock private CommercialPlanRepository planRepository;

  @Mock private CommercialPlanMilestoneRepository milestoneRepository;

  @Mock private CommercialPlanSimulationRepository simulationRepository;

  @Mock private MarketNicheRepository nicheRepository;

  @Mock private HypothesisRepository hypothesisRepository;

  @Mock private ExperimentRepository experimentRepository;

  @Mock private CommercialPlanExecutionSyncService executionSyncService;

  @Mock private CommercialPlanVersionService versionService;

  private CommercialPlanService service;

  /** Prepara o servico com repositorios simulados antes de cada teste. */
  @BeforeEach
  void setUp() {
    service =
        new CommercialPlanService(
            planRepository,
            milestoneRepository,
            simulationRepository,
            nicheRepository,
            hypothesisRepository,
            experimentRepository,
            executionSyncService,
            versionService);
  }

  /** Deve bloquear planos sem os gates comerciais minimos. */
  @Test
  void createBlocksPlanWhenCommercialGateIsIncomplete() {
    when(planRepository.save(any(CommercialPlan.class)))
        .thenAnswer(
            invocation -> {
              CommercialPlan plan = invocation.getArgument(0);
              plan.setId(10L);
              return plan;
            });

    CommercialPlan plan =
        service.create(
            new CreateCommercialPlanRequest(
                "Primeira venda",
                null,
                null,
                null,
                "Validar primeira venda",
                "",
                "Agenda vulneravel",
                "Kit low-ticket",
                null,
                "Meta Ads",
                "Venda",
                "1 venda",
                "100 acessos sem envio",
                LocalDate.now().plusDays(14),
                null,
                BigDecimal.valueOf(27),
                BigDecimal.valueOf(81),
                BigDecimal.valueOf(67),
                BigDecimal.valueOf(10),
                1000,
                BigDecimal.valueOf(1.5),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(5),
                BigDecimal.ZERO,
                2,
                3,
                1,
                1,
                2,
                5,
                "Validar formulario",
                null,
                null));

    assertThat(plan.getStatus()).isEqualTo(CommercialPlanStatus.BLOCKED);
    assertThat(plan.getTargetRevenue()).isEqualByComparingTo("27");
    assertThat(plan.getOperationalRevenueTarget()).isEqualByComparingTo("81");
    assertThat(plan.getExperimentsToCreate()).isEqualTo(2);
    assertThat(plan.getExperimentsToPublish()).isEqualTo(3);
    assertThat(plan.getProductsToValidate()).isEqualTo(1);
    assertThat(plan.getProductTypesToExplore()).isEqualTo(1);
    assertThat(plan.getApproachesToTest()).isEqualTo(2);
    assertThat(plan.getCustomerConversationsTarget()).isEqualTo(5);
    verify(milestoneRepository, times(9)).save(any());
  }

  /** Deve registrar simulacao corretiva quando o plano ainda tem lacuna comercial. */
  @Test
  void simulateRecommendsCorrectionWhenPlanHasCommercialGap() {
    CommercialPlan plan =
        CommercialPlan.builder()
            .id(20L)
            .name("Plano com lacuna")
            .commercialObjective("Validar primeira venda")
            .mainPain("Agenda vulneravel")
            .mainOffer("Kit low-ticket")
            .mainMetric("Venda")
            .successCriteria("1 venda")
            .stopCriteria("100 acessos sem envio")
            .deadline(LocalDate.now().plusDays(14))
            .build();
    when(planRepository.findById(20L)).thenReturn(Optional.of(plan));
    when(milestoneRepository.findByPlanIdOrderBySequenceOrderAsc(20L)).thenReturn(List.of());
    when(simulationRepository.save(any(CommercialPlanSimulation.class)))
        .thenAnswer(
            invocation -> {
              CommercialPlanSimulation simulation = invocation.getArgument(0);
              simulation.setId(30L);
              return simulation;
            });

    CommercialPlanSimulation simulation =
        service.simulate(
            20L, new CreateCommercialPlanSimulationRequest("Decisao antes de publicar."));

    assertThat(simulation.getRecommendation()).isEqualTo(CommercialPlanRecommendation.CORRECT);
    assertThat(simulation.getBestNextAction()).contains("Completar objetivo");
    verify(planRepository, times(2)).save(plan);
  }

  /** Aplica hipóteses aprovadas apenas nos campos que ainda estavam ausentes. */
  @Test
  void applyAgentAssumptionsPreservesExistingValuesAndVersionsProposal() {
    CommercialPlan plan =
        CommercialPlan.builder().id(42L).name("MUSA").offerPriceBrl(BigDecimal.valueOf(97)).build();
    when(planRepository.findById(42L)).thenReturn(Optional.of(plan));
    when(planRepository.save(any(CommercialPlan.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.applyAgentAssumptions(
        42L,
        """
        {"decision":"APPROVE","validatedAssumptions":{"offerPriceBrl":147,"variableCostPerSaleBrl":20,"expectedMonthlyTraffic":300,"expectedConversionRatePercent":2.5,"expectedCacBrl":35,"expectedRefundRatePercent":5,"fixedOperationalCostBrl":80}}
        """);

    assertThat(plan.getOfferPriceBrl()).isEqualByComparingTo("97");
    assertThat(plan.getVariableCostPerSaleBrl()).isEqualByComparingTo("20");
    assertThat(plan.getExpectedMonthlyTraffic()).isEqualTo(300);
    assertThat(plan.getExpectedConversionRatePercent()).isEqualByComparingTo("2.5");
    verify(versionService)
        .snapshot(
            plan, "ATENA_PLUTUS", "Premissas hipotéticas definidas e validadas pelos agentes");
  }

  /** Atualiza o plano quando existe um unico experimento ativo da mesma hipotese. */
  @Test
  void synchronizeRunningExperimentLinksUniqueCompatibleExperiment() {
    Hypothesis hypothesis =
        Hypothesis.builder().id(UUID.randomUUID()).title("Agenda cheia").build();
    Experiment oldExperiment =
        Experiment.builder()
            .id(84L)
            .hypothesisRef(hypothesis)
            .status(ExperimentStatus.PLANNED)
            .build();
    Experiment runningExperiment =
        Experiment.builder()
            .id(85L)
            .hypothesisRef(hypothesis)
            .status(ExperimentStatus.RUNNING)
            .build();
    CommercialPlan plan =
        CommercialPlan.builder().id(2L).hypothesis(hypothesis).experiment(oldExperiment).build();
    when(planRepository.findById(2L)).thenReturn(Optional.of(plan));
    when(milestoneRepository.findByPlanIdOrderBySequenceOrderAsc(2L)).thenReturn(List.of());
    when(experimentRepository.findByStatus(ExperimentStatus.RUNNING))
        .thenReturn(List.of(runningExperiment));
    when(planRepository.save(any(CommercialPlan.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CommercialPlan synchronizedPlan = service.synchronizeRunningExperiment(2L);

    assertThat(synchronizedPlan.getExperiment().getId()).isEqualTo(85L);
  }

  /** Usa o contexto do experimento antigo quando o plano legado nao gravou hipotese nem nicho. */
  @Test
  void synchronizeRunningExperimentUsesLinkedExperimentContextForLegacyPlan() {
    Hypothesis hypothesis =
        Hypothesis.builder().id(UUID.randomUUID()).title("Agenda cheia").build();
    Experiment oldExperiment =
        Experiment.builder()
            .id(84L)
            .hypothesisRef(hypothesis)
            .status(ExperimentStatus.PLANNED)
            .build();
    Experiment runningExperiment =
        Experiment.builder()
            .id(85L)
            .hypothesisRef(hypothesis)
            .status(ExperimentStatus.RUNNING)
            .build();
    CommercialPlan plan = CommercialPlan.builder().id(2L).experiment(oldExperiment).build();
    when(planRepository.findById(2L)).thenReturn(Optional.of(plan));
    when(milestoneRepository.findByPlanIdOrderBySequenceOrderAsc(2L)).thenReturn(List.of());
    when(experimentRepository.findByStatus(ExperimentStatus.RUNNING))
        .thenReturn(List.of(runningExperiment));
    when(planRepository.save(any(CommercialPlan.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CommercialPlan synchronizedPlan = service.synchronizeRunningExperiment(2L);

    assertThat(synchronizedPlan.getExperiment().getId()).isEqualTo(85L);
  }

  /** Bloqueia selecao automatica quando mais de um experimento ativo pertence ao plano. */
  @Test
  void synchronizeRunningExperimentRejectsAmbiguousCandidates() {
    Hypothesis hypothesis =
        Hypothesis.builder().id(UUID.randomUUID()).title("Agenda cheia").build();
    CommercialPlan plan = CommercialPlan.builder().id(2L).hypothesis(hypothesis).build();
    when(planRepository.findById(2L)).thenReturn(Optional.of(plan));
    when(milestoneRepository.findByPlanIdOrderBySequenceOrderAsc(2L)).thenReturn(List.of());
    when(experimentRepository.findByStatus(ExperimentStatus.RUNNING))
        .thenReturn(
            List.of(
                Experiment.builder()
                    .id(85L)
                    .hypothesisRef(hypothesis)
                    .status(ExperimentStatus.RUNNING)
                    .build(),
                Experiment.builder()
                    .id(86L)
                    .hypothesisRef(hypothesis)
                    .status(ExperimentStatus.RUNNING)
                    .build()));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.synchronizeRunningExperiment(2L))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("Mais de um experimento RUNNING compativel");
  }
}
