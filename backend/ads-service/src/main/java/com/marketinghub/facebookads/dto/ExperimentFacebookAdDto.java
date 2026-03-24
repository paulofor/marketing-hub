package com.marketinghub.facebookads.dto;

import com.marketinghub.facebookads.FacebookAdStatus;
import java.time.Instant;
import java.util.List;

public record ExperimentFacebookAdDto(
        String id,
        String name,
        FacebookAdStatus status,
        Instant createdAt,
        String trackingCode,
        List<ExperimentFacebookAdFunnelStageDto> funnelStages
) {}
