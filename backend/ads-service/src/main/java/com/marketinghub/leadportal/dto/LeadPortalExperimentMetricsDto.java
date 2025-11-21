package com.marketinghub.leadportal.dto;

import java.util.List;

/**
 * Resume métricas básicas do fluxo de portal por experimento.
 */
public record LeadPortalExperimentMetricsDto(
        Long experimentId,
        String experimentName,
        long leadsAccessed,
        long leadsWithImage,
        List<LeadPortalExperimentUserDto> uniqueLeads) {
}
