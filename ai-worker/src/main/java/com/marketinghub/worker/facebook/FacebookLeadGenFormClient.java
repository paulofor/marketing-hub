package com.marketinghub.worker.facebook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.instantform.InstantFormChatGptClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal Graph API client to create and activate Instant Forms (lead gen forms) for experiments.
 */
@Component
public class FacebookLeadGenFormClient {
    private static final Logger log = LoggerFactory.getLogger(FacebookLeadGenFormClient.class);

    private final WebClient graphClient;
    private final ObjectMapper objectMapper;
    private final String apiVersion;

    public FacebookLeadGenFormClient(
            WebClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${facebook.graph-api.base-url:https://graph.facebook.com}") String baseUrl,
            @Value("${facebook.graph-api.version:v23.0}") String apiVersion
    ) {
        this.graphClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiVersion = apiVersion.startsWith("v") ? apiVersion : "v" + apiVersion;
    }

    public LeadGenForm createAndActivateLeadGenForm(String accessToken, String pageId, InstantFormChatGptClient.InstantFormPlan plan) {
        Objects.requireNonNull(accessToken, "accessToken");
        Objects.requireNonNull(pageId, "pageId");
        Objects.requireNonNull(plan, "plan");

        JsonNode creationResponse = executeCreate(accessToken, pageId, plan);
        if (creationResponse == null) {
            return null;
        }
        String formId = creationResponse.path("id").asText(null);
        if (!StringUtils.hasText(formId)) {
            log.error("Facebook returned empty form id while creating leadgen form for page {}", pageId);
            return null;
        }
        activateForm(accessToken, formId);
        JsonNode details = fetchForm(accessToken, formId);
        return new LeadGenForm(
                formId,
                details != null ? details.path("status").asText(null) : null,
                parseInstant(details != null ? details.path("created_time").asText(null) : null),
                parseInstant(details != null ? details.path("updated_time").asText(null) : null),
                details != null && details.hasNonNull("leads_count") ? details.path("leads_count").asLong() : null
        );
    }

    private JsonNode executeCreate(String accessToken, String pageId, InstantFormChatGptClient.InstantFormPlan plan) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", sanitize(plan.name()));
        String locale = sanitize(plan.locale());
        if (StringUtils.hasText(locale)) {
            payload.put("locale", locale);
        }
        String followUp = sanitize(plan.followUpActionUrl());
        if (StringUtils.hasText(followUp)) {
            payload.put("follow_up_action_url", followUp);
        }
        String privacyPolicyUrl = sanitize(plan.privacyPolicyUrl());
        if (StringUtils.hasText(privacyPolicyUrl)) {
            Map<String, Object> privacyPolicy = new LinkedHashMap<>();
            privacyPolicy.put("url", privacyPolicyUrl);
            privacyPolicy.put("link_text", "Política de Privacidade");
            payload.put("privacy_policy", privacyPolicy);
        }
        List<Map<String, Object>> questions = buildQuestions(plan.questions());
        if (!questions.isEmpty()) {
            payload.put("questions", questions);
        }
        Map<String, Object> thankYouPage = new LinkedHashMap<>();
        thankYouPage.put("title", "Obrigado!");
        String leadMagnet = sanitize(plan.leadMagnet());
        String valueProposition = sanitize(plan.valueProposition());
        if (StringUtils.hasText(leadMagnet)) {
            thankYouPage.put("body", leadMagnet);
        } else if (StringUtils.hasText(valueProposition)) {
            thankYouPage.put("body", valueProposition);
        }
        if (!thankYouPage.isEmpty()) {
            payload.put("thank_you_page", thankYouPage);
        }
        payload.put("is_optimized_for_quality", true);
        payload.put("access_token", accessToken);

        String path = "/" + apiVersion + "/" + pageId + "/leadgen_forms";
        return executePost(path, payload);
    }

    private void activateForm(String accessToken, String formId) {
        Map<String, Object> payload = Map.of(
                "status", "ACTIVE",
                "access_token", accessToken
        );
        String path = "/" + apiVersion + "/" + formId;
        executePost(path, payload);
    }

    private JsonNode fetchForm(String accessToken, String formId) {
        String path = "/" + apiVersion + "/" + formId + "?fields=id,status,leads_count,created_time,updated_time&access_token=" + accessToken;
        return executeGet(path);
    }

