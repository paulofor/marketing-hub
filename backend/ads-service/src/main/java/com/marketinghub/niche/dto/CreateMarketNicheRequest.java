package com.marketinghub.niche.dto;

import lombok.Data;

/**
 * Request body for creating a market niche.
 */
@Data
public class CreateMarketNicheRequest {
    private String name;
    private String description;
    private String interestCategory;
    private String roleCategory;
    private String demandVolume;
    private String promises;
    private String offers;

    private String baseSegmentation;
    private String interests;
    private String demographicFilters;
    private String extraTips;
    private Integer hypothesesToGenerate;
    private Integer audiencesToGenerate;
    private Long chatDialogId;
}
