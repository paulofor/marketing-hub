package com.marketinghub.experiment.service;

import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueDto;
import com.marketinghub.experiment.dto.ExperimentReadinessIssueType;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageCode;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.repository.jpa.experiment.ExperimentTargetingSelectionRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private static final List<TargetingElementType> PUBLISHABLE_TARGETING_TYPES = List.of(
            TargetingElementType.INTEREST,
            TargetingElementType.JOB_TITLE,
            TargetingElementType.BEHAVIOR
    );
    private static final Set<TargetingElementType> PUBLISHABLE_TARGETING_TYPE_SET = Set.copyOf(PUBLISHABLE_TARGETING_TYPES);

    private final GeraLandingStageExecutionRepository geraLandingStageExecutionRepository;
    private final GeraSalesPageStageExecutionRepository geraSalesPageStageExecutionRepository;

    /** Cria o serviço com as fontes canônicas de prontidão do experimento. */
    public ExperimentReadinessService(ExperimentService experimentService,
                                      CreativeRepository creativeRepository,
                                      ExperimentTargetingSelectionRepository targetingSelectionRepository,
                                      GeraLandingStageExecutionRepository geraLandingStageExecutionRepository,
                                      GeraSalesPageStageExecutionRepository geraSalesPageStageExecutionRepository) {
        this.experimentService = experimentService;
        this.creativeRepository = creativeRepository;
        this.targetingSelectionRepository = targetingSelectionRepository;
        this.geraLandingStageExecutionRepository = geraLandingStageExecutionRepository;
        this.geraSalesPageStageExecutionRepository = geraSalesPageStageExecutionRepository;
    }

    /** Resume a prontidão do experimento usando apenas dados canônicos aprovados para publicação. */
    @Transactional(readOnly = true)
    public ExperimentReadinessSummaryDto summarize(Long experimentId) {
        Experiment experiment = experimentService.get(experimentId);
        long creativeCount = creativeRepository.countByExperimentIdAndStatus(experimentId, CreativeStatus.READY);
        boolean hasCreatives = creativeCount > 0;

        long leadPortalFlowCount = hasReadyLeadPortalFlow(experiment) ? 1L : 0L;
        boolean hasLeadPortalFlow = leadPortalFlowCount > 0;

        boolean hasCompleteTargeting = hasConfiguredTargeting(experiment);
        List<TargetingElementType> missingTypes = hasCompleteTargeting
                ? List.of()
                : PUBLISHABLE_TARGETING_TYPES;
        long geraLandingCompletedStageCount = countCompletedGeraLandingStages(experimentId);
        boolean hasGeraLandingPipeline = geraLandingCompletedStageCount == GERA_LANDING_REQUIRED_STAGES.size();
        boolean lowTicket = isLowTicketProduct(experiment);
        boolean hasGeraSalesPagePipeline = hasCompletedGeraSalesPagePipeline(experimentId);

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
        if (!lowTicket && !hasLeadPortalFlow) {
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
                    "Ainda não há nenhum interesse, cargo ou comportamento publicável salvo para este experimento.",
                    "Acesse a aba Segmentação, marque ao menos um interesse, cargo ou comportamento com ID oficial da Meta e salve o público.",
                    List.copyOf(missingTypes)
            ));
        }

        if (lowTicket && !hasGeraSalesPagePipeline) {
            issues.add(new ExperimentReadinessIssueDto(
                    ExperimentReadinessIssueType.GERA_SALES_PAGE,
                    "Página de venda não foi criada pelo pipeline",
                    "Experimentos low-ticket só podem ser liberados quando o GeraSalesPage v1 concluir a publicação.",
                    "Execute ou refaça o GeraSalesPage v1 e use a página de venda gerada pelo pipeline.",
                    List.of()
            ));
        }

        if (!lowTicket && !hasGeraLandingPipeline) {
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
        if (isLowTicketProduct(experiment) && !hasCompletedGeraSalesPagePipeline(experiment != null ? experiment.getId() : null)) {
            missing.add("geraSalesPagePipeline");
        }
        if (!hasConfiguredTargeting(experiment)) {
            missing.add("approvedTargetingPackage");
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

    /** Verifica se o experimento possui segmentação escolhida e salva pelo usuário. */
    private boolean hasConfiguredTargeting(Experiment experiment) {
        if (experiment == null) {
            return false;
        }
        return hasSelectedTargeting(experiment.getId());
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

    /** Verifica se o usuário selecionou qualquer público publicável para segmentação do experimento. */
    private boolean hasSelectedTargeting(Long experimentId) {
        if (experimentId == null) {
            return false;
        }
        return targetingSelectionRepository.findByExperimentIdWithTargetingElement(experimentId).stream()
                .map(com.marketinghub.experiment.ExperimentTargetingSelection::getTargetingElement)
                .anyMatch(this::isPublishableTargetingElement);
    }

    /** Confirma que o item de público escolhido está aprovado e possui identificador oficial da Meta. */
    private boolean isPublishableTargetingElement(TargetingElement element) {
        return element != null
                && element.getStatus() == TargetingElementStatus.APPROVED
                && element.getType() != null
                && PUBLISHABLE_TARGETING_TYPE_SET.contains(element.getType())
                && element.getMetaId() != null
                && !element.getMetaId().isBlank();
    }

    /** Verifica se o experimento possui URL de destino aprovada para campanha. */
    private boolean hasApprovedLandingDestination(Experiment experiment) {
        return experiment != null
                && experiment.getFollowUpActionUrl() != null
                && !experiment.getFollowUpActionUrl().isBlank();
    }

    /** Identifica experimento de venda direta low-ticket. */
    private boolean isLowTicketProduct(Experiment experiment) {
        return experiment != null && experiment.getExperimentType() == ExperimentType.LOW_TICKET_PRODUCT;
    }

    /** Verifica a conclusão da etapa final que publica a página de venda canônica. */
    private boolean hasCompletedGeraSalesPagePipeline(Long experimentId) {
        if (experimentId == null) {
            return false;
        }
        return geraSalesPageStageExecutionRepository
                .findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
                        experimentId, GeraSalesPageStageCode.PUBLICATION_PACKAGE.code())
                .map(com.marketinghub.gerasalespage.v1.GeraSalesPageStageExecution::getStatus)
                .map(STATUS_COMPLETED::equalsIgnoreCase)
                .orElse(false);
    }

}
