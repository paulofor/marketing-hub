package com.marketinghub.audience.dto;

import com.marketinghub.audience.AudienceSource;
import com.marketinghub.audience.TargetingStatus;
import lombok.Data;

import java.util.List;

/**
 * Request para atualizar targeting estruturado e seu status.
 */
@Data
public class UpdateAudienceTargetingRequest {
    private String targetingSpec;
    private TargetingStatus status;
    private String notes;
    private String lastReviewedBy;
    private AudienceSource source;
    private List<AudienceTargetingSeedRequest> seeds;
}
