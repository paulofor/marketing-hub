package com.marketinghub.niche.dto;

import lombok.Data;
import java.math.BigDecimal;

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
    private BigDecimal cost;
    private BigDecimal expense;
    private BigDecimal totalCost;
    private BigDecimal totalRevenue;

    private String baseSegmentation;
    private String interests;
    private String demographicFilters;
    private String extraTips;
    private Integer hypothesesToGenerate;
    private Integer interestsToGenerate;
    private Integer jobTitlesToGenerate;
    private Integer behaviorsToGenerate;
    private Integer detailedDescriptionsToGenerate;
    private String hypothesisModel;
    private String interestModel;
    private String jobTitleModel;
    private String behaviorModel;
    private String detailedDescriptionModel;
    private Long differentiatedTechnologyId;
    private Long chatDialogId;
}
