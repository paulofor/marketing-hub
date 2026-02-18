package com.marketinghub.facebookads.dto;

import com.marketinghub.facebookads.FacebookAdStatus;
import java.time.Instant;
import java.util.List;

public record ExperimentFacebookAdSetDto(
        String id,
        String name,
        FacebookAdStatus status,
        Instant createdAt,
        Long experimentAdSetId,
        List<ExperimentFacebookAdDto> ads,
        List<String> issues
) {}
