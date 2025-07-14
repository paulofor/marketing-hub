package com.marketinghub.niche.dto;

import lombok.Data;

/**
 * Request body for creating a market niche.
 */
@Data
public class CreateMarketNicheRequest {
    private String name;
    private String description;
    private String demandVolume;
    private String promises;
    private String offers;

    private String baseSegmentation;
    private String interests;
    private String demographicFilters;
    private String extraTips;
}
