package com.marketinghub.leadportal.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.leadportal.LeadPortalSimpleFormStyle;
import com.marketinghub.leadportal.LeadPortalSimpleFormStyleDefinition;
import org.springframework.util.StringUtils;

import java.time.Instant;
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
        String legacyPreviewHtml,
        String renderMode,
        FormSpec formSpec,
        EngagementContractMetadata engagementContract,
        String model,
        String prompt,
        String imagePromptModel,
        String imagePromptTemplate,
        Integer imagePromptBatchSize,
        List<Question> questions,
        SimpleFormStylePayload simpleFormStyle,
        String facebookPixelId,
        String facebookPixelCode,
        java.time.Instant facebookPixelCreatedAt) {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern IMAGE_SRC_PATTERN = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PIXEL_MARKER_PATTERN = Pattern.compile(
            "fbq\\s*\\(|fbevents\\.js|connect\\.facebook\\.net",
            Pattern.CASE_INSENSITIVE);

    public static LeadPortalFlowPublicationRequest from(LeadPortalFlow flow) {
        return from(flow, null);
    }

    public static LeadPortalFlowPublicationRequest from(LeadPortalFlow flow, String heroImageOverride) {
        LeadPortalSimpleFormStyle style = flow.getSimpleFormStyle();
        String experimentHeroImageUrl = StringUtils.hasText(heroImageOverride)
                ? heroImageOverride
                : resolveExperimentHeroImageUrl(flow);
        SimpleFormStylePayload stylePayload = style == null ? null : new SimpleFormStylePayload(
                style.getSlug(),
                style.getName(),
                mergeDefinitionWithExperimentImage(style.getDefinition(), experimentHeroImageUrl),
                style.getPreviewImageUrl());
        String facebookPixelId = flow.getMarketNiche() != null ? flow.getMarketNiche().getFacebookPixelId() : null;
        String facebookPixelCode = flow.getMarketNiche() != null ? flow.getMarketNiche().getFacebookPixelCode() : null;
        Instant facebookPixelCreatedAt = flow.getMarketNiche() != null ? flow.getMarketNiche().getFacebookPixelCreatedAt() : null;
        String htmlWithPixel = injectFacebookPixelWhenEligible(flow, flow.getCustomFormHtml(), facebookPixelId);
        return new LeadPortalFlowPublicationRequest(
                flow.getSlug(),
                flow.getName(),
                flow.getDescription(),
                htmlWithPixel,
                htmlWithPixel,
                resolveRenderMode(flow),
                new FormSpec(flow.getQuestions().stream()
                        .map(LeadPortalFlowPublicationRequest::toQuestion)
                        .toList()),
                EngagementContractMetadata.defaultV1(),
                flow.getModel(),
                flow.getPrompt(),
                flow.getImagePromptModel(),
                flow.getImagePromptTemplate(),
                flow.getImagePromptBatchSize(),
                flow.getQuestions().stream().map(LeadPortalFlowPublicationRequest::toQuestion).toList(),
                stylePayload,
                facebookPixelId,
                facebookPixelCode,
                facebookPixelCreatedAt);
    }

    private static String injectFacebookPixelWhenEligible(LeadPortalFlow flow, String customFormHtml, String facebookPixelId) {
        if (!isEligibleForNichePixelInjection(flow)) {
            return customFormHtml;
        }
        if (!StringUtils.hasText(customFormHtml) || !StringUtils.hasText(facebookPixelId)) {
            return customFormHtml;
        }
        if (PIXEL_MARKER_PATTERN.matcher(customFormHtml).find()) {
            return customFormHtml;
        }
        String pixelSnippet = buildPixelSnippet(facebookPixelId);
        if (containsIgnoreCase(customFormHtml, "</head>")) {
            return replaceFirstIgnoreCase(customFormHtml, "</head>", pixelSnippet + "\n</head>");
        }
        if (containsIgnoreCase(customFormHtml, "<body")) {
            return replaceFirstIgnoreCase(customFormHtml, "<body", pixelSnippet + "\n<body");
        }
        return pixelSnippet + "\n" + customFormHtml;
    }

    private static boolean isEligibleForNichePixelInjection(LeadPortalFlow flow) {
        return flow != null
                && flow.isApproved()
                && flow.getExperiment() != null;
    }

    private static String buildPixelSnippet(String facebookPixelId) {
        String escapedPixelId = escapeEcmaScript(facebookPixelId.trim());
        return """
                <!-- Meta Pixel Code -->
                <script>
                  !function(f,b,e,v,n,t,s){if(f.fbq)return;n=f.fbq=function(){n.callMethod?
                  n.callMethod.apply(n,arguments):n.queue.push(arguments)};if(!f._fbq)f._fbq=n;
                  n.push=n;n.loaded=!0;n.version='2.0';n.queue=[];t=b.createElement(e);t.async=!0;
                  t.src=v;s=b.getElementsByTagName(e)[0];s.parentNode.insertBefore(t,s)}(window, document,'script',
                  'https://connect.facebook.net/en_US/fbevents.js');
                  fbq('init', '%s');
                  fbq('track', 'PageView');
                </script>
                <noscript><img height="1" width="1" style="display:none"
                  src="https://www.facebook.com/tr?id=%s&ev=PageView&noscript=1"
                /></noscript>
                <!-- End Meta Pixel Code -->
                """.formatted(escapedPixelId, escapedPixelId).trim();
    }

    private static String escapeEcmaScript(String rawValue) {
        return rawValue
                .replace("\\", "\\\\")
                .replace("'", "\\'");
    }

    private static boolean containsIgnoreCase(String source, String token) {
        return source.toLowerCase().contains(token.toLowerCase());
    }

    private static String replaceFirstIgnoreCase(String source, String target, String replacement) {
        return source.replaceFirst("(?i)" + Pattern.quote(target), Matcher.quoteReplacement(replacement));
    }

    private static String resolveRenderMode(LeadPortalFlow flow) {
        if (flow.isSchemaFirst()) {
            return "schema-first";
        }
        return flow.getQuestions() == null || flow.getQuestions().isEmpty() ? "legacy-html" : "fallback";
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

    public record FormSpec(List<Question> questions) {
    }

    public record SimpleFormStylePayload(
            String slug,
            String name,
            LeadPortalSimpleFormStyleDefinition definition,
            String previewImageUrl) {
    }

    public record EngagementContractMetadata(
            String version,
            String renderCompleteEndpoint,
            String submissionEndpoint,
            String idempotencyField,
            List<String> submissionRequiredFields) {

        private static final String RENDER_ENDPOINT = "/api/public/lead-portal/flows/{slug}/render-complete";
        private static final String SUBMISSION_ENDPOINT = "/api/public/lead-portal/flows/{slug}/submission";

        public static EngagementContractMetadata defaultV1() {
            return new EngagementContractMetadata(
                    "lead-portal-submission-engagement.v1",
                    RENDER_ENDPOINT,
                    SUBMISSION_ENDPOINT,
                    "submissionId",
                    List.of("slug", "submissionId", "submittedAt", "contato"));
        }
    }
}
