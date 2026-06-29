package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar as regras centrais do servico de planejamento comercial. */
@ExtendWith(MockitoExtension.class)
class CommercialPlanServiceTest {
    @Mock
    private CommercialPlanRepository planRepository;

    @Mock
    private CommercialPlanMilestoneRepository milestoneRepository;

    @Mock
    private CommercialPlanSimulationRepository simulationRepository;

    @Mock
    private MarketNicheRepository nicheRepository;

    @Mock
    private HypothesisRepository hypothesisRepository;

    @Mock
    private ExperimentRepository experimentRepository;

    private CommercialPlanService service;

    /** Prepara o servico com repositorios simulados antes de cada teste. */
    @BeforeEach
    void setUp() {
        service = new CommercialPlanService(
                planRepository,
                milestoneRepository,
                simulationRepository,
                nicheRepository,
                hypothesisRepository,
                experimentRepository);
    }

    /** Deve bloquear planos sem os gates comerciais minimos. */
    @Test
    void createBlocksPlanWhenCommercialGateIsIncomplete() {
        when(planRepository.save(any(CommercialPlan.class))).thenAnswer(invocation -> {
            CommercialPlan plan = invocation.getArgument(0);
            plan.setId(10L);
            return plan;
        });

        CommercialPlan plan = service.create(new CreateCommercialPlanRequest(
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
                "Validar formulario",
                null,
                null));

        assertThat(plan.getStatus()).isEqualTo(CommercialPlanStatus.BLOCKED);
        verify(milestoneRepository, times(9)).save(any());
    }

    /** Deve registrar simulacao corretiva quando o plano ainda tem lacuna comercial. */
    @Test
    void simulateRecommendsCorrectionWhenPlanHasCommercialGap() {
        CommercialPlan plan = CommercialPlan.builder()
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
        when(simulationRepository.save(any(CommercialPlanSimulation.class))).thenAnswer(invocation -> {
            CommercialPlanSimulation simulation = invocation.getArgument(0);
            simulation.setId(30L);
            return simulation;
        });

        CommercialPlanSimulation simulation = service.simulate(
                20L,
                new CreateCommercialPlanSimulationRequest("Decisao antes de publicar."));

        assertThat(simulation.getRecommendation()).isEqualTo(CommercialPlanRecommendation.CORRECT);
        assertThat(simulation.getBestNextAction()).contains("Completar objetivo");
        verify(planRepository).save(plan);
    }
}
