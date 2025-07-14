package com.marketinghub.niche.dto;

import java.time.Instant;
import lombok.Data;

/**
 * Data transfer object for {@link com.marketinghub.niche.MarketNiche}.
 */
@Data
public class MarketNicheDto {
    private Long id;
    private String name;
    private String description;
    private String demandVolume;
    private String promises;
    private String offers;
    private String baseSegmentation;
    private String interests;
    private String demographicFilters;
    private String extraTips;
    private Instant createdAt;
    private Instant updatedAt;
}