    private JsonNode executePost(String path, Map<String, Object> payload) {
        Map<String, Object> sanitized = sanitizePayload(payload);
        String maskedPath = maskAccessTokenInPath(path);
        log.info("POSTing to Facebook Graph API: path==>{}, payload={} ", maskedPath, sanitized);
        try {
            return graphClient.post()
                    .uri(path)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .defaultIfEmpty(objectMapper.nullNode())
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("Facebook Graph API POST failed: path<=={}, status={}, response={}", maskedPath, ex.getRawStatusCode(), maskAccessToken(ex.getResponseBodyAsString()), ex);
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST || ex.getStatusCode() == HttpStatus.UNAUTHORIZED || ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                return null;
            }
            throw ex;
        } catch (WebClientRequestException ex) {
            log.error("Facebook Graph API POST request error: path==>{}, message={} ", maskedPath, ex.getMessage(), ex);
            return null;
        }
    }

    private JsonNode executeGet(String path) {
        String maskedPath = maskAccessTokenInPath(path);
        log.info("GETting from Facebook Graph API: path==>{}", maskedPath);
        try {
            return graphClient.get()
                    .uri(path)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .defaultIfEmpty(objectMapper.nullNode())
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("Facebook Graph API GET failed: path<=={}, status={}, response={}", maskedPath, ex.getRawStatusCode(), maskAccessToken(ex.getResponseBodyAsString()), ex);
        } catch (WebClientRequestException ex) {
            log.error("Facebook Graph API GET request error: path==>{}, message={}", maskedPath, ex.getMessage(), ex);
        }
        return null;
    }

    private Map<String, Object> sanitizePayload(Map<String, Object> payload) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        payload.forEach((key, value) -> {
            if ("access_token".equals(key) && value instanceof String token) {
                sanitized.put(key, maskAccessToken(token));
            } else {
                sanitized.put(key, value);
            }
        });
        return sanitized;
    }

    private String maskAccessToken(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        int visible = Math.min(6, value.length());
        return value.substring(0, visible) + "***";
    }

    private String maskAccessTokenInPath(String path) {
        if (!StringUtils.hasText(path) || !path.contains("access_token=")) {
            return path;
        }
        int start = path.indexOf("access_token=") + "access_token=".length();
        int end = path.indexOf('&', start);
        if (end == -1) {
            end = path.length();
        }
        String token = path.substring(start, end);
        return path.replace(token, maskAccessToken(token));
    }

    private List<Map<String, Object>> buildQuestions(List<InstantFormChatGptClient.InstantFormPlan.Question> questions) {
        List<Map<String, Object>> result = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(1);
        result.add(Map.of("type", "FULL_NAME"));
        result.add(Map.of("type", "EMAIL"));
        result.add(Map.of("type", "PHONE_NUMBER"));
        if (questions == null || questions.isEmpty()) {
            return result;
        }
        for (InstantFormChatGptClient.InstantFormPlan.Question question : questions) {
            if (question == null || !StringUtils.hasText(question.label())) {
                continue;
            }
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("type", "CUSTOM");
            node.put("label", sanitize(question.label()));
            node.put("key", "custom_question_" + counter.getAndIncrement());
            if (question.options() != null && !question.options().isEmpty()) {
                List<Map<String, String>> options = new ArrayList<>();
                int optionIndex = 1;
                for (String option : question.options()) {
                    String sanitizedOption = sanitize(option);
                    if (StringUtils.hasText(sanitizedOption)) {
                        Map<String, String> optionNode = new LinkedHashMap<>();
                        optionNode.put("key", "option_" + optionIndex++);
                        optionNode.put("value", sanitizedOption);
                        options.add(optionNode);
                    }
                }
                if (!options.isEmpty()) {
                    node.put("options", options);
                }
            }
            String helpText = sanitize(question.helpText());
            if (StringUtils.hasText(helpText)) {
                node.put("description", helpText);
            }
            result.add(node);
        }
        return result;
    }

    private Instant parseInstant(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            log.warn("Could not parse instant from value returned by Facebook: {}", value, ex);
            return null;
        }
    }

    public record LeadGenForm(
            String id,
            String status,
            Instant createdTime,
            Instant updatedTime,
            Long leadsCount
    ) {
    }

    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
