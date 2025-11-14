package com.marketinghub.facebookadsworker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.facebookadsworker.util.InstantFormPublicationHelper;
import com.marketinghub.facebookadsworker.util.JsonLogFormatter;
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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasText;

@Service
public class FacebookAdsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookAdsService.class);

    private final WebClient webClient;
    private final AtomicReference<String> accessToken;
    private final String apiVersion;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, String> pageAccessTokens;

    public FacebookAdsService(WebClient.Builder builder,
                              @Value("${facebook.graph-api.base-url:https://graph.facebook.com}") String baseUrl,
                              @Value("${facebook.graph-api.version:v23.0}") String apiVersion,
                              ObjectMapper objectMapper) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.accessToken = new AtomicReference<>(null);
        this.apiVersion = normalizeVersion(apiVersion);
        this.objectMapper = objectMapper;
        this.pageAccessTokens = new ConcurrentHashMap<>();
        LOGGER.info("Configured Facebook Graph API version: {}", this.apiVersion);
    }

    public String getCurrentAccessToken() {
        return accessToken.get();
    }

    private String requireAccessToken() {
        String token = accessToken.get();
        if (!hasText(token)) {
            throw new IllegalStateException("Facebook access token is not configured");
        }
        return token;
    }

    public void updateAccessToken(String newToken) {
        if (!hasText(newToken)) {
            throw new IllegalArgumentException("newToken must not be blank");
        }
        String maskedOldToken = maskAccessToken(accessToken.get());
        String maskedNewToken = maskAccessToken(newToken);
        accessToken.set(newToken.trim());
        pageAccessTokens.clear();
        LOGGER.info(
            "Facebook access token updated: previousToken={}, newToken={}",
            maskedOldToken,
            maskedNewToken
        );
    }

    public String createInstagramCampaign(String adAccountId, String name) {
        return createInstagramCampaign(adAccountId, name, "OUTCOME_TRAFFIC");
    }

    public String createInstagramCampaign(String adAccountId, String name, String objective) {
        String path = buildVersionedPath("/act_" + adAccountId + "/campaigns");
        String resolvedObjective = hasText(objective) ? objective : "OUTCOME_TRAFFIC";
        Map<String, Object> body = Map.of(
            "name", name,
            "objective", resolvedObjective,
            "status", "PAUSED",
            "special_ad_categories", List.of(),
            "access_token", requireAccessToken()
        );

        JsonNode response = executePost(path, body);
        return response.path("id").asText();
    }

    public String createCampaign(String adAccountId, String name) {
        return createCampaign(adAccountId, name, "OUTCOME_TRAFFIC");
    }

    public String createCampaign(String adAccountId, String name, String objective) {
        return createInstagramCampaign(adAccountId, name, objective);
    }

    public String createAdSet(String adAccountId, AdSetRequest request) {
        Objects.requireNonNull(request, "request");

        Map<String, Object> targeting = buildTargeting(request);

        Map<String, Object> body = new HashMap<>();
        body.put("name", request.name());
        body.put("campaign_id", request.campaignId());
        body.put("daily_budget", request.dailyBudget());
        body.put("billing_event", request.billingEvent());
        body.put("optimization_goal", request.optimizationGoal());
        body.put("status", "PAUSED");
        body.put("destination_type", request.destinationType());
        body.put("targeting", targeting);
        if (request.bidStrategy() != null && !request.bidStrategy().isBlank()) {
            body.put("bid_strategy", request.bidStrategy());
        }
        if (request.bidAmount() != null && !request.bidAmount().isBlank()) {
            body.put("bid_amount", request.bidAmount());
        }
        if (request.pageId() != null && !request.pageId().isBlank()) {
            body.put("promoted_object", Map.of("page_id", request.pageId()));
        }
        body.put("access_token", requireAccessToken());

        String path = buildVersionedPath("/act_" + adAccountId + "/adsets");

        JsonNode response = executePost(path, body);
        return response.path("id").asText();
    }

    private Map<String, Object> buildTargeting(AdSetRequest request) {
        Map<String, Object> targeting = new HashMap<>();
        if (hasText(request.targetingJson())) {
            try {
                JsonNode node = objectMapper.readTree(request.targetingJson());
                if (node != null && node.isObject()) {
                    Map<String, Object> parsed = objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
                    if (parsed != null) {
                        targeting.putAll(parsed);
                    }
                }
            } catch (Exception ex) {
                LOGGER.warn(
                    "Failed to parse targeting JSON for ad set '{}': {}",
                    request.name(),
                    ex.getMessage(),
                    ex
                );
            }
        }

        if (hasText(request.savedAudienceId())) {
            targeting.put("saved_audience_id", request.savedAudienceId().trim());
        }

        if (!targeting.containsKey("geo_locations")) {
            String country = request.targetCountry();
            if (hasText(country)) {
                targeting.put("geo_locations", Map.of("countries", List.of(country)));
            }
        }

        if (targeting.isEmpty()) {
            String country = request.targetCountry();
            if (hasText(country)) {
                targeting.put("geo_locations", Map.of("countries", List.of(country)));
            } else {
                targeting.put("geo_locations", Map.of());
            }
        }

        return targeting;
    }

    public String createSavedAudience(String adAccountId, SavedAudienceRequest request) {
        Objects.requireNonNull(request, "request");
        if (!hasText(request.name())) {
            throw new IllegalArgumentException("Saved audience name must not be blank");
        }
        if (!hasText(request.targetingJson())) {
            throw new IllegalArgumentException("Saved audience targeting must not be blank");
        }

        Map<String, Object> targeting;
        try {
            JsonNode node = objectMapper.readTree(request.targetingJson());
            if (node == null || !node.isObject()) {
                throw new IllegalArgumentException("Saved audience targeting must be a JSON object");
            }
            targeting = objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse saved audience targeting JSON", ex);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("name", request.name().trim());
        if (hasText(request.description())) {
            body.put("description", request.description().trim());
        }
        body.put("targeting", targeting);
        body.put("access_token", requireAccessToken());

        String path = buildVersionedPath("/act_" + adAccountId + "/saved_audiences");
        JsonNode response = executePost(path, body);
        return response.path("id").asText();
    }

    public String createAdCreative(String adAccountId, AdCreativeRequest request) {
        Objects.requireNonNull(request, "request");

        boolean hasLeadGenForm = hasText(request.leadGenFormId());
        String resolvedLink = null;
        if (hasText(request.websiteUrl())) {
            resolvedLink = request.websiteUrl().trim();
        } else if (hasLeadGenForm) {
            resolvedLink = InstantFormPublicationHelper.buildInstantFormShareLink(request.leadGenFormId());
        }
        boolean hasWebsiteUrl = hasText(resolvedLink);

        Map<String, Object> linkData = new HashMap<>();
        if (hasWebsiteUrl) {
            linkData.put("link", resolvedLink);
        }
        linkData.put("message", request.message());
        if (request.headline() != null && !request.headline().isBlank()) {
            linkData.put("name", request.headline());
        }
        if (request.description() != null && !request.description().isBlank()) {
            linkData.put("description", request.description());
        }
        if (hasText(request.imageHash())) {
            linkData.put("image_hash", request.imageHash());
        } else if (hasText(request.imageUrl())) {
            linkData.put("picture", request.imageUrl());
        }
        if (request.callToActionType() != null && !request.callToActionType().isBlank()) {
            Map<String, Object> callToAction = new HashMap<>();
            callToAction.put("type", request.callToActionType());
            Map<String, Object> value = new HashMap<>();
            if (hasWebsiteUrl) {
                value.put("link", resolvedLink);
            }
            if (hasLeadGenForm) {
                value.put("lead_gen_form_id", request.leadGenFormId());
            }
            if (!value.isEmpty()) {
                callToAction.put("value", value);
            }
            linkData.put("call_to_action", callToAction);
        }

        Map<String, Object> objectStorySpec = new HashMap<>();
        objectStorySpec.put("page_id", request.pageId());
        if (request.instagramActorId() != null && !request.instagramActorId().isBlank()) {
            objectStorySpec.put("instagram_user_id", request.instagramActorId());
        }
        objectStorySpec.put("link_data", linkData);

        Map<String, Object> body = new HashMap<>();
        body.put("name", request.name());
        body.put("object_story_spec", objectStorySpec);
        body.put("access_token", requireAccessToken());

        String path = buildVersionedPath("/act_" + adAccountId + "/adcreatives");

        JsonNode response = executePost(path, body);
        return response.path("id").asText();
    }

    public String createInstantForm(String pageId, InstantFormCreationRequest request) {
        if (!hasText(pageId)) {
            throw new IllegalArgumentException("pageId must not be blank");
        }
        Objects.requireNonNull(request, "request");

        String normalizedPageId = pageId.trim();
        if (!hasText(normalizedPageId)) {
            throw new IllegalArgumentException("pageId must not be blank");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("name", request.name());
        if (hasText(request.locale())) {
            body.put("locale", request.locale());
        }
        if (request.privacyPolicy() != null && hasText(request.privacyPolicy().url())) {
            Map<String, Object> privacyPolicy = new HashMap<>();
            privacyPolicy.put("url", request.privacyPolicy().url());
            if (hasText(request.privacyPolicy().linkText())) {
                privacyPolicy.put("link_text", request.privacyPolicy().linkText());
            }
            body.put("privacy_policy", privacyPolicy);
        }
        if (request.questions() != null && !request.questions().isEmpty()) {
            List<Map<String, Object>> questions = new ArrayList<>(request.questions().size());
            for (InstantFormCreationRequest.Question question : request.questions()) {
                if (question == null || !hasText(question.type())) {
                    continue;
                }
                Map<String, Object> questionMap = new HashMap<>();
                String questionType = question.type().trim();
                questionMap.put("type", questionType);
                if (question.key() != null && !question.key().isBlank()) {
                    questionMap.put("key", question.key());
                }
                if (question.label() != null && !question.label().isBlank()) {
                    if (isCustomInstantFormQuestion(questionType)) {
                        questionMap.put("label", question.label());
                    } else {
                        LOGGER.debug(
                            "Ignoring label for instant form question of type {} because Graph API does not accept custom labels",
                            questionType
                        );
                    }
                }
                if (question.options() != null && !question.options().isEmpty()) {
                    questionMap.put("options", question.options());
                }
                if (question.helperText() != null && !question.helperText().isBlank()) {
                    LOGGER.debug(
                        "Ignoring helper text for instant form question of type {} as it is not supported by the Graph API",
                        question.type()
                    );
                }
                if (question.required() != null) {
                    questionMap.put("required", question.required());
                }
                if (Boolean.TRUE.equals(question.allowMultiSelect())) {
                    questionMap.put("allow_multiple_selections", Boolean.TRUE);
                }
                questions.add(questionMap);
            }
            if (!questions.isEmpty()) {
                body.put("questions", questions);
            }
        }
        if (hasText(request.followUpActionUrl())) {
            body.put("follow_up_action_url", request.followUpActionUrl());
            if (hasText(request.followUpActionText())) {
                body.put("follow_up_action_text", request.followUpActionText());
            }
        }

        String pageAccessToken = resolvePageAccessToken(normalizedPageId);
        if (!hasText(pageAccessToken)) {
            LOGGER.warn("Falling back to account access token while creating instant form for page {}", normalizedPageId);
            pageAccessToken = requireAccessToken();
        }
        body.put("access_token", pageAccessToken);

        String path = buildVersionedPath("/" + normalizedPageId + "/leadgen_forms");
        try {
            JsonNode response = executePost(path, body);
            String createdId = extractInstantFormCreationIdentifier(response);
            if (!hasText(createdId)) {
                LOGGER.warn(
                    "Facebook response when creating instant form on page {} did not include an identifier: response={}",
                    normalizedPageId,
                    response
                );
            }
            return createdId;
        } catch (WebClientResponseException ex) {
            String existingIdentifier = handleInstantFormDuplicate(normalizedPageId, request, ex);
            if (hasText(existingIdentifier)) {
                return existingIdentifier;
            }
            throw ex;
        }
    }

    public String uploadAdImage(String adAccountId, String imageUrl) {
        Objects.requireNonNull(adAccountId, "adAccountId");
        if (!hasText(imageUrl)) {
            throw new IllegalArgumentException("imageUrl must not be blank");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("url", imageUrl);
        body.put("access_token", requireAccessToken());

        String path = buildVersionedPath("/act_" + adAccountId + "/adimages");
        JsonNode response = executePost(path, body);
        JsonNode imagesNode = response.path("images");
        if (imagesNode.isMissingNode() || imagesNode.isNull()) {
            throw new IllegalStateException("Facebook did not return any image hash");
        }

        for (JsonNode value : imagesNode) {
            String hash = value.path("hash").asText(null);
            if (hasText(hash)) {
                return hash;
            }
        }
        throw new IllegalStateException("Facebook image upload response did not contain a hash");
    }

    public String createAd(String adAccountId, AdRequest request) {
        Objects.requireNonNull(request, "request");

        Map<String, Object> body = new HashMap<>();
        body.put("name", request.name());
        body.put("adset_id", request.adSetId());
        body.put("creative", Map.of("creative_id", request.creativeId()));
        body.put("status", "PAUSED");
        body.put("access_token", requireAccessToken());

        String path = buildVersionedPath("/act_" + adAccountId + "/ads");

        JsonNode response = executePost(path, body);
        return response.path("id").asText();
    }

    public JsonNode getCampaignMetrics(String campaignId) {
        String path = buildVersionedPath("/" + campaignId + "/insights?access_token=" + requireAccessToken());
        FacebookApiResponse response = executeGet(path);
        return response.body();
    }

    public void publishInstantForm(String formId) {
        Objects.requireNonNull(formId, "formId");
        String normalizedFormId = formId.trim();
        if (!hasText(normalizedFormId)) {
            throw new IllegalArgumentException("formId must not be blank");
        }

        JsonNode details = null;
        try {
            details = fetchInstantForm(normalizedFormId);
        } catch (WebClientResponseException.NotFound ex) {
            LOGGER.warn(
                "Facebook instant form {} was not found while checking status before publication; proceeding with publish request",
                normalizedFormId,
                ex
            );
        } catch (WebClientResponseException ex) {
            LOGGER.warn(
                "Could not verify Facebook instant form {} status before publication: status={}, message={}",
                normalizedFormId,
                ex.getRawStatusCode(),
                ex.getMessage(),
                ex
            );
        }
        if (details != null) {
            String status = details.path("status").asText(null);
            if (hasText(status) && "ACTIVE".equalsIgnoreCase(status.trim())) {
                LOGGER.info(
                    "Instant form {} is already ACTIVE on Facebook; skipping publication request.",
                    normalizedFormId
                );
                return;
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("status", "ACTIVE");
        body.put("access_token", requireAccessToken());
        String path = buildVersionedPath("/" + normalizedFormId);
        executePost(path, body);
    }

    public JsonNode fetchInstantForm(String formId) {
        Objects.requireNonNull(formId, "formId");
        String path = buildVersionedPath("/" + formId + "?access_token=" + requireAccessToken());
        return executeGet(path).body();
    }

    public String findInstantFormIdentifier(String pageId, String formName) {
        if (!hasText(pageId) || !hasText(formName)) {
            return null;
        }
        String normalizedPageId = pageId.trim();
        String normalizedName = formName.trim();
        if (!hasText(normalizedPageId) || !hasText(normalizedName)) {
            return null;
        }

        String pageAccessToken = resolvePageAccessToken(normalizedPageId);
        if (!hasText(pageAccessToken)) {
            LOGGER.warn(
                "Could not resolve page access token while looking for leadgen forms on page {}; skipping lookup",
                normalizedPageId
            );
            return null;
        }

        String path = buildVersionedPath(
            "/" +
            normalizedPageId +
            "/leadgen_forms?fields=id,name,status,draft_id&limit=200&access_token=" +
            pageAccessToken
        );
        FacebookApiResponse response = executeGet(path);
        if (response == null || response.body() == null) {
            return null;
        }
        JsonNode data = response.body().path("data");
        if (data == null || !data.isArray()) {
            return null;
        }

        String fallbackIdentifier = null;
        for (JsonNode node : data) {
            if (node == null || node.isNull()) {
                continue;
            }
            String candidateName = node.path("name").asText(null);
            if (!hasText(candidateName) || !candidateName.trim().equalsIgnoreCase(normalizedName)) {
                continue;
            }
            String resolvedIdentifier = extractInstantFormIdentifier(node);
            if (!hasText(resolvedIdentifier)) {
                continue;
            }
            String status = node.path("status").asText(null);
            if (hasText(status) && "DRAFT".equalsIgnoreCase(status.trim())) {
                return resolvedIdentifier;
            }
            if (fallbackIdentifier == null) {
                fallbackIdentifier = resolvedIdentifier;
            }
        }
        return fallbackIdentifier;
    }

    private String handleInstantFormDuplicate(
        String pageId,
        InstantFormCreationRequest request,
        WebClientResponseException ex
    ) {
        ObjectNode errorDetails = extractErrorDetails(ex.getResponseBodyAsString());
        if (!isDuplicateInstantFormNameError(errorDetails)) {
            return null;
        }
        if (request == null || !hasText(request.name())) {
            LOGGER.warn(
                "Facebook reported duplicate instant form name for page {}, but the request did not include a valid name",
                pageId
            );
            return null;
        }
        String resolvedIdentifier = findInstantFormIdentifier(pageId, request.name());
        if (hasText(resolvedIdentifier)) {
            LOGGER.info(
                "Facebook reported duplicate instant form name; reusing identifier {} for page {} (name={})",
                resolvedIdentifier,
                pageId,
                request.name()
            );
            return resolvedIdentifier;
        }
        LOGGER.warn(
            "Facebook reported duplicate instant form name for page {}, but no existing identifier could be resolved",
            pageId
        );
        return null;
    }

    private boolean isDuplicateInstantFormNameError(ObjectNode errorDetails) {
        if (errorDetails == null) {
            return false;
        }
        int code = errorDetails.path("code").asInt();
        int subcode = errorDetails.path("error_subcode").asInt();
        if (code == 100 && subcode == 1892019) {
            return true;
        }
        String normalizedMessage = buildCombinedErrorMessage(errorDetails);
        if (!hasText(normalizedMessage)) {
            return false;
        }
        return normalizedMessage.contains("form name already exists") ||
            normalizedMessage.contains("nome do formulário já existe") ||
            normalizedMessage.contains("nome do formulario ja existe");
    }

    private String buildCombinedErrorMessage(ObjectNode errorDetails) {
        StringBuilder builder = new StringBuilder();
        appendLowerCaseErrorField(builder, errorDetails, "message");
        appendLowerCaseErrorField(builder, errorDetails, "error_user_title");
        appendLowerCaseErrorField(builder, errorDetails, "error_user_msg");
        if (errorDetails.hasNonNull("error_data")) {
            String errorDataText = errorDetails.get("error_data").toString();
            if (!errorDataText.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(errorDataText.toLowerCase(Locale.ROOT));
            }
        }
        return builder.toString();
    }

    private void appendLowerCaseErrorField(StringBuilder builder, ObjectNode errorDetails, String fieldName) {
        if (errorDetails.hasNonNull(fieldName)) {
            String value = errorDetails.get(fieldName).asText("");
            if (!value.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(value.toLowerCase(Locale.ROOT));
            }
        }
    }

    private String extractInstantFormIdentifier(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String draftId = node.path("draft_id").asText(null);
        if (hasText(draftId)) {
            return draftId.trim();
        }
        String id = node.path("id").asText(null);
        return hasText(id) ? id.trim() : null;
    }

    private JsonNode executePost(String path, Map<String, Object> body) {
        Map<String, Object> sanitizedBody = sanitizeBody(body);
        String maskedPath = maskAccessTokenInPath(path);
        LOGGER.info(
            "Sending POST request to Facebook API: path==>{}, body={}",
            maskedPath,
            JsonLogFormatter.wrap(objectMapper, sanitizedBody)
        );
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
                "Facebook API POST request failed: path<=={}, status={}, responseBody={}, errorDetails={}, headers={}",
                maskedPath,
                ex.getRawStatusCode(),
                maskAccessToken(responseBody),
                errorDetails,
                JsonLogFormatter.wrap(objectMapper, sanitizeHeaders(ex.getHeaders())),
                ex
            );
            if (isAccessTokenExpired(errorDetails)) {
                throw new FacebookAccessTokenExpiredException(resolveAccessTokenExpiredMessage(errorDetails), errorDetails, ex);
            }
            if (isPermissionError(errorDetails)) {
                throw new FacebookPermissionException(resolvePermissionMessage(errorDetails), errorDetails, ex);
            }
            throw ex;
        } catch (WebClientRequestException ex) {
            LOGGER.error(
                "Facebook API POST request could not be completed: path==>{}, message={}",
                maskedPath,
                ex.getMessage(),
                ex
            );
            throw ex;
        }
    }

    private String extractInstantFormCreationIdentifier(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String[] candidateFields = {"id", "draft_id", "form_id"};
        for (String field : candidateFields) {
            String value = node.path(field).asText(null);
            if (hasText(value)) {
                return value.trim();
            }
        }
        JsonNode nestedData = node.path("data");
        if (nestedData != null && nestedData.isArray()) {
            for (JsonNode element : nestedData) {
                String nested = extractInstantFormCreationIdentifier(element);
                if (hasText(nested)) {
                    return nested;
                }
            }
        }
        JsonNode instantFormNode = node.path("instant_form");
        if (instantFormNode != null && instantFormNode.isObject()) {
            String nested = extractInstantFormCreationIdentifier(instantFormNode);
            if (hasText(nested)) {
                return nested;
            }
        }
        return null;
    }

    private FacebookApiResponse executeGet(String path) {
        String maskedPath = maskAccessTokenInPath(path);
        LOGGER.info("Sending GET request to Facebook API: path==>{}", maskedPath);
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
            return nonNullResponse;
        } catch (WebClientResponseException ex) {
            String responseBody = ex.getResponseBodyAsString();
            ObjectNode errorDetails = extractErrorDetails(responseBody);
            LOGGER.error(
                "Facebook API GET request failed: path<=={}, status={}, responseBody={}, errorDetails={}",
                maskedPath,
                ex.getRawStatusCode(),
                maskAccessToken(responseBody),
                errorDetails,
                ex
            );
            if (isAccessTokenExpired(errorDetails)) {
                throw new FacebookAccessTokenExpiredException(resolveAccessTokenExpiredMessage(errorDetails), errorDetails, ex);
            }
            if (isPermissionError(errorDetails)) {
                throw new FacebookPermissionException(resolvePermissionMessage(errorDetails), errorDetails, ex);
            }
            throw ex;
        } catch (WebClientRequestException ex) {
            LOGGER.error(
                "Facebook API GET request could not be completed: path==>{}, message={}",
                maskedPath,
                ex.getMessage(),
                ex
            );
            throw ex;
        }
    }

    private String resolvePageAccessToken(String pageId) {
        return pageAccessTokens.computeIfAbsent(pageId, this::requestPageAccessToken);
    }

    private String requestPageAccessToken(String pageId) {
        if (!hasText(pageId)) {
            return null;
        }
        String path = buildVersionedPath(
            "/" +
            pageId +
            "?fields=access_token&access_token=" +
            requireAccessToken()
        );
        FacebookApiResponse response = executeGet(path);
        if (response == null || response.body() == null) {
            LOGGER.warn("Facebook response when resolving page access token for page {} was empty", pageId);
            return null;
        }
        String token = response.body().path("access_token").asText(null);
        if (!hasText(token)) {
            LOGGER.warn("Facebook response did not include access token while resolving page {}", pageId);
            return null;
        }
        return token.trim();
    }

    private void logSuccessfulResponse(String method, String maskedPath, FacebookApiResponse response) {
        LOGGER.info(
            "Facebook API response received: method={}, path<=={}, status={}, headers={}, body={}",
            method,
            maskedPath,
            formatStatus(response.statusCode()),
            JsonLogFormatter.wrap(objectMapper, sanitizeHeaders(response.headers())),
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
        String currentToken = accessToken.get();
        if (path == null || path.isBlank() || currentToken == null || currentToken.isBlank()) {
            return path;
        }
        return path.replace(currentToken, maskAccessToken(currentToken));
    }

    private boolean isCustomInstantFormQuestion(String questionType) {
        if (!hasText(questionType)) {
            return false;
        }
        String normalized = questionType.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("CUSTOM") || normalized.startsWith("CUSTOM_");
    }

    public record InstantFormCreationRequest(
        String name,
        String locale,
        PrivacyPolicy privacyPolicy,
        List<Question> questions,
        String followUpActionText,
        String followUpActionUrl
    ) {
        public record PrivacyPolicy(String url, String linkText) {
        }

        public record Question(
            String type,
            String key,
            String label,
            List<Map<String, Object>> options,
            String helperText,
            Boolean required,
            Boolean allowMultiSelect
        ) {
            public Question(String type) {
                this(type, null, null, null, null, null, null);
            }
        }
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

    private boolean isAccessTokenExpired(ObjectNode errorDetails) {
        if (errorDetails == null) {
            return false;
        }
        int code = errorDetails.path("code").asInt();
        if (code != 190) {
            return false;
        }

        int subcode = errorDetails.path("error_subcode").asInt();
        if (subcode == 463 || subcode == 467) {
            return true;
        }

        String message = errorDetails.path("message").asText("").toLowerCase();
        return message.contains("session has expired") || message.contains("token has expired");
    }

    private String resolveAccessTokenExpiredMessage(ObjectNode errorDetails) {
        if (errorDetails == null) {
            return "Facebook access token has expired";
        }
        if (errorDetails.hasNonNull("error_user_msg")) {
            return errorDetails.get("error_user_msg").asText();
        }
        if (errorDetails.hasNonNull("message")) {
            return errorDetails.get("message").asText();
        }
        return "Facebook access token has expired";
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

    public TokenRenewalResponse renewLongLivedToken(String appId, String appSecret, String currentToken) {
        Objects.requireNonNull(appId, "appId");
        Objects.requireNonNull(appSecret, "appSecret");
        Objects.requireNonNull(currentToken, "currentToken");

        String path = buildVersionedPath("/oauth/access_token");
        LOGGER.info(
            "Requesting Facebook long-lived token renewal: appId={}, token={}",
            appId,
            maskAccessToken(currentToken)
        );

        try {
            JsonNode response = webClient
                .get()
                .uri(uriBuilder ->
                    uriBuilder
                        .path(path)
                        .queryParam("grant_type", "fb_exchange_token")
                        .queryParam("client_id", appId)
                        .queryParam("client_secret", appSecret)
                        .queryParam("fb_exchange_token", currentToken)
                        .build()
                )
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

            if (response == null || response.isMissingNode()) {
                throw new IllegalStateException("Facebook returned an empty response while renewing the token");
            }

            String newToken = response.path("access_token").asText(null);
            long expiresInSeconds = response.path("expires_in").asLong(0L);
            if (newToken == null || newToken.isBlank()) {
                throw new IllegalStateException("Facebook response does not contain access_token");
            }

            LOGGER.info(
                "Facebook token renewal succeeded for appId={}, expiresInSeconds={}, token={}",
                appId,
                expiresInSeconds,
                maskAccessToken(newToken)
            );
            return new TokenRenewalResponse(newToken, expiresInSeconds);
        } catch (WebClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            LOGGER.error(
                "Facebook token renewal failed: appId={}, status={}, responseBody={}",
                appId,
                ex.getRawStatusCode(),
                maskAccessToken(body),
                ex
            );
            throw ex;
        } catch (WebClientRequestException ex) {
            LOGGER.error(
                "Facebook token renewal request could not be completed: appId={}, message={}",
                appId,
                ex.getMessage(),
                ex
            );
            throw ex;
        }
    }

    public record AdSetRequest(
        String name,
        String campaignId,
        String dailyBudget,
        String billingEvent,
        String optimizationGoal,
        String destinationType,
        String bidStrategy,
        String bidAmount,
        String pageId,
        String targetCountry,
        String targetingJson,
        String savedAudienceId
    ) {}

    public record SavedAudienceRequest(String name, String description, String targetingJson) {}

    public record AdCreativeRequest(
        String name,
        String pageId,
        String instagramActorId,
        String websiteUrl,
        String leadGenFormId,
        String message,
        String imageHash,
        String imageUrl,
        String callToActionType,
        String headline,
        String description
    ) {}

    public record AdRequest(
        String name,
        String adSetId,
        String creativeId
    ) {}

    public record TokenRenewalResponse(String accessToken, long expiresInSeconds) {}

    private record FacebookApiResponse(HttpStatusCode statusCode, HttpHeaders headers, JsonNode body) {}
}
