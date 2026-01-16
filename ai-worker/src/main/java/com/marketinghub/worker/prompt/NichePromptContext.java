package com.marketinghub.worker.prompt;

import com.marketinghub.differentiatedtechnology.DifferentiatedTechnology;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.description.NicheDetailedDescription;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public record NichePromptContext(Long id,
                                 String name,
                                 String description,
                                 String baseSegmentation,
                                 String interests,
                                 String demographicFilters,
                                 String extraTips,
                                 String interestCategory,
                                 String roleCategory,
                                 List<Map<String, Object>> detailedDescriptions,
                                 Map<String, Object> latestDetailedDescription,
                                 Map<String, Object> hypothesisDetailedDescription,
                                 Map<String, Object> differentiatedTechnology) {

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
        map.put("detailedDescriptions", detailedDescriptions);
        map.put("latestDetailedDescription", latestDetailedDescription);
        map.put("hypothesisDetailedDescription", hypothesisDetailedDescription);
        map.put("differentiatedTechnology", differentiatedTechnology);
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

    public List<Map<String, Object>> getDetailedDescriptions() {
        return detailedDescriptions;
    }

    public Map<String, Object> getLatestDetailedDescription() {
        return latestDetailedDescription;
    }

    public Map<String, Object> getHypothesisDetailedDescription() {
        return hypothesisDetailedDescription;
    }

    public Map<String, Object> getDifferentiatedTechnology() {
        return differentiatedTechnology;
    }

    public static NichePromptContext from(MarketNiche niche) {
        return from(niche, List.of());
    }

    public static NichePromptContext from(MarketNiche niche, List<NicheDetailedDescription> detailedDescriptions) {
        if (niche == null) {
            return null;
        }
        List<Map<String, Object>> descriptionContext = Optional.ofNullable(detailedDescriptions)
                .orElse(List.of())
                .stream()
                .sorted(java.util.Comparator
                        .comparing(NicheDetailedDescription::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                        .thenComparing(NicheDetailedDescription::getId, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(NichePromptContext::mapDetailedDescription)
                .collect(Collectors.toList());
        Map<String, Object> latestDetailedDescription = descriptionContext.isEmpty()
                ? null
                : descriptionContext.get(descriptionContext.size() - 1);
        Map<String, Object> selectedDetailedDescription = Optional.ofNullable(niche.getHypothesisDetailedDescription())
                .map(NichePromptContext::mapDetailedDescription)
                .orElse(null);
        Map<String, Object> differentiatedTechnology = Optional.ofNullable(niche.getDifferentiatedTechnology())
                .map(NichePromptContext::mapDifferentiatedTechnology)
                .orElse(null);
        return new NichePromptContext(
                niche.getId(),
                niche.getName(),
                niche.getDescription(),
                niche.getBaseSegmentation(),
                niche.getInterests(),
                niche.getDemographicFilters(),
                niche.getExtraTips(),
                niche.getInterestCategory(),
                niche.getRoleCategory(),
                descriptionContext,
                latestDetailedDescription,
                selectedDetailedDescription,
                differentiatedTechnology
        );
    }

    private static Map<String, Object> mapDetailedDescription(NicheDetailedDescription description) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", description.getId());
        map.put("title", description.getTitle());
        map.put("description", description.getDescription());
        map.put("pains", description.getPains());
        map.put("desires", description.getDesires());
        map.put("needs", description.getNeeds());
        map.put("model", description.getModel());
        map.put("prompt", description.getPrompt());
        map.put("createdAt", description.getCreatedAt());
        map.put("updatedAt", description.getUpdatedAt());
        return map;
    }

    private static Map<String, Object> mapDifferentiatedTechnology(DifferentiatedTechnology technology) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", technology.getId());
        map.put("name", technology.getName());
        map.put("description", technology.getDescription());
        map.put("promptText", technology.getPromptText());
        map.put("createdAt", technology.getCreatedAt());
        map.put("updatedAt", technology.getUpdatedAt());
        return map;
    }
}
