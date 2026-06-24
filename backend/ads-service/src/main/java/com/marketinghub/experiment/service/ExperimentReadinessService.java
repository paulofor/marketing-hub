package com.marketinghub.experiment.service;

import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueDto;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueType;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.repository.jpa.experiment.ExperimentTargetingSelectionRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
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
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final List<String> GERA_LANDING_REQUIRED_STAGES = List.of(
            "landing-page-wireframe",
            "landing-page-copy",
            "landing-page-image-planning",
            "landing-page-image-generation",
            "landing-page-design-preset",
            "landing-page-quality-review",
            "landing-page-deliverables"
    );

    private final ExperimentTargetingSelectionRepository targetingSelectionRepository;
    private final TargetingElementRepository targetingElementRepository;
    private final GeraLandingStageExecutionRepository geraLandingStageExecutionRepository;

    /** Cria o serviço com as fontes canônicas de prontidão do experimento. */
    public ExperimentReadinessService(ExperimentService experimentService,
                                      CreativeRepository creativeRepository,
                                      ExperimentTargetingSelectionRepository targetingSelectionRepository,
                                      TargetingElementRepository targetingElementRepository,
                                      GeraLandingStageExecutionRepository geraLandingStageExecutionRepository) {
        this.experimentService = experimentService;
        this.creativeRepository = creativeRepository;
        this.targetingSelectionRepository = targetingSelectionRepository;
        this.targetingElementRepository = targetingElementRepository;
        this.geraLandingStageExecutionRepository = geraLandingStageExecutionRepository;
    }

    /** Resume a prontidão do experimento usando apenas dados canônicos aprovados para publicação. */
    @Transactional(readOnly = true)
    public ExperimentReadinessSummaryDto summarize(Long experimentId) {
        Experiment experiment = experimentService.get(experimentId);
        long creativeCount = creativeRepository.countByExperimentIdAndStatus(experimentId, CreativeStatus.READY);
        boolean hasCreatives = creativeCount > 0;

        long leadPortalFlowCount = hasReadyLeadPortalFlow(experiment) ? 1L : 0L;
        boolean hasLeadPortalFlow = leadPortalFlowCount > 0;

        List<String> missingConfiguration = computeMissingConfiguration(experiment);
        List<TargetingElementType> missingTypes = mapMissingTargetingTypes(missingConfiguration);
        boolean hasCompleteTargeting = missingTypes.isEmpty();
        long geraLandingCompletedStageCount = countCompletedGeraLandingStages(experimentId);
        boolean hasGeraLandingPipeline = geraLandingCompletedStageCount == GERA_LANDING_REQUIRED_STAGES.size();

        List<ExperimentReadinessIssueDto> issues = new ArrayList<>();
        if (!hasCreatives) {
            issues.add(new ExperimentReadinessIssueDto(
                    ExperimentReadinessIssueType.CREATIVE,
                    "Nenhum criativo aprovado",
                    "Este experimento ainda não possui criativos aprovados para publicação.",
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

        if (!hasGeraLandingPipeline) {
            issues.add(new ExperimentReadinessIssueDto(
                    ExperimentReadinessIssueType.GERA_LANDING,
                    "GeraLanding incompleto",
                    "O fluxo do GeraLanding ainda não concluiu todas as etapas obrigatórias.",
                    "Abra a aba Gera landing, corrija etapas com falha e execute as pendentes até a finalização.",
                    List.of()
            ));
        }

        return new ExperimentReadinessSummaryDto(
                hasCreatives,
                creativeCount,
                hasLeadPortalFlow,
                leadPortalFlowCount,
                hasCompleteTargeting,
                hasGeraLandingPipeline,
                geraLandingCompletedStageCount,
                GERA_LANDING_REQUIRED_STAGES.size(),
                List.copyOf(missingTypes),
                List.copyOf(issues)
        );
    }

    /** Verifica se o experimento já pode entrar na fila de campanha paga. */
    public boolean isReadyForCampaign(Experiment experiment) {
        return computeMissingConfiguration(experiment).isEmpty();
    }

    /** Lista as configurações faltantes que bloqueiam publicação de campanha. */
    public List<String> computeMissingConfiguration(Experiment experiment) {
        List<String> missing = new ArrayList<>();
        if (!hasApprovedCreative(experiment)) {
            missing.add("creativeApproval");
        }
        if (!hasApprovedLandingDestination(experiment)) {
            missing.add("landingDestination");
        }
        return List.copyOf(missing);
    }

    /** Conta as etapas obrigatórias do GeraLanding cuja execução mais recente foi concluída. */
    private long countCompletedGeraLandingStages(Long experimentId) {
        return GERA_LANDING_REQUIRED_STAGES.stream()
                .filter(stageCode -> geraLandingStageExecutionRepository
                        .findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(experimentId, stageCode)
                        .map(GeraLandingStageExecution::getStatus)
                        .map(STATUS_COMPLETED::equalsIgnoreCase)
                        .orElse(false))
                .count();
    }

    /** Converte pendências de configuração em tipos de segmentação faltantes. */
    private List<TargetingElementType> mapMissingTargetingTypes(List<String> missingConfiguration) {
        if (!missingConfiguration.contains("approvedTargetingPackage")) {
            return List.of();
        }
        return List.of(
                TargetingElementType.JOB_TITLE
        );
    }


    /** Verifica se o experimento possui alguma segmentação configurada. */
    private boolean hasConfiguredTargeting(Experiment experiment) {
        if (experiment == null) {
            return false;
        }
        if (hasApprovedTargetingPackage(experiment)) {
            return true;
        }
        return hasSelectedTargeting(experiment.getId());
    }

    /** Verifica se existe pacote de cargo aprovado para o nicho e hipótese do experimento. */
    private boolean hasApprovedTargetingPackage(Experiment experiment) {
        if (experiment.getNiche() == null || experiment.getNiche().getId() == null) {
            return false;
        }
        return !targetingElementRepository.findApprovedForExperiment(
                experiment.getNiche().getId(),
                TargetingElementType.JOB_TITLE,
                experiment.getHypothesisRef() != null ? experiment.getHypothesisRef().getId() : null).isEmpty();
    }

    /** Verifica a aprovação real dos criativos pela fonte canônica: registros READY. */
    private boolean hasApprovedCreative(Experiment experiment) {
        if (experiment == null || experiment.getId() == null) {
            return false;
        }
        return creativeRepository.existsByExperimentIdAndStatus(experiment.getId(), CreativeStatus.READY);
    }

    /** Verifica se existe fluxo aprovado do portal do lead vinculado ao experimento. */
    private boolean hasReadyLeadPortalFlow(Experiment experiment) {
        return experiment != null
                && experiment.getLeadPortalFlow() != null
                && experiment.getLeadPortalFlow().isApproved();
    }

    /** Verifica se o usuário selecionou cargo para segmentação do experimento. */
    private boolean hasSelectedTargeting(Long experimentId) {
        if (experimentId == null) {
            return false;
        }
        return targetingSelectionRepository.countByExperimentIdAndCandidateType(
                experimentId, TargetingCandidateType.WORK_POSITION) > 0;
    }

    /** Verifica se o experimento possui URL de destino aprovada para campanha. */
    private boolean hasApprovedLandingDestination(Experiment experiment) {
        return experiment != null
                && experiment.getFollowUpActionUrl() != null
                && !experiment.getFollowUpActionUrl().isBlank();
    }

}
