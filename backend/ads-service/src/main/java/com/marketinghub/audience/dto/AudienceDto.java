package com.marketinghub.audience.dto;

import lombok.Builder;
import lombok.Data;

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
}
