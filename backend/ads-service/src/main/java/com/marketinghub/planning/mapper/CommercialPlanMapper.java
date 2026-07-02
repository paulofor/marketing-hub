package com.marketinghub.planning.mapper;

import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanMilestone;
import com.marketinghub.planning.CommercialPlanSimulation;
import com.marketinghub.planning.dto.CommercialPlanDto;
import com.marketinghub.planning.dto.CommercialPlanMilestoneDto;
import com.marketinghub.planning.dto.CommercialPlanSimulationDto;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

/** Responsabilidade: converter entidades de planejamento comercial em contratos de API. */
@Component
public class CommercialPlanMapper {
    /** Converte um plano completo para DTO com marcos e simulacoes. */
    public CommercialPlanDto toDto(
            CommercialPlan plan,
            List<CommercialPlanMilestone> milestones,
            List<CommercialPlanSimulation> simulations) {
        return new CommercialPlanDto(
                plan.getId(),
                plan.getName(),
                plan.getPlanType(),
                plan.getStatus(),
                plan.getNiche() != null ? plan.getNiche().getId() : null,
                plan.getNiche() != null ? plan.getNiche().getName() : null,
                plan.getHypothesis() != null ? plan.getHypothesis().getId() : null,
                plan.getHypothesis() != null ? plan.getHypothesis().getTitle() : null,
                plan.getExperiment() != null ? plan.getExperiment().getId() : null,
                plan.getExperiment() != null ? plan.getExperiment().getName() : null,
                plan.getCommercialObjective(),
                plan.getTargetAudience(),
                plan.getMainPain(),
                plan.getMainOffer(),
                plan.getMainLeadMagnet(),
                plan.getMainChannel(),
                plan.getMainMetric(),
                plan.getSuccessCriteria(),
                plan.getStopCriteria(),
                plan.getDeadline(),
                plan.getMaxBudget(),
                plan.getTargetRevenue(),
                plan.getOperationalRevenueTarget(),
                plan.getExperimentsToCreate(),
                plan.getExperimentsToPublish(),
                daysRemaining(plan),
                plan.getNextAction(),
                plan.getCurrentBlocker(),
                plan.getRootCause(),
                plan.getMostLikelyScenario(),
                plan.getMainFutureRisk(),
                plan.getActionToAvoid(),
                milestones.stream().map(this::toMilestoneDto).toList(),
                simulations.stream().map(this::toSimulationDto).toList(),
                plan.getCreatedAt(),
                plan.getUpdatedAt());
    }

    /** Converte um marco comercial para DTO. */
    public CommercialPlanMilestoneDto toMilestoneDto(CommercialPlanMilestone milestone) {
        return new CommercialPlanMilestoneDto(
                milestone.getId(),
                milestone.getSequenceOrder(),
                milestone.getCode(),
                milestone.getName(),
                milestone.getStatus(),
                milestone.getDueDate(),
                milestone.getTargetCost(),
                milestone.getTargetRevenue(),
                milestone.getExperimentsToCreate(),
                milestone.getExperimentsToPublish(),
                milestone.getEvidenceSource(),
                milestone.getBlocker(),
                milestone.getRecommendedNextAction());
    }

    /** Converte uma simulacao para DTO. */
    public CommercialPlanSimulationDto toSimulationDto(CommercialPlanSimulation simulation) {
        return new CommercialPlanSimulationDto(
                simulation.getId(),
                simulation.getRecommendation(),
                simulation.getMostLikelyScenario(),
                simulation.getBestRealisticScenario(),
                simulation.getWorstLikelyScenario(),
                simulation.getMainRisk(),
                simulation.getBestNextAction(),
                simulation.getActionToAvoid(),
                simulation.getContinueCondition(),
                simulation.getStopCondition(),
                simulation.getEvidence7Days(),
                simulation.getEvidence14Days(),
                simulation.getEvidence30Days(),
                simulation.getDecisionNotes(),
                simulation.getCreatedAt());
    }

    /** Calcula dias restantes ate o prazo final do plano. */
    private long daysRemaining(CommercialPlan plan) {
        if (plan.getDeadline() == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), plan.getDeadline());
    }
}
