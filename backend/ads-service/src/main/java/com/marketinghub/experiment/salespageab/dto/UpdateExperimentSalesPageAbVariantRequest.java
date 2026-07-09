package com.marketinghub.experiment.salespageab.dto;

import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbVariantStatus;
import java.math.BigDecimal;

/** Responsabilidade: receber alteracoes operacionais de uma variante A/B. */
public record UpdateExperimentSalesPageAbVariantRequest(
        ExperimentSalesPageAbVariantStatus status,
        BigDecimal trafficWeight,
        String salesPageUrl,
        String checkoutUrl,
        String adDestinationUrl,
        String analyticsVariantParam,
        Long publicationAuditId,
        Long experimentVideoAssetId,
        Boolean requiredCollectorsPresent) {
}
