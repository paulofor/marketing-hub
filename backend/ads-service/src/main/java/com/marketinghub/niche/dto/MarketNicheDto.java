package com.marketinghub.niche.dto;

import java.time.Instant;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Data transfer object for {@link com.marketinghub.niche.MarketNiche}.
 */
@Data
public class MarketNicheDto {
    private Long id;
    private String name;
    private String description;
    private String interestCategory;
    private String roleCategory;
    private String demandVolume;
    private String promises;
    private String offers;
    private BigDecimal cost;
    private BigDecimal expense;
    private String baseSegmentation;
    private String interests;
    private String demographicFilters;
    private String extraTips;
    private Integer hypothesesToGenerate;
    private Integer audiencesToGenerate;
    private String hypothesisModel;
    private Long differentiatedTechnologyId;
    private Long chatDialogId;
    private Instant createdAt;
    private Instant updatedAt;
}
