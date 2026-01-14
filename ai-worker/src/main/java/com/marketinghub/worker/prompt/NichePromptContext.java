package com.marketinghub.worker.prompt;

import com.marketinghub.niche.MarketNiche;

public record NichePromptContext(Long id,
                                 String name,
                                 String description,
                                 String baseSegmentation,
                                 String interests,
                                 String demographicFilters,
                                 String extraTips,
                                 String interestCategory,
                                 String roleCategory) {
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getBaseSegmentation() {
        return baseSegmentation;
    }

    public String getInterests() {
        return interests;
    }

    public String getDemographicFilters() {
        return demographicFilters;
    }

    public String getExtraTips() {
        return extraTips;
    }

    public String getInterestCategory() {
        return interestCategory;
    }

    public String getRoleCategory() {
        return roleCategory;
    }

    public static NichePromptContext from(MarketNiche niche) {
        if (niche == null) {
            return null;
        }
        return new NichePromptContext(
                niche.getId(),
                niche.getName(),
                niche.getDescription(),
                niche.getBaseSegmentation(),
                niche.getInterests(),
                niche.getDemographicFilters(),
                niche.getExtraTips(),
                niche.getInterestCategory(),
                niche.getRoleCategory()
        );
    }
}
