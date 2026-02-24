package com.marketinghub.experiment.service;

import com.marketinghub.creative.repository.CreativeRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueDto;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueType;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.facebookads.playbook.ExperimentAdSetSpec;
import com.marketinghub.facebookads.playbook.ExperimentAdSetSpecSlot;
import com.marketinghub.facebookads.playbook.ExperimentAdSetWorkflow;
import com.marketinghub.facebookads.playbook.ExperimentAdSetWorkflowStatus;
import com.marketinghub.facebookads.playbook.repository.ExperimentAdSetSpecRepository;
import com.marketinghub.facebookads.playbook.repository.ExperimentAdSetWorkflowRepository;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.repository.TargetingElementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final ExperimentAdSetWorkflowRepository adSetWorkflowRepository;
    private final ExperimentAdSetSpecRepository adSetSpecRepository;

    public ExperimentReadinessService(ExperimentService experimentService,
                                      CreativeRepository creativeRepository,
                                      LeadPortalFlowRepository leadPortalFlowRepository,
                                      TargetingElementRepository targetingElementRepository,
                                      ExperimentAdSetWorkflowRepository adSetWorkflowRepository,
                                      ExperimentAdSetSpecRepository adSetSpecRepository) {
        this.experimentService = experimentService;
        this.creativeRepository = creativeRepository;
        this.leadPortalFlowRepository = leadPortalFlowRepository;
        this.targetingElementRepository = targetingElementRepository;
        this.adSetWorkflowRepository = adSetWorkflowRepository;
        this.adSetSpecRepository = adSetSpecRepository;
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

        List<TargetingElementType> missingTypes = new ArrayList<>(determineMissingTargeting(nicheId, hypothesisId));
        boolean hasCompleteTargeting = missingTypes.isEmpty();
        boolean hasReadyAdSetSpecs = hasReadyAdSetSpecs(experimentId);

        if (!hasCompleteTargeting && hasReadyAdSetSpecs) {
            missingTypes.clear();
            hasCompleteTargeting = true;
        }

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

    private boolean hasReadyAdSetSpecs(Long experimentId) {
        if (experimentId == null) {
            return false;
        }
        return adSetWorkflowRepository.findByExperimentId(experimentId)
                .filter(workflow -> workflow.getStatus() == ExperimentAdSetWorkflowStatus.COMPLETED)
                .filter(this::hasEnoughReadySpecs)
                .isPresent();
    }

    private boolean hasEnoughReadySpecs(ExperimentAdSetWorkflow workflow) {
        List<ExperimentAdSetSpec> specs = adSetSpecRepository.findByWorkflowId(workflow.getId());
        if (specs.isEmpty()) {
            return false;
        }
        long readyCount = specs.stream()
                .filter(this::isSpecReady)
                .count();
        return readyCount >= ExperimentAdSetSpecSlot.values().length;
    }

    private boolean isSpecReady(ExperimentAdSetSpec spec) {
        if (spec == null) {
            return false;
        }
        if (!"READY".equalsIgnoreCase(spec.getReachStatus())) {
            return false;
        }
        String validation = spec.getValidationStatus();
        return !StringUtils.hasText(validation) || "VALID".equalsIgnoreCase(validation.trim());
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
