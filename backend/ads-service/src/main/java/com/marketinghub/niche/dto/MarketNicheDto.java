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
    private Instant createdAt;
    private Instant updatedAt;
}
