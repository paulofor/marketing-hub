package com.marketinghub.facebookadsworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FacebookAdsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookAdsService.class);

    private final WebClient webClient;
    private final String accessToken;
    private final String apiVersion;
    private final ObjectMapper objectMapper;

    public FacebookAdsService(WebClient.Builder builder,
                              @Value("${facebook.graph-api.base-url:https://graph.facebook.com}") String baseUrl,
                              @Value("${facebook.access-token}") String accessToken,
                              @Value("${facebook.graph-api.version:v23.0}") String apiVersion,
                              ObjectMapper objectMapper) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.accessToken = accessToken;
        this.apiVersion = normalizeVersion(apiVersion);
        this.objectMapper = objectMapper;
        LOGGER.info("Configured Facebook Graph API version: {}", this.apiVersion);
    }

    public String createInstagramCampaign(String adAccountId, String name) {
        String path = buildVersionedPath("/act_" + adAccountId + "/campaigns");
        Map<String, Object> body = Map.of(
            "name", name,
            "objective", "OUTCOME_TRAFFIC",
            "status", "PAUSED",
            "special_ad_categories", List.of(),
            "access_token", accessToken
        );

        JsonNode response = executePost(path, body);
        return response.path("id").asText();
    }

    public String createCampaign(String adAccountId, String name) {
        return createInstagramCampaign(adAccountId, name);
    }

    public String createAdSet(String adAccountId, AdSetRequest request) {
        Objects.requireNonNull(request, "request");

        Map<String, Object> targeting = Map.of(
            "geo_locations", Map.of("countries", List.of(request.targetCountry()))
        );

        Map<String, Object> body = new HashMap<>();
        body.put("name", request.name());
        body.put("campaign_id", request.campaignId());
        body.put("daily_budget", request.dailyBudget());
        body.put("billing_event", request.billingEvent());
        body.put("optimization_goal", request.optimizationGoal());
        body.put("status", "PAUSED");
        body.put("destination_type", request.destinationType());
        body.put("targeting", targeting);
        if (request.pageId() != null && !request.pageId().isBlank()) {
            body.put("promoted_object", Map.of("page_id", request.pageId()));
        }
        body.put("access_token", accessToken);

        String path = buildVersionedPath("/act_" + adAccountId + "/adsets");

        JsonNode response = executePost(path, body);
        return response.path("id").asText();
    }

    public String createAdCreative(String adAccountId, AdCreativeRequest request) {
        Objects.requireNonNull(request, "request");

        Map<String, Object> linkData = new HashMap<>();
        linkData.put("link", request.websiteUrl());
        linkData.put("message", request.message());
        linkData.put("call_to_action", Map.of(
            "type", request.callToActionType(),
            "value", Map.of("link", request.websiteUrl())
        ));

        Map<String, Object> objectStorySpec = new HashMap<>();
        objectStorySpec.put("page_id", request.pageId());
        if (request.instagramActorId() != null && !request.instagramActorId().isBlank()) {
            objectStorySpec.put("instagram_actor_id", request.instagramActorId());
        }
        objectStorySpec.put("link_data", linkData);

        Map<String, Object> body = new HashMap<>();
        body.put("name", request.name());
        body.put("object_story_spec", objectStorySpec);
        body.put("access_token", accessToken);

        String path = buildVersionedPath("/act_" + adAccountId + "/adcreatives");

        JsonNode response = executePost(path, body);
        return response.path("id").asText();
    }

    public String createAd(String adAccountId, AdRequest request) {
        Objects.requireNonNull(request, "request");

        Map<String, Object> body = new HashMap<>();
        body.put("name", request.name());
        body.put("adset_id", request.adSetId());
        body.put("creative", Map.of("creative_id", request.creativeId()));
        body.put("status", "PAUSED");
        body.put("access_token", accessToken);

        String path = buildVersionedPath("/act_" + adAccountId + "/ads");

        JsonNode response = executePost(path, body);
        return response.path("id").asText();
    }

    public JsonNode getCampaignMetrics(String campaignId) {
        String path = buildVersionedPath("/" + campaignId + "/insights?access_token=" + accessToken);
        String maskedPath = maskAccessTokenInPath(path);
        LOGGER.info("Sending GET request to Facebook API: path={}", maskedPath);
        try {
            FacebookApiResponse apiResponse = webClient
                .get()
                .uri(path)
                .exchangeToMono(response -> {
                    if (response.statusCode().isError()) {
                        return response.createException().flatMap(Mono::error);
                    }
                    return response
                        .bodyToMono(JsonNode.class)
                        .defaultIfEmpty(objectMapper.nullNode())
                        .map(body -> new FacebookApiResponse(response.statusCode(), response.headers().asHttpHeaders(), body));
                })
                .block();
            FacebookApiResponse nonNullResponse =
                apiResponse != null ? apiResponse : new FacebookApiResponse(null, HttpHeaders.EMPTY, objectMapper.nullNode());
            logSuccessfulResponse("GET", maskedPath, nonNullResponse);
            return nonNullResponse.body();
        } catch (WebClientResponseException ex) {
            String responseBody = ex.getResponseBodyAsString();
            LOGGER.error(
                "Facebook API GET request failed: path={}, status={}, responseBody={}, errorDetails={}",
                maskedPath,
                ex.getRawStatusCode(),
                maskAccessToken(responseBody),
                extractErrorDetails(responseBody),
                ex
            );
            throw ex;
        } catch (WebClientRequestException ex) {
            LOGGER.error("Facebook API GET request could not be completed: path={}, message={}", maskedPath, ex.getMessage(), ex);
            throw ex;
        }
    }

    private JsonNode executePost(String path, Map<String, Object> body) {
        Map<String, Object> sanitizedBody = sanitizeBody(body);
        String maskedPath = maskAccessTokenInPath(path);
        LOGGER.info("Sending POST request to Facebook API: path={}, body={}", maskedPath, sanitizedBody);
        try {
            FacebookApiResponse apiResponse = webClient
                .post()
                .uri(path)
                .bodyValue(body)
                .exchangeToMono(response -> {
                    if (response.statusCode().isError()) {
                        return response.createException().flatMap(Mono::error);
                    }
                    return response
                        .bodyToMono(JsonNode.class)
                        .defaultIfEmpty(objectMapper.nullNode())
                        .map(bodyNode -> new FacebookApiResponse(response.statusCode(), response.headers().asHttpHeaders(), bodyNode));
                })
                .block();
            FacebookApiResponse nonNullResponse =
                apiResponse != null ? apiResponse : new FacebookApiResponse(null, HttpHeaders.EMPTY, objectMapper.nullNode());
            logSuccessfulResponse("POST", maskedPath, nonNullResponse);
            return nonNullResponse.body();
        } catch (WebClientResponseException ex) {
            String responseBody = ex.getResponseBodyAsString();
            ObjectNode errorDetails = extractErrorDetails(responseBody);
            LOGGER.error(
                "Facebook API POST request failed: path={}, status={}, responseBody={}, errorDetails={}, headers={}",
                maskedPath,
                ex.getRawStatusCode(),
                maskAccessToken(responseBody),
                errorDetails,
                ex.getHeaders(),
                ex
            );
            if (isPermissionError(errorDetails)) {
                throw new FacebookPermissionException(resolvePermissionMessage(errorDetails), errorDetails, ex);
            }
            throw ex;
        } catch (WebClientRequestException ex) {
            LOGGER.error(
                "Facebook API POST request could not be completed: path={}, message={}",
                maskedPath,
                ex.getMessage(),
                ex
            );
            throw ex;
        }
    }

    private void logSuccessfulResponse(String method, String maskedPath, FacebookApiResponse response) {
        LOGGER.info(
            "Facebook API response received: method={}, path={}, status={}, headers={}, body={}",
            method,
            maskedPath,
            formatStatus(response.statusCode()),
            sanitizeHeaders(response.headers()),
            response.body()
        );
    }

    private String formatStatus(HttpStatusCode statusCode) {
        if (statusCode == null) {
            return "unknown";
        }
        return statusCode.value() + " " + statusCode;
    }

    private Map<String, List<String>> sanitizeHeaders(HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> sanitized = new HashMap<>();
        headers.forEach((key, values) ->
            sanitized.put(
                key,
                values.stream()
                    .map(this::maskAccessToken)
                    .collect(Collectors.toList())
            )
        );
        return sanitized;
    }

    private Map<String, Object> sanitizeBody(Map<String, Object> body) {
        Map<String, Object> sanitized = new HashMap<>();
        body.forEach((key, value) -> sanitized.put(key, sanitizeValue(key, value)));
        return sanitized;
    }

    private Object sanitizeValue(String key, Object value) {
        if (value instanceof Map<?, ?> valueMap) {
            Map<String, Object> nested = new HashMap<>();
            valueMap.forEach((nestedKey, nestedValue) ->
                nested.put(String.valueOf(nestedKey), sanitizeValue(String.valueOf(nestedKey), nestedValue))
            );
            return nested;
        }
        if (value instanceof List<?> listValue) {
            List<Object> sanitizedList = new ArrayList<>(listValue.size());
            for (Object item : listValue) {
                sanitizedList.add(sanitizeValue(key, item));
            }
            return sanitizedList;
        }
        if ("access_token".equalsIgnoreCase(key) && value instanceof String stringValue) {
            return maskAccessToken(stringValue);
        }
        return value;
    }

    private String maskAccessToken(String token) {
        if (token == null || token.isBlank()) {
            return token;
        }
        if (token.length() <= 6) {
            return "***";
        }
        return token.substring(0, 3) + "..." + token.substring(token.length() - 3);
    }

    private String maskAccessTokenInPath(String path) {
        if (path == null || path.isBlank() || accessToken == null || accessToken.isBlank()) {
            return path;
        }
        return path.replace(accessToken, maskAccessToken(accessToken));
    }

    private ObjectNode extractErrorDetails(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode errorNode = root.path("error");
            if (errorNode.isMissingNode() || errorNode.isNull()) {
                return null;
            }

            ObjectNode details = objectMapper.createObjectNode();
            copyIfPresent(errorNode, details, "type");
            copyIfPresent(errorNode, details, "code");
            copyIfPresent(errorNode, details, "error_subcode");
            copyIfPresent(errorNode, details, "message");
            copyIfPresent(errorNode, details, "error_user_title");
            copyIfPresent(errorNode, details, "error_user_msg");
            copyIfPresent(errorNode, details, "fbtrace_id");

            if (errorNode.has("error_data")) {
                details.set("error_data", errorNode.get("error_data"));
            }

            return details;
        } catch (Exception parsingError) {
            LOGGER.warn("Could not parse Facebook error payload: message={}", parsingError.getMessage(), parsingError);
            return null;
        }
    }

    private void copyIfPresent(JsonNode source, ObjectNode target, String fieldName) {
        JsonNode value = source.get(fieldName);
        if (value != null && !value.isNull()) {
            target.set(fieldName, value);
        }
    }

    private boolean isPermissionError(ObjectNode errorDetails) {
        if (errorDetails == null) {
            return false;
        }
        int code = errorDetails.path("code").asInt();
        if (code != 200) {
            return false;
        }

        int subcode = errorDetails.path("error_subcode").asInt();
        if (subcode == 1815066) {
            return true;
        }

        StringBuilder messageBuilder = new StringBuilder();
        if (errorDetails.has("message")) {
            messageBuilder.append(errorDetails.path("message").asText(""));
        }
        if (errorDetails.has("error_user_title")) {
            messageBuilder.append(' ').append(errorDetails.path("error_user_title").asText(""));
        }
        if (errorDetails.has("error_user_msg")) {
            messageBuilder.append(' ').append(errorDetails.path("error_user_msg").asText(""));
        }
        return messageBuilder.toString().toLowerCase().contains("permission");
    }

    private String resolvePermissionMessage(ObjectNode errorDetails) {
        if (errorDetails == null) {
            return "Facebook API returned a permissions error";
        }
        if (errorDetails.hasNonNull("error_user_msg")) {
            return errorDetails.get("error_user_msg").asText();
        }
        if (errorDetails.hasNonNull("message")) {
            return errorDetails.get("message").asText();
        }
        return "Facebook API returned a permissions error";
    }

    private String buildVersionedPath(String resourcePath) {
        String normalizedResource = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        return "/" + apiVersion + normalizedResource;
    }

    private String normalizeVersion(String version) {
        if (version == null || version.isBlank()) {
            return "v23.0";
        }
        String trimmed = version.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed.startsWith("v") ? trimmed : "v" + trimmed;
    }

    public record AdSetRequest(
        String name,
        String campaignId,
        String dailyBudget,
        String billingEvent,
        String optimizationGoal,
        String destinationType,
        String pageId,
        String targetCountry
    ) {}

    public record AdCreativeRequest(
        String name,
        String pageId,
        String instagramActorId,
        String websiteUrl,
        String message,
        String callToActionType
    ) {}

    public record AdRequest(
        String name,
        String adSetId,
        String creativeId
    ) {}

    private record FacebookApiResponse(HttpStatusCode statusCode, HttpHeaders headers, JsonNode body) {}
}
