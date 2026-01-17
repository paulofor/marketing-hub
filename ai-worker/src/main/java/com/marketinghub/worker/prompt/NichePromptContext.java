package com.marketinghub.worker.prompt;

import com.marketinghub.differentiatedtechnology.DifferentiatedTechnology;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.description.NicheDetailedDescription;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.proxy.HibernateProxy;

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
        NicheDetailedDescription selectedHypothesisDescription = selectHypothesisDetailedDescription(
                resolveHypothesisDetailedDescription(niche),
                detailedDescriptions);
        Map<String, Object> selectedDetailedDescription = Optional.ofNullable(selectedHypothesisDescription)
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

    private static NicheDetailedDescription selectHypothesisDetailedDescription(
            NicheDetailedDescription hypothesis,
            List<NicheDetailedDescription> detailedDescriptions) {
        if (hypothesis == null) {
            return null;
        }
        Long hypothesisId = resolveIdentifier(hypothesis);
        if (hypothesisId != null && detailedDescriptions != null) {
            for (NicheDetailedDescription description : detailedDescriptions) {
                if (hypothesisId.equals(resolveIdentifier(description))) {
                    return description;
                }
            }
        }
        return hypothesis;
    }

    private static Map<String, Object> mapDetailedDescription(NicheDetailedDescription description) {
        if (description == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", resolveIdentifier(description));
        if (!Hibernate.isInitialized(description)) {
            try {
                Hibernate.initialize(description);
            } catch (LazyInitializationException ignored) {
                return map;
            }
        }
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

    private static NicheDetailedDescription resolveHypothesisDetailedDescription(MarketNiche niche) {
        if (niche == null) {
            return null;
        }
        try {
            java.lang.reflect.Method method = niche.getClass().getMethod("getHypothesisDetailedDescription");
            Object response = method.invoke(niche);
            if (response instanceof NicheDetailedDescription description) {
                return description;
            }
        } catch (NoSuchMethodException ignored) {
            // Older ads-service versions do not provide hypothesisDetailedDescription.
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static Map<String, Object> mapDifferentiatedTechnology(DifferentiatedTechnology technology) {
        if (technology == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        if (!Hibernate.isInitialized(technology)) {
            try {
                Hibernate.initialize(technology);
            } catch (LazyInitializationException ignored) {
                map.put("id", resolveIdentifier(technology));
                return map;
            }
        }
        map.put("id", technology.getId());
        map.put("name", technology.getName());
        map.put("description", technology.getDescription());
        map.put("promptText", technology.getPromptText());
        map.put("createdAt", technology.getCreatedAt());
        map.put("updatedAt", technology.getUpdatedAt());
        return map;
    }

    private static Long resolveIdentifier(DifferentiatedTechnology technology) {
        if (technology instanceof HibernateProxy proxy) {
            Object id = proxy.getHibernateLazyInitializer().getIdentifier();
            if (id instanceof Long value) {
                return value;
            }
        }
        return technology.getId();
    }

    private static Long resolveIdentifier(NicheDetailedDescription description) {
        if (description == null) {
            return null;
        }
        if (description instanceof HibernateProxy proxy) {
            Object id = proxy.getHibernateLazyInitializer().getIdentifier();
            if (id instanceof Long value) {
                return value;
            }
        }
        return description.getId();
    }
}
