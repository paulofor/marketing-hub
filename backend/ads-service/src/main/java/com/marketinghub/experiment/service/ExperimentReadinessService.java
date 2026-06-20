package com.marketinghub.experiment.service;

import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCaptureDestinationType;
import com.marketinghub.ads.FacebookInstantForm;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueDto;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueType;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.repository.jpa.experiment.ExperimentTargetingSelectionRepository;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
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

    /** Resume os bloqueios operacionais que impedem a liberação do experimento para campanha. */
    @Transactional(readOnly = true)
    public ExperimentReadinessSummaryDto summarize(Long experimentId) {
        Experiment experiment = experimentService.get(experimentId);
        long creativeCount = creativeRepository.countByExperimentId(experimentId);
        boolean hasCreatives = hasApprovedCreative(experiment);

        ExperimentCaptureDestinationType captureDestinationType = resolveCaptureDestinationType(experiment);
        boolean requiresLandingDestination = captureDestinationType == ExperimentCaptureDestinationType.LANDING_PAGE;
        boolean requiresInstantFormDestination = captureDestinationType == ExperimentCaptureDestinationType.META_INSTANT_FORM;
        long leadPortalFlowCount = hasReadyLeadPortalFlow(experiment) ? 1L : 0L;
        boolean hasLeadPortalFlow = leadPortalFlowCount > 0;
        long instantFormCount = hasReadyInstantForm(experiment) ? 1L : 0L;
        boolean hasInstantForm = instantFormCount > 0;

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
        if (requiresLandingDestination && !hasApprovedLandingDestination(experiment)) {
            issues.add(new ExperimentReadinessIssueDto(
                    ExperimentReadinessIssueType.LEAD_PORTAL_FLOW,
                    "Sem landing publicada",
                    "A opção Landing Page exige uma URL final de landing consolidada no experimento.",
                    "Aprove uma landing para definir a URL de destino da campanha.",
                    List.of()
            ));
        }
        if (requiresInstantFormDestination && !hasInstantForm) {
            issues.add(new ExperimentReadinessIssueDto(
                    ExperimentReadinessIssueType.INSTANT_FORM,
                    "Sem Instant Form publicável",
                    "A opção Meta Instant Form exige um formulário aprovado, vinculado à página e com identificador ou link da Meta.",
                    "Acesse a aba Instant Forms, selecione um formulário aprovado e confirme se ele possui ID ou link da Meta.",
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
                captureDestinationType,
                hasLeadPortalFlow,
                leadPortalFlowCount,
                hasInstantForm,
                instantFormCount,
                hasCompleteTargeting,
                List.copyOf(missingTypes),
                List.copyOf(issues)
        );
    }

    /** Indica se o experimento já atende ao mínimo para entrar na fila de campanha. */
    public boolean isReadyForCampaign(Experiment experiment) {
        return computeMissingConfiguration(experiment).isEmpty();
    }

    /** Calcula as chaves de configuração ausentes usadas pela fila de publicação. */
    public List<String> computeMissingConfiguration(Experiment experiment) {
        List<String> missing = new ArrayList<>();
        if (!hasApprovedCreative(experiment)) {
            missing.add("creativeApproval");
        }
        ExperimentCaptureDestinationType captureDestinationType = resolveCaptureDestinationType(experiment);
        if (captureDestinationType == ExperimentCaptureDestinationType.LANDING_PAGE
                && !hasApprovedLandingDestination(experiment)) {
            missing.add("landingDestination");
        }
        if (captureDestinationType == ExperimentCaptureDestinationType.META_INSTANT_FORM
                && !hasReadyInstantForm(experiment)) {
            missing.add("instantFormDestination");
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

    /** Verifica se existe fluxo do portal aprovado para uso como destino Landing Page. */
    private boolean hasReadyLeadPortalFlow(Experiment experiment) {
        return experiment != null
                && experiment.getLeadPortalFlow() != null
                && experiment.getLeadPortalFlow().isApproved();
    }

    /** Verifica se existe Instant Form aprovado e endereçável pela Meta para publicação on-ad. */
    private boolean hasReadyInstantForm(Experiment experiment) {
        if (experiment == null || experiment.getFacebookInstantForm() == null) {
            return false;
        }
        FacebookInstantForm form = experiment.getFacebookInstantForm();
        return form.isApproved()
                && form.getPage() != null
                && (hasText(form.getFormId()) || hasText(form.getShareLink()));
    }

    /** Resolve o destino de captura persistido, preservando Landing Page como padrão compatível. */
    private ExperimentCaptureDestinationType resolveCaptureDestinationType(Experiment experiment) {
        if (experiment == null || experiment.getCaptureDestinationType() == null) {
            return ExperimentCaptureDestinationType.LANDING_PAGE;
        }
        return experiment.getCaptureDestinationType();
    }

    /** Indica se o texto possui conteúdo útil para validação de destino. */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean hasSelectedTargeting(Long experimentId) {
        if (experimentId == null) {
            return false;
        }
        return targetingSelectionRepository.countByExperimentIdAndCandidateType(
                experimentId, TargetingCandidateType.WORK_POSITION) > 0;
    }

    private boolean hasApprovedLandingDestination(Experiment experiment) {
        return experiment != null
                && experiment.getFollowUpActionUrl() != null
                && !experiment.getFollowUpActionUrl().isBlank();
    }

}
