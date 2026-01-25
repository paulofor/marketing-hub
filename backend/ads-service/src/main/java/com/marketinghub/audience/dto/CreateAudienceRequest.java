package com.marketinghub.audience.dto;

import com.marketinghub.audience.AudienceSource;
import lombok.Data;

import java.util.UUID;

/**
 * Request body for creating an audience.
 */
@Data
public class CreateAudienceRequest {
    private String name;
    private String description;
    private Long marketNicheId;
    private UUID hypothesisId;
    private String prompt;
    private String model;
    private AudienceSource source;
}
