package com.marketinghub.facebookads.dto;

import com.marketinghub.facebookads.FacebookAdStatus;
import java.time.Instant;
import java.util.List;

public record ExperimentFacebookCampaignDto(
        String id,
        String name,
        String objective,
        FacebookAdStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant metricsLastSyncedAt,
        String metricsLastError,
        List<ExperimentFacebookAdSetDto> adSets,
        List<String> issues
) {}
