package com.marketinghub.productai.dto;

import java.time.Instant;
import java.util.List;

/** Contrato que descreve o funil de coleta de dados para amostra personalizada de Produto IA. */
public record PersonalizedSampleFunnelDto(
        Long experimentId,
        Long leadPortalFlowId,
        String leadPortalFlowSlug,
        boolean approved,
        Instant approvedAt,
        List<String> dataKeys) {
}
