package com.marketinghub.audience.dto;

import com.marketinghub.audience.AudienceSource;
import com.marketinghub.audience.TargetingStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Data transfer object for {@link com.marketinghub.audience.Audience}.
 */
@Data
@Builder
public class AudienceDto {
    private Long id;
    private String name;
    private String description;
    private Long marketNicheId;
    private UUID hypothesisId;
    private String prompt;
    private String model;
    private boolean approved;
    private String targetingSpec;
    private TargetingStatus targetingStatus;
    private String targetingNotes;
    private AudienceSource source;
    private String lastReviewedBy;
    private List<AudienceTargetingSeedDto> seeds;
}
