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
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.repository.TargetingElementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
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
    private final TargetingElementRepository targetingElementRepository;
    private final ExperimentAdSetWorkflowRepository adSetWorkflowRepository;
    private final ExperimentAdSetSpecRepository adSetSpecRepository;

    public ExperimentReadinessService(ExperimentService experimentService,
                                      CreativeRepository creativeRepository,
                                      TargetingElementRepository targetingElementRepository,
                                      ExperimentAdSetWorkflowRepository adSetWorkflowRepository,
                                      ExperimentAdSetSpecRepository adSetSpecRepository) {
        this.experimentService = experimentService;
        this.creativeRepository = creativeRepository;
        this.targetingElementRepository = targetingElementRepository;
        this.adSetWorkflowRepository = adSetWorkflowRepository;
        this.adSetSpecRepository = adSetSpecRepository;
    }

    @Transactional(readOnly = true)
    public ExperimentReadinessSummaryDto summarize(Long experimentId) {
        Experiment experiment = experimentService.get(experimentId);
        long creativeCount = creativeRepository.countByExperimentId(experimentId);
        boolean hasCreatives = creativeCount > 0;

        long leadPortalFlowCount = experiment.getLeadPortalFlow() != null ? 1L : 0L;
        boolean hasLeadPortalFlow = leadPortalFlowCount > 0;

        List<String> missingConfiguration = computeMissingConfiguration(experiment);
        List<TargetingElementType> missingTypes = mapMissingTargetingTypes(experiment, missingConfiguration);
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

    public boolean isReadyForCampaign(Experiment experiment) {
        return computeMissingConfiguration(experiment).isEmpty();
    }

    public List<String> computeMissingConfiguration(Experiment experiment) {
        List<String> missing = new ArrayList<>();
        if (!experiment.isCreativeApproved()) {
            missing.add("creativeApproval");
        }
        if (experiment.getKpiTargetCpl() == null) {
            missing.add("kpiTargetCpl");
        }
        if (experiment.getStopLossCpl() == null) {
            missing.add("stopLossCpl");
        }
        if (experiment.getSampleSize() == null) {
            missing.add("sampleSize");
        }
        if (experiment.getStartDate() == null) {
            missing.add("startDate");
        }
        if (experiment.getEndDate() == null) {
            missing.add("endDate");
        }
        if (experiment.getJourneyTemplate() == null) {
            missing.add("journeyTemplate");
        }
        if (!StringUtils.hasText(resolveExperimentPageId(experiment))) {
            missing.add("pageId");
        }
        if (experiment.getInstagramAccount() == null) {
            missing.add("instagramAccount");
        }
        if (isNextStepInstantForm(experiment)) {
            if (experiment.getFacebookInstantForm() == null) {
                missing.add("facebookInstantForm");
            } else {
                if (!experiment.getFacebookInstantForm().isApproved()) {
                    missing.add("facebookInstantFormApproval");
                }
                if (!experiment.getFacebookInstantForm().isPublished()) {
                    missing.add("facebookInstantFormPublication");
                }
            }
        }
        if (!hasApprovedTargeting(experiment)) {
            missing.add("approvedTargetingElements");
        }
        return List.copyOf(missing);
    }

    private List<TargetingElementType> mapMissingTargetingTypes(Experiment experiment, List<String> missingConfiguration) {
        if (!missingConfiguration.contains("approvedTargetingElements")) {
            return List.of();
        }
        Long nicheId = experiment.getNiche() != null ? experiment.getNiche().getId() : null;
        UUID hypothesisId = experiment.getHypothesisRef() != null ? experiment.getHypothesisRef().getId() : null;
        return determineMissingTargeting(nicheId, hypothesisId);
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

    private boolean hasApprovedTargeting(Experiment experiment) {
        Long nicheId = experiment.getNiche() != null ? experiment.getNiche().getId() : null;
        UUID hypothesisId = experiment.getHypothesisRef() != null ? experiment.getHypothesisRef().getId() : null;
        List<TargetingElementType> missing = determineMissingTargeting(nicheId, hypothesisId);
        return missing.isEmpty() || hasReadyAdSetSpecs(experiment.getId());
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

    private String resolveExperimentPageId(Experiment experiment) {
        if (experiment.getFacebookPage() == null) {
            return null;
        }
        return experiment.getFacebookPage().getPageId();
    }

    private boolean isNextStepInstantForm(Experiment experiment) {
        if (experiment.getJourneyTemplate() == null) {
            return false;
        }
        List<JourneyStep> steps = experiment.getJourneyTemplate().getSteps();
        if (steps == null || steps.isEmpty()) {
            return false;
        }
        List<JourneyStep> ordered = steps.stream()
                .sorted(Comparator.comparingInt(step -> step.getPosition() != null ? step.getPosition() : Integer.MAX_VALUE))
                .toList();
        JourneyStep previous = null;
        for (JourneyStep step : ordered) {
            if (previous != null && previous.getStimulusType() == JourneyStimulusType.AD) {
                return step.getStimulusType() == JourneyStimulusType.INSTANT_FORM;
            }
            previous = step;
        }
        return false;
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
