package com.marketinghub.experiment.service;

import com.marketinghub.creative.repository.CreativeRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueDto;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueType;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.repository.TargetingElementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Consolida pendências básicas de preparação do experimento.
 */
@Service
public class ExperimentReadinessService {
    private final ExperimentService experimentService;
    private final CreativeRepository creativeRepository;
    private final LeadPortalFlowRepository leadPortalFlowRepository;
    private final TargetingElementRepository targetingElementRepository;

    public ExperimentReadinessService(ExperimentService experimentService,
                                      CreativeRepository creativeRepository,
                                      LeadPortalFlowRepository leadPortalFlowRepository,
                                      TargetingElementRepository targetingElementRepository) {
        this.experimentService = experimentService;
        this.creativeRepository = creativeRepository;
        this.leadPortalFlowRepository = leadPortalFlowRepository;
        this.targetingElementRepository = targetingElementRepository;
    }

    @Transactional(readOnly = true)
    public ExperimentReadinessSummaryDto summarize(Long experimentId) {
        Experiment experiment = experimentService.get(experimentId);
        long creativeCount = creativeRepository.countByExperimentId(experimentId);
        boolean hasCreatives = creativeCount > 0;

        long leadPortalFlowCount = leadPortalFlowRepository.countByExperimentId(experimentId);
        boolean hasLeadPortalFlow = leadPortalFlowCount > 0 || experiment.getLeadPortalFlow() != null;

        Long nicheId = experiment.getNiche() != null ? experiment.getNiche().getId() : null;
        UUID hypothesisId = experiment.getHypothesisRef() != null ? experiment.getHypothesisRef().getId() : null;

        List<TargetingElementType> missingTypes = determineMissingTargeting(nicheId, hypothesisId);
        boolean hasCompleteTargeting = missingTypes.isEmpty();

        List<ExperimentReadinessIssueDto> issues = new ArrayList<>();
        if (!hasCreatives) {
            issues.add(new ExperimentReadinessIssueDto(
                    ExperimentReadinessIssueType.CREATIVE,
                    "Nenhum criativo cadastrado",
                    "Este experimento ainda não possui criativos aprovados ou em produção.",
                    "Use a aba Criativos para gerar novas peças por IA ou cadastre um criativo manualmente.",
                    List.of()
            ));
        }
        if (!hasLeadPortalFlow) {
            issues.add(new ExperimentReadinessIssueDto(
                    ExperimentReadinessIssueType.LEAD_PORTAL_FLOW,
                    "Sem fluxo do portal do lead",
                    "Ainda não há um fluxo do portal vinculado a este experimento.",
                    "Solicite a geração de um fluxo ou associe um existente na aba Portal do Lead.",
                    List.of()
            ));
        }
        if (!hasCompleteTargeting) {
            issues.add(new ExperimentReadinessIssueDto(
                    ExperimentReadinessIssueType.TARGETING,
                    "Público incompleto",
                    "Faltam elementos aprovados para: " + describeTargetingTypes(missingTypes) + ".",
                    "Acesse a aba Segmentação e aprove ao menos um interesse, um cargo e um comportamento.",
                    List.copyOf(missingTypes)
            ));
        }

        return new ExperimentReadinessSummaryDto(
                hasCreatives,
                creativeCount,
                hasLeadPortalFlow,
                leadPortalFlowCount,
                hasCompleteTargeting,
                List.copyOf(missingTypes),
                List.copyOf(issues)
        );
    }

    private List<TargetingElementType> determineMissingTargeting(Long nicheId, UUID hypothesisId) {
        EnumSet<TargetingElementType> missing = EnumSet.noneOf(TargetingElementType.class);
        if (nicheId == null) {
            missing.addAll(List.of(TargetingElementType.values()));
            return List.copyOf(missing);
        }
        for (TargetingElementType type : TargetingElementType.values()) {
            boolean exists = targetingElementRepository.existsApprovedForExperiment(nicheId, type, hypothesisId);
            if (!exists) {
                missing.add(type);
            }
        }
        return List.copyOf(missing);
    }

    private String describeTargetingTypes(List<TargetingElementType> types) {
        return types.stream()
                .map(this::humanReadable)
                .collect(Collectors.joining(", "));
    }

    private String humanReadable(TargetingElementType type) {
        return switch (type) {
            case INTEREST -> "interesses";
            case JOB_TITLE -> "cargos";
            case BEHAVIOR -> "comportamentos";
        };
    }
}
