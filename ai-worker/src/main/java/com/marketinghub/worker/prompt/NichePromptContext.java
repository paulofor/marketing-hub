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
