package com.marketinghub.leadportal.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJob;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJobStatus;
import com.marketinghub.repository.jpa.experiment.frameworkimage.FrameworkImageGenerationJobRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves the best hero image URL available for a given experiment, priorizando
 * os assets produzidos pelo framework-image pipeline.
 */
@Component
public class ExperimentHeroImageResolver {

    private static final Logger log = LoggerFactory.getLogger(ExperimentHeroImageResolver.class);
    private static final List<String> DEFAULT_HERO_KEYS = List.of("s0-hero", "hero", "item-1");

    private final FrameworkImageGenerationJobRepository jobRepository;
    private final ObjectMapper objectMapper;

    public ExperimentHeroImageResolver(FrameworkImageGenerationJobRepository jobRepository,
                                       ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<String> resolve(Experiment experiment) {
        if (experiment == null || experiment.getId() == null) {
            return Optional.empty();
        }

        List<String> heroKeys = collectHeroKeys(experiment.getLandingPageImagePlanning());
        if (heroKeys.isEmpty()) {
            return Optional.empty();
        }

        List<FrameworkImageGenerationJob> jobs = jobRepository
                .findByExperimentIdAndPlanningItemKeyInAndStatusOrderByCreatedAtDesc(
                        experiment.getId(), heroKeys, FrameworkImageGenerationJobStatus.COMPLETED);

        return jobs.stream()
                .map(job -> firstNonBlank(job.getWebUrl(), job.getSourceUrl()))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .findFirst();
    }

    private List<String> collectHeroKeys(String rawPlanning) {
        Set<String> keys = new LinkedHashSet<>(extractHeroKeysFromPlanning(rawPlanning));
        keys.addAll(DEFAULT_HERO_KEYS);
        return keys.stream().filter(StringUtils::hasText).toList();
    }

    private List<String> extractHeroKeysFromPlanning(String rawPlanning) {
        if (!StringUtils.hasText(rawPlanning)) {
            return List.of();
        }
        try {
            JsonNode planningNode = resolvePlanningNode(objectMapper.readTree(rawPlanning));
            JsonNode imagesNode = planningNode.path("images");
            if (!imagesNode.isArray() || imagesNode.isEmpty()) {
                return List.of();
            }

            List<String> heroKeys = new ArrayList<>();
            for (int index = 0; index < imagesNode.size(); index++) {
                JsonNode imageNode = imagesNode.get(index);
                if (imageNode == null || !imageNode.isObject()) {
                    continue;
                }
                if (isHeroPlacement(imageNode)) {
                    heroKeys.add(normalizePlanningItemKey(imageNode.path("sectionId").asText(null), index));
                }
            }
            return heroKeys;
        } catch (Exception ex) {
            log.warn("Failed to parse landing page image planning while resolving hero image", ex);
            return List.of();
        }
    }

    private JsonNode resolvePlanningNode(JsonNode rootNode) {
        if (rootNode == null || !rootNode.isObject()) {
            return objectMapper.createObjectNode();
        }
        JsonNode landingPageImagePlanning = rootNode.path("landingPageImagePlanning");
        if (landingPageImagePlanning.isObject()) {
            return landingPageImagePlanning;
        }
        JsonNode imagePlan = rootNode.path("imagePlan");
        if (imagePlan.isObject()) {
            return imagePlan;
        }
        JsonNode artifactContent = rootNode.path("artifact").path("content");
        if (artifactContent.isObject()) {
            return artifactContent;
        }
        return rootNode;
    }

    private boolean isHeroPlacement(JsonNode imageNode) {
        return equalsIgnoreCase(imageNode.path("placement").asText(null), "hero")
                || containsHeroToken(imageNode.path("sectionId").asText(null))
                || containsHeroToken(imageNode.path("sectionName").asText(null));
    }

    private boolean containsHeroToken(String value) {
        return value != null && value.toLowerCase().contains("hero");
    }

    private String normalizePlanningItemKey(String value, int index) {
        String trimmed = value != null ? value.trim() : null;
        if (StringUtils.hasText(trimmed)) {
            return trimmed;
        }
        return "item-" + (index + 1);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean equalsIgnoreCase(String value, String expected) {
        return value != null && expected != null && value.equalsIgnoreCase(expected);
    }
}
