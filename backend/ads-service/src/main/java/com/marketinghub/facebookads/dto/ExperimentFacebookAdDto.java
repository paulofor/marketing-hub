package com.marketinghub.facebookads.dto;

import com.marketinghub.facebookads.FacebookAdStatus;
import java.time.Instant;

public record ExperimentFacebookAdDto(
        String id,
        String name,
        FacebookAdStatus status,
        Instant createdAt
) {}
