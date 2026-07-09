package com.marketinghub.experiment.salespageab.dto;

import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbTestStatus;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: expor o plano de teste A/B de pagina de venda para UI e workers. */
public record ExperimentSalesPageAbTestDto(
        Long id,
        Long experimentId,
        String name,
        ExperimentSalesPageAbTestStatus status,
        String hypothesis,
        String primaryMetric,
        String secondaryMetrics,
        String winnerRule,
        Integer minimumRuntimeDays,
        Integer minimumSampleSize,
        boolean metaSplitTestRecommended,
        String notes,
        List<ExperimentSalesPageAbVariantDto> variants,
        Instant createdAt,
        Instant updatedAt) {
}
