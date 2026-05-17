package com.marketinghub.experiment.service;

import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.repository.CreativeRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueDto;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueType;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.experiment.repository.ExperimentTargetingSelectionRepository;
import com.marketinghub.targeting.repository.TargetingElementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Consolida pendências básicas de preparação do experimento.
 */
@Service
public class ExperimentReadinessService {
    private final ExperimentService experimentService;
    private final CreativeRepository creativeRepository;
    private final ExperimentTargetingSelectionRepository targetingSelectionRepository;
    private final TargetingElementRepository targetingElementRepository;

    public ExperimentReadinessService(ExperimentService experimentService,
                                      CreativeRepository creativeRepository,
                                      ExperimentTargetingSelectionRepository targetingSelectionRepository,
                                      TargetingElementRepository targetingElementRepository) {
        this.experimentService = experimentService;
        this.creativeRepository = creativeRepository;
        this.targetingSelectionRepository = targetingSelectionRepository;
        this.targetingElementRepository = targetingElementRepository;
    }

    @Transactional(readOnly = true)
    public ExperimentReadinessSummaryDto summarize(Long experimentId) {
        Experiment experiment = experimentService.get(experimentId);
        long creativeCount = creativeRepository.countByExperimentId(experimentId);
        boolean hasCreatives = hasApprovedCreative(experiment);

        long leadPortalFlowCount = hasReadyLeadPortalFlow(experiment) ? 1L : 0L;
        boolean hasLeadPortalFlow = leadPortalFlowCount > 0;

        List<String> missingConfiguration = computeMissingConfiguration(experiment);
        List<TargetingElementType> missingTypes = mapMissingTargetingTypes(missingConfiguration);
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
                    "Público não selecionado",
                    "Ainda não há nenhuma segmentação salva para este experimento.",
                    "Acesse a aba Segmentação, marque ao menos um cargo com ID oficial da Meta e salve o público.",
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

    public boolean isReadyForCampaign(Experiment experiment) {
        return computeMissingConfiguration(experiment).isEmpty();
    }

    public List<String> computeMissingConfiguration(Experiment experiment) {
        List<String> missing = new ArrayList<>();
        if (!hasApprovedCreative(experiment)) {
            missing.add("creativeApproval");
        }
        if (!hasReadyLeadPortalFlow(experiment)) {
            missing.add("leadPortalFlow");
        }
        if (!hasConfiguredTargeting(experiment)) {
            missing.add("approvedTargetingPackage");
        }
        return List.copyOf(missing);
    }

    private List<TargetingElementType> mapMissingTargetingTypes(List<String> missingConfiguration) {
        if (!missingConfiguration.contains("approvedTargetingPackage")) {
            return List.of();
        }
        return List.of(
                TargetingElementType.JOB_TITLE
        );
    }


    private boolean hasConfiguredTargeting(Experiment experiment) {
        if (experiment == null) {
            return false;
        }
        if (hasApprovedTargetingPackage(experiment)) {
            return true;
        }
        return hasSelectedTargeting(experiment.getId());
    }

    private boolean hasApprovedTargetingPackage(Experiment experiment) {
        if (experiment.getNiche() == null || experiment.getNiche().getId() == null) {
            return false;
        }
        return !targetingElementRepository.findApprovedForExperiment(
                experiment.getNiche().getId(),
                TargetingElementType.JOB_TITLE,
                experiment.getHypothesisRef() != null ? experiment.getHypothesisRef().getId() : null).isEmpty();
    }

    private boolean hasApprovedCreative(Experiment experiment) {
        if (experiment == null || experiment.getId() == null) {
            return false;
        }
        return experiment.isCreativeApproved()
                && creativeRepository.existsByExperimentIdAndStatus(experiment.getId(), CreativeStatus.READY);
    }

    private boolean hasReadyLeadPortalFlow(Experiment experiment) {
        return experiment != null
                && experiment.getLeadPortalFlow() != null
                && experiment.getLeadPortalFlow().isApproved();
    }

    private boolean hasSelectedTargeting(Long experimentId) {
        if (experimentId == null) {
            return false;
        }
        return targetingSelectionRepository.countByExperimentIdAndCandidateType(
                experimentId, TargetingCandidateType.WORK_POSITION) > 0;
    }


}
