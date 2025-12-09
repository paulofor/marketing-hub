package com.marketinghub.leadportal.dto;

import java.time.Instant;
import java.util.List;

/**
 * Resume métricas básicas do fluxo de portal por experimento.
 */
public record LeadPortalExperimentMetricsDto(
        Long experimentId,
        String experimentName,
        long leadsAccessed,
        long leadsWithImage,
        List<LeadPortalExperimentUserDto> uniqueLeads,
        long sampleEmailsGenerated,
        Long selectedSampleEmailId,
        String selectedSampleEmailSubject,
        String selectedSampleEmailPreviewText,
        String selectedSampleEmailCallToAction,
        Instant selectedSampleEmailUpdatedAt,
        long packagesWithWatermark,
        long packagesNotified,
        Instant lastPackageNotificationAt) {
}
