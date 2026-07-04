package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentType;
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
                && (experiment.getExperimentType() == ExperimentType.LOW_TICKET_PRODUCT
                || experiment.getCampaignObjective() == ExperimentCampaignObjective.SALES);
    }

    /** Lista violações da regra de destino para campanhas com intenção de compra. */
    public List<String> missingConfiguration(Experiment experiment) {
        if (!requiresSalesPageBeforePurchase(experiment)) {
            return List.of();
        }
        List<String> missing = new ArrayList<>();
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
        String html = publication.getHtml();
        return html.contains("data-mh-sales-page-analytics")
                && html.contains("page_view")
                && html.contains("page_load_metric")
                && html.contains("section_view_time")
                && html.contains("checkout_click");
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
}
