package com.marketinghub.experiment.salespageab.dto;

import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbVariantStatus;
import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbVariantType;
import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: expor uma variante de teste A/B para UI e workers. */
public record ExperimentSalesPageAbVariantDto(
        Long id,
        String variantKey,
        String name,
        ExperimentSalesPageAbVariantType variantType,
        ExperimentSalesPageAbVariantStatus status,
        BigDecimal trafficWeight,
        String salesPageUrl,
        String checkoutUrl,
        String adDestinationUrl,
        String analyticsVariantParam,
        Long publicationAuditId,
        Long experimentVideoAssetId,
        boolean requiredCollectorsPresent,
        Instant createdAt,
        Instant updatedAt) {
}
