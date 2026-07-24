package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.gerasalespage.v1.GeraSalesPageAnalyticsContract;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageCode;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Centraliza a política de destino de campanha para impedir tráfego frio direto para compra.
 */
@Service
public class ExperimentCampaignDestinationPolicy {
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String MUSA_PDE_CANONICAL_HOST = "clubemusa.com.br";

    private final GeraSalesPageStageExecutionRepository geraSalesPageStageExecutionRepository;
    private final GeraSalesPagePublicationAuditRepository geraSalesPagePublicationAuditRepository;

    /** Cria a política usando as auditorias canônicas do GeraSalesPage. */
    public ExperimentCampaignDestinationPolicy(
            GeraSalesPageStageExecutionRepository geraSalesPageStageExecutionRepository,
            GeraSalesPagePublicationAuditRepository geraSalesPagePublicationAuditRepository) {
        this.geraSalesPageStageExecutionRepository = geraSalesPageStageExecutionRepository;
        this.geraSalesPagePublicationAuditRepository = geraSalesPagePublicationAuditRepository;
    }

    /** Informa se o experimento tem intenção de compra e precisa de página intermediária auditada. */
    public boolean requiresSalesPageBeforePurchase(Experiment experiment) {
        return experiment != null
                && !isPdeMembershipSubscriptionFunnel(experiment)
                && (experiment.getExperimentType() == ExperimentType.LOW_TICKET_PRODUCT
                || experiment.getCampaignObjective() == ExperimentCampaignObjective.SALES);
    }

    /** Lista violações da regra de destino para campanhas com intenção de compra. */
    public List<String> missingConfiguration(Experiment experiment) {
        if (isPdeMembershipSubscriptionFunnel(experiment)) {
            return missingPdeMembershipConfiguration(experiment);
        }
        if (!requiresSalesPageBeforePurchase(experiment)) {
            return List.of();
        }
        List<String> missing = new ArrayList<>();
        if (!hasCompleteCommercialContract(experiment)) {
            missing.add("commercialContract");
            return List.copyOf(missing);
        }
        Optional<GeraSalesPagePublicationAudit> salesPagePublication =
                latestSalesPagePublication(experiment != null ? experiment.getId() : null);
        if (!hasCompletedGeraSalesPagePipeline(experiment != null ? experiment.getId() : null)
                || salesPagePublication.isEmpty()) {
            missing.add("geraSalesPagePipeline");
            return List.copyOf(missing);
        }
        GeraSalesPagePublicationAudit publication = salesPagePublication.get();
        if (!hasAdDestinationPointingToSalesPage(experiment, publication)) {
            missing.add("salesPageAdDestination");
        }
        if (!hasRequiredSalesPageAnalyticsCollectors(publication)) {
            missing.add("salesPageAnalyticsCollectors");
        }
        return List.copyOf(missing);
    }

    /** Lista violações do funil PDE com login gratuito e paywall interno. */
    public List<String> missingPdeMembershipConfiguration(Experiment experiment) {
        if (!isPdeMembershipSubscriptionFunnel(experiment)) {
            return List.of();
        }
        List<String> missing = new ArrayList<>();
        if (!hasCompleteCommercialContract(experiment)) {
            missing.add("commercialContract");
        }
        if (!hasPdeMembershipDestination(experiment)) {
            missing.add("pdeMembershipDestination");
        }
        return List.copyOf(missing);
    }

    /** Confirma se o experimento usa o funil canônico do Clube MUSA/PDE. */
    public boolean isPdeMembershipSubscriptionFunnel(Experiment experiment) {
        return experiment != null
                && experiment.getExperimentType() == ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL;
    }

    /** Confirma que o anúncio leva para a entrada/login do Clube MUSA ou slot produtivo aprovado. */
    public boolean hasPdeMembershipDestination(Experiment experiment) {
        if (experiment == null) {
            return false;
        }
        String destinationUrl = normalizeUrl(experiment.getFollowUpActionUrl());
        return isMusaPdePublicEntry(destinationUrl);
    }

    /** Busca a página de venda publicada mais recente do experimento. */
    public Optional<GeraSalesPagePublicationAudit> latestSalesPagePublication(Long experimentId) {
        if (experimentId == null) {
            return Optional.empty();
        }
        return geraSalesPagePublicationAuditRepository.findTopByExperimentIdOrderByPublishedAtDesc(experimentId);
    }

    /** Verifica a conclusão da etapa final que publica a página de venda canônica. */
    public boolean hasCompletedGeraSalesPagePipeline(Long experimentId) {
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

    /** Confirma que o anúncio levará para a página de venda auditada, não para o checkout. */
    public boolean hasAdDestinationPointingToSalesPage(
            Experiment experiment,
            GeraSalesPagePublicationAudit publication) {
        if (experiment == null || publication == null) {
            return false;
        }
        String destinationUrl = normalizeUrl(experiment.getFollowUpActionUrl());
        String salesPageUrl = normalizeUrl(publication.getSalesPageUrl());
        String checkoutUrl = normalizeUrl(publication.getCheckoutUrl());
        return StringUtils.hasText(destinationUrl)
                && StringUtils.hasText(salesPageUrl)
                && destinationUrl.equals(salesPageUrl)
                && (!StringUtils.hasText(checkoutUrl) || !destinationUrl.equals(checkoutUrl));
    }

    /** Confirma que a página publicada possui todos os coletores mínimos de venda. */
    public boolean hasRequiredSalesPageAnalyticsCollectors(GeraSalesPagePublicationAudit publication) {
        if (publication == null || !StringUtils.hasText(publication.getHtml())) {
            return false;
        }
        return GeraSalesPageAnalyticsContract.hasRequiredCollectors(publication.getHtml());
    }

    /** Confirma se a etapa Oferta deixou um contrato comercial mínimo para venda. */
    public boolean hasCompleteCommercialContract(Experiment experiment) {
        return experiment != null
                && StringUtils.hasText(experiment.getSinglePain())
                && StringUtils.hasText(experiment.getFreeReward())
                && StringUtils.hasText(experiment.getFunnelPromise())
                && StringUtils.hasText(experiment.getPrimaryCta())
                && experiment.getUnitPrice() != null
                && experiment.getUnitPrice().signum() > 0;
    }

    /** Normaliza URL para comparação de destino sem depender de barra final. */
    private String normalizeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        String normalized = url.trim().toLowerCase(Locale.ROOT);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /** Verifica se a URL pertence ao domínio canônico ou a subdomínio produtivo do Clube MUSA. */
    private boolean isMusaPdePublicEntry(String destinationUrl) {
        return StringUtils.hasText(destinationUrl)
                && (destinationUrl.equals("https://" + MUSA_PDE_CANONICAL_HOST)
                || destinationUrl.startsWith("https://" + MUSA_PDE_CANONICAL_HOST + "/")
                || destinationUrl.matches("^https://[a-z0-9-]+\\.clubemusa\\.com\\.br($|/.*)"));
    }
}
