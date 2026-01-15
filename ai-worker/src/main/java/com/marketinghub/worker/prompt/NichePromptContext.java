package com.marketinghub.worker.prompt;

import com.marketinghub.niche.MarketNiche;

import java.util.LinkedHashMap;
import java.util.Map;

public record NichePromptContext(Long id,
                                 String name,
                                 String description,
                                 String baseSegmentation,
                                 String interests,
                                 String demographicFilters,
                                 String extraTips,
                                 String interestCategory,
                                 String roleCategory) {

    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("description", description);
        map.put("baseSegmentation", baseSegmentation);
        map.put("interests", interests);
        map.put("demographicFilters", demographicFilters);
        map.put("extraTips", extraTips);
        map.put("interestCategory", interestCategory);
        map.put("roleCategory", roleCategory);
        return map;
    }

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
