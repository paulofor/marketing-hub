package com.marketinghub.leadportal.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.leadportal.LeadPortalSimpleFormStyle;
import com.marketinghub.leadportal.LeadPortalSimpleFormStyleDefinition;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Payload sent to the public lead portal when a flow becomes available.
 */
public record LeadPortalFlowPublicationRequest(
        String slug,
        String name,
        String description,
        String customFormHtml,
        String model,
        String prompt,
        String imagePromptModel,
        String imagePromptTemplate,
        Integer imagePromptBatchSize,
        List<Question> questions,
        SimpleFormStylePayload simpleFormStyle) {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern IMAGE_SRC_PATTERN = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE);

    public static LeadPortalFlowPublicationRequest from(LeadPortalFlow flow) {
        LeadPortalSimpleFormStyle style = flow.getSimpleFormStyle();
        String experimentHeroImageUrl = resolveExperimentHeroImageUrl(flow);
        SimpleFormStylePayload stylePayload = style == null ? null : new SimpleFormStylePayload(
                style.getSlug(),
                style.getName(),
                mergeDefinitionWithExperimentImage(style.getDefinition(), experimentHeroImageUrl),
                style.getPreviewImageUrl());
        return new LeadPortalFlowPublicationRequest(
                flow.getSlug(),
                flow.getName(),
                flow.getDescription(),
                flow.getCustomFormHtml(),
                flow.getModel(),
                flow.getPrompt(),
                flow.getImagePromptModel(),
                flow.getImagePromptTemplate(),
                flow.getImagePromptBatchSize(),
                flow.getQuestions().stream()
                        .map(LeadPortalFlowPublicationRequest::toQuestion)
                        .toList(),
                stylePayload);
    }

    private static LeadPortalSimpleFormStyleDefinition mergeDefinitionWithExperimentImage(
            LeadPortalSimpleFormStyleDefinition definition,
            String experimentHeroImageUrl) {
        if (definition == null || !StringUtils.hasText(experimentHeroImageUrl)) {
            return definition;
        }
        return new LeadPortalSimpleFormStyleDefinition(
                definition.backgroundColor(),
                definition.backgroundGradient(),
                definition.backgroundPatternUrl(),
                definition.cardBackground(),
                definition.cardBorderColor(),
                definition.cardShadow(),
                definition.headingColor(),
                definition.textColor(),
                definition.mutedTextColor(),
                definition.primaryColor(),
                definition.accentColor(),
                definition.buttonBackground(),
                definition.buttonTextColor(),
                definition.buttonShadow(),
                definition.buttonBorderRadius(),
                definition.highlightBackground(),
                definition.inputBackground(),
                definition.inputBorderColor(),
                definition.heroLayout(),
                experimentHeroImageUrl,
                definition.heroImageBlendColor());
    }

    private static String resolveExperimentHeroImageUrl(LeadPortalFlow flow) {
        if (flow == null || flow.getExperiment() == null) {
            return null;
        }
        String fromImagePlanning = findImageUrlInJson(flow.getExperiment().getLandingPageImagePlanning());
        if (StringUtils.hasText(fromImagePlanning)) {
            return fromImagePlanning;
        }
        return findImageUrlInHtml(flow.getExperiment().getLandingPageHtml());
    }

    private static String findImageUrlInJson(String payload) {
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(payload);
            return findFirstImageUrl(root);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String findFirstImageUrl(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            for (String key : List.of("imageUrl", "webUrl", "sourceUrl", "url")) {
                JsonNode value = node.get(key);
                if (value != null && value.isTextual() && StringUtils.hasText(value.asText())) {
                    return value.asText();
                }
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                String nested = findFirstImageUrl(fields.next().getValue());
                if (StringUtils.hasText(nested)) {
                    return nested;
                }
            }
            return null;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                String nested = findFirstImageUrl(item);
                if (StringUtils.hasText(nested)) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static String findImageUrlInHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return null;
        }
        Matcher matcher = IMAGE_SRC_PATTERN.matcher(html);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Question toQuestion(LeadPortalFlowQuestion question) {
        return new Question(
                question.getTitle(),
                question.getDataKey(),
                question.getType(),
                question.isRequired(),
                question.getDescription(),
                question.getPlaceholder(),
                List.copyOf(Objects.requireNonNullElse(question.getOptions(), List.of())));
    }

    public record Question(
            String title,
            String dataKey,
            LeadPortalQuestionType type,
            boolean required,
            String description,
            String placeholder,
            List<String> options) {
    }

    public record SimpleFormStylePayload(
            String slug,
            String name,
            LeadPortalSimpleFormStyleDefinition definition,
            String previewImageUrl) {
    }
}
