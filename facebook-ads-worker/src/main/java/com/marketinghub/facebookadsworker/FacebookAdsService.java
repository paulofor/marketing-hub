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
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasText;

@Service
public class FacebookAdsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookAdsService.class);
    private static final Pattern INTEREST_WITH_ID_PATTERN = Pattern.compile(".*\\((\\d{5,})\\)\\s*$");
    private static final String INTEREST_SEARCH_LOCALE = "pt_BR";

    private final WebClient webClient;
    private final AtomicReference<String> accessToken;
    private final String apiVersion;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, String> pageAccessTokens;
    private final ConcurrentMap<String, String> interestIdCache;
    private static final Set<String> UNSUPPORTED_TARGETING_FIELDS = Set.of(
        "detailed_targeting_description"
    );

    public FacebookAdsService(WebClient.Builder builder,
                              @Value("${facebook.graph-api.base-url:https://graph.facebook.com}") String baseUrl,
                              @Value("${facebook.graph-api.version:v23.0}") String apiVersion,
                              ObjectMapper objectMapper) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.accessToken = new AtomicReference<>(null);
        this.apiVersion = normalizeVersion(apiVersion);
        this.objectMapper = objectMapper;
        this.pageAccessTokens = new ConcurrentHashMap<>();
        this.interestIdCache = new ConcurrentHashMap<>();
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

        removeUnsupportedTargetingFields(targeting);
        normalizeInterests(targeting);
        normalizeCustomAudiences(targeting);
        normalizeGeoLocations(targeting);
        disableAdvantageAudience(targeting);

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

    private void disableAdvantageAudience(Map<String, Object> targeting) {
        if (targeting == null) {
            return;
        }

        Map<String, Object> automation = new HashMap<>();
        Object existingAutomation = targeting.get("targeting_automation");
        if (existingAutomation instanceof Map<?, ?> existingMap) {
            existingMap.forEach((key, value) -> {
                if (key instanceof String stringKey) {
                    automation.put(stringKey, value);
                }
            });
        }

        automation.put("advantage_audience", 0);
        targeting.put("targeting_automation", automation);
    }

    private static final Map<String, Integer> LOCALE_CODE_TO_ID = Map.of(
        "pt_BR",
        16,
        "pt_PT",
        86,
        "es_LA",
        24,
        "es_ES",
        23,
        "en_US",
        6,
        "en_GB",
        47
    );

    private void removeUnsupportedTargetingFields(Map<String, Object> targeting) {
        if (targeting == null || targeting.isEmpty()) {
            return;
        }

        Object languages = targeting.remove("languages");
        if (languages != null) {
            normalizeLocales(targeting, languages);
        }

        normalizeExistingLocales(targeting);

        for (String field : UNSUPPORTED_TARGETING_FIELDS) {
            if (targeting.remove(field) != null) {
                LOGGER.warn(
                    "Removing unsupported targeting field '{}' before sending request to Facebook",
                    field
                );
            }
        }
    }

    private void normalizeLocales(Map<String, Object> targeting, Object languages) {
        if (targeting.containsKey("locales")) {
            LOGGER.warn("Ignoring targeting 'languages' because 'locales' is already defined");
            return;
        }
        if (!(languages instanceof List<?> languagesList) || languagesList.isEmpty()) {
            LOGGER.warn("Removing unsupported targeting field 'languages' before sending request to Facebook");
            return;
        }

        List<Integer> locales = languagesList
            .stream()
            .map(this::resolveLocaleValue)
            .filter(Objects::nonNull)
            .toList();

        if (locales.isEmpty()) {
            LOGGER.warn(
                "Removing unsupported targeting field 'languages' before sending request to Facebook after failing to map locales"
            );
            return;
        }

        targeting.put("locales", locales);
        LOGGER.warn("Replacing unsupported targeting field 'languages' with 'locales' before sending request to Facebook");
    }

    private void normalizeExistingLocales(Map<String, Object> targeting) {
        Object locales = targeting.get("locales");
        if (!(locales instanceof List<?> localeList) || localeList.isEmpty()) {
            return;
        }

        List<Integer> normalized = localeList
            .stream()
            .map(this::resolveLocaleValue)
            .filter(Objects::nonNull)
            .toList();

        if (normalized.isEmpty()) {
            targeting.remove("locales");
            LOGGER.warn("Removing targeting 'locales' after failing to coerce values to numeric IDs");
            return;
        }

        if (normalized.size() != localeList.size()) {
            LOGGER.warn("Coercing targeting 'locales' to numeric IDs supported by Meta Ads");
        }

        targeting.put("locales", normalized);
    }

    private Integer resolveLocaleValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }

        String text = value.toString();
        if (!hasText(text)) {
            return null;
        }

        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ignored) {
            // handled below when checking locale codes
        }

        String normalized = text.trim();
        if (!normalized.equals(normalized.toUpperCase(Locale.ROOT))) {
            normalized = normalized.toUpperCase(Locale.ROOT);
        }
        Integer resolved = LOCALE_CODE_TO_ID.get(normalized);
        if (resolved != null) {
            return resolved;
        }

        LOGGER.warn("Ignoring unsupported locale code '{}' in targeting.languages", text);
        return null;
    }

    private void normalizeInterests(Map<String, Object> targeting) {
        if (targeting == null || targeting.isEmpty()) {
            return;
        }
        Object rawInterests = targeting.get("interests");
        if (!(rawInterests instanceof List<?> interestsList) || interestsList.isEmpty()) {
            return;
        }

        List<Map<String, Object>> normalized = new ArrayList<>();
        boolean needsUpdate = false;

        for (Object interest : interestsList) {
            if (interest instanceof Map<?, ?> interestMap) {
                String id = extractInterestId(interestMap.get("id"));
                String name = extractInterestName(interestMap.get("name"));
                if (!hasText(id) && hasText(name)) {
                    id = resolveInterestId(name);
                    needsUpdate = true;
                }
                if (!hasText(id)) {
                    needsUpdate = true;
                    continue;
                }
                Map<String, Object> normalizedInterest = new HashMap<>();
                normalizedInterest.put("id", id);
                if (hasText(name)) {
                    normalizedInterest.put("name", name);
                }
                if (!interestMap.equals(normalizedInterest)) {
                    needsUpdate = true;
                }
                normalized.add(normalizedInterest);
            } else if (interest instanceof String interestName) {
                String id = resolveInterestId(interestName);
                if (!hasText(id)) {
                    LOGGER.warn(
                        "Skipping Facebook interest '{}' because no ID was resolved",
                        interestName
                    );
                    needsUpdate = true;
                    continue;
                }
                Map<String, Object> normalizedInterest = new HashMap<>();
                normalizedInterest.put("id", id);
                normalizedInterest.put("name", interestName.trim());
                normalized.add(normalizedInterest);
                needsUpdate = true;
            } else if (interest instanceof Number numberInterest) {
                String id = numberInterest.toString();
                if (!hasText(id)) {
                    needsUpdate = true;
                    continue;
                }
                Map<String, Object> normalizedInterest = new HashMap<>();
                normalizedInterest.put("id", id);
                normalized.add(normalizedInterest);
                needsUpdate = true;
            } else {
                needsUpdate = true;
            }
        }

        if (!needsUpdate) {
            return;
        }

        if (normalized.isEmpty()) {
            targeting.remove("interests");
            return;
        }

        targeting.put("interests", normalized);
    }

    private void normalizeCustomAudiences(Map<String, Object> targeting) {
        normalizeCustomAudienceList(targeting, "custom_audiences");
        normalizeCustomAudienceList(targeting, "excluded_custom_audiences");
    }

    private void normalizeCustomAudienceList(Map<String, Object> targeting, String fieldName) {
        if (targeting == null || targeting.isEmpty()) {
            return;
        }
        Object rawValue = targeting.get(fieldName);
        if (!(rawValue instanceof List<?> audienceList) || audienceList.isEmpty()) {
            return;
        }

        List<Map<String, Object>> normalized = new ArrayList<>();
        boolean needsUpdate = false;

        for (Object audience : audienceList) {
            if (audience instanceof Map<?, ?> audienceMap) {
                String id = extractCustomAudienceId(audienceMap.get("id"));
                if (!hasText(id)) {
                    id = extractCustomAudienceId(audienceMap.get("custom_audience_id"));
                }
                if (!hasText(id)) {
                    id = extractCustomAudienceId(audienceMap.get("audience_id"));
                }
                String name = extractCustomAudienceName(audienceMap.get("name"));
                if (!hasText(id)) {
                    needsUpdate = true;
                    LOGGER.warn(
                        "Skipping Facebook custom audience entry in '{}' because no ID was resolved",
                        fieldName
                    );
                    continue;
                }
                Map<String, Object> normalizedAudience = new HashMap<>();
                normalizedAudience.put("id", id);
                if (hasText(name)) {
                    normalizedAudience.put("name", name);
                }
                if (!audienceMap.equals(normalizedAudience)) {
                    needsUpdate = true;
                }
                normalized.add(normalizedAudience);
            } else if (audience instanceof String audienceString) {
                String id = extractCustomAudienceId(audienceString);
                if (!hasText(id)) {
                    needsUpdate = true;
                    LOGGER.warn(
                        "Skipping Facebook custom audience '{}' in '{}' because no ID was resolved",
                        audienceString,
                        fieldName
                    );
                    continue;
                }
                Map<String, Object> normalizedAudience = new HashMap<>();
                normalizedAudience.put("id", id);
                normalizedAudience.put("name", audienceString.trim());
                normalized.add(normalizedAudience);
                needsUpdate = true;
            } else if (audience instanceof Number audienceNumber) {
                String id = extractCustomAudienceId(audienceNumber);
                if (!hasText(id)) {
                    needsUpdate = true;
                    continue;
                }
                Map<String, Object> normalizedAudience = new HashMap<>();
                normalizedAudience.put("id", id);
                normalized.add(normalizedAudience);
                needsUpdate = true;
            } else {
                needsUpdate = true;
            }
        }

        if (!needsUpdate) {
            return;
        }

        if (normalized.isEmpty()) {
            targeting.remove(fieldName);
            return;
        }

        targeting.put(fieldName, normalized);
    }

    private void normalizeGeoLocations(Map<String, Object> targeting) {
        if (targeting == null || targeting.isEmpty()) {
            return;
        }

        Object geoLocations = targeting.get("geo_locations");
        if (!(geoLocations instanceof Map<?, ?> geoMap) || geoMap.isEmpty()) {
            return;
        }

        Map<String, Object> normalizedGeo = copyGeoMapWithStringKeys(geoMap);
        boolean updated = normalizeRegions(geoMap, normalizedGeo);

        if (updated) {
            targeting.put("geo_locations", normalizedGeo);
        }
    }

    private boolean normalizeRegions(Map<?, ?> geoMap, Map<String, Object> normalizedGeo) {
        Object regions = geoMap.get("regions");
        if (!(regions instanceof List<?> regionList) || regionList.isEmpty()) {
            return false;
        }

        List<Map<String, Object>> normalizedRegions = new ArrayList<>();
        boolean updated = false;

        for (Object region : regionList) {
            Integer key = extractRegionKey(region);
            if (key == null) {
                updated = true;
                LOGGER.warn(
                    "Skipping Facebook region targeting entry because key is not numeric: {}",
                    JsonLogFormatter.wrap(region)
                );
                continue;
            }

            Map<String, Object> normalizedRegion = new HashMap<>();
            normalizedRegion.put("key", key);
            normalizedRegions.add(normalizedRegion);

            if (!(region instanceof Map<?, ?> regionMap) || !Objects.equals(regionMap.get("key"), key)) {
                updated = true;
            }
        }

        if (normalizedRegions.isEmpty()) {
            normalizedGeo.remove("regions");
            return true;
        }

        if (updated) {
            normalizedGeo.put("regions", normalizedRegions);
        }

        return updated;
    }

    private Map<String, Object> copyGeoMapWithStringKeys(Map<?, ?> geoMap) {
        Map<String, Object> normalizedGeo = new HashMap<>();

        geoMap.forEach((key, value) -> {
            if (key instanceof String stringKey) {
                normalizedGeo.put(stringKey, value);
            } else {
                LOGGER.warn(
                    "Skipping geo_locations entry because key is not a string: {}",
                    JsonLogFormatter.wrap(key)
                );
            }
        });

        return normalizedGeo;
    }

    private Integer extractRegionKey(Object region) {
        if (region == null) {
            return null;
        }
        if (region instanceof Map<?, ?> regionMap) {
            return extractRegionKey(regionMap.get("key"));
        }
        if (region instanceof Number number) {
            return number.intValue();
        }
        if (!(region instanceof String stringRegion)) {
            return null;
        }

        String trimmed = stringRegion.trim();
        if (!hasText(trimmed)) {
            return null;
        }

        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String extractCustomAudienceId(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number numberValue) {
            String id = numberValue.toString();
            return hasText(id) ? id : null;
        }
        if (value instanceof String stringValue) {
            return extractInterestIdFromText(stringValue);
        }
        return null;
    }

    private String extractCustomAudienceName(Object value) {
        if (!(value instanceof String stringValue)) {
            return null;
        }
        String trimmed = stringValue.trim();
        return hasText(trimmed) ? trimmed : null;
    }

    private String extractInterestId(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number numberValue) {
            String id = numberValue.toString();
            return hasText(id) ? id : null;
        }
        if (value instanceof String stringValue) {
            return extractInterestIdFromText(stringValue);
        }
        return null;
    }

    private String extractInterestName(Object value) {
        if (!(value instanceof String stringValue)) {
            return null;
        }
        String trimmed = stringValue.trim();
        return hasText(trimmed) ? trimmed : null;
    }

    private String extractInterestIdFromText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (!hasText(trimmed)) {
            return null;
        }
        if (trimmed.chars().allMatch(Character::isDigit)) {
            return trimmed;
        }
        Matcher matcher = INTEREST_WITH_ID_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    private String resolveInterestId(String interestName) {
        if (!hasText(interestName)) {
            return null;
        }
        String normalizedName = interestName.trim();
        if (!hasText(normalizedName)) {
            return null;
        }

        String directId = extractInterestIdFromText(normalizedName);
        if (hasText(directId)) {
            return directId;
        }

        String cacheKey = normalizedName.toLowerCase(Locale.ROOT);
        String cached = interestIdCache.get(cacheKey);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }

        String resolved = fetchInterestIdFromFacebook(normalizedName);
        interestIdCache.put(cacheKey, resolved != null ? resolved : "");
        return resolved;
    }

    private String fetchInterestIdFromFacebook(String interestName) {
        String path = UriComponentsBuilder
            .fromPath(buildVersionedPath("/search"))
            .queryParam("type", "adinterest")
            .queryParam("q", interestName)
            .queryParam("limit", 1)
            .queryParam("fields", "id,name")
            .queryParam("locale", INTEREST_SEARCH_LOCALE)
            .queryParam("access_token", requireAccessToken())
            .encode(StandardCharsets.UTF_8)
            .toUriString();

        try {
            FacebookApiResponse response = executeGet(path);
            if (response == null || response.body() == null) {
                return null;
            }
            JsonNode data = response.body().path("data");
            if (data == null || !data.isArray()) {
                return null;
            }
            for (JsonNode node : data) {
                if (node == null || node.isNull()) {
                    continue;
                }
                String id = node.path("id").asText(null);
                if (hasText(id)) {
                    return id.trim();
                }
            }
        } catch (WebClientResponseException ex) {
            LOGGER.warn(
                "Facebook interest lookup failed for '{}': status={}, message={}",
                interestName,
                ex.getRawStatusCode(),
                ex.getMessage()
            );
        } catch (WebClientRequestException ex) {
            LOGGER.warn(
                "Facebook interest lookup failed for '{}': message={}",
                interestName,
                ex.getMessage()
            );
        } catch (Exception ex) {
            LOGGER.warn(
                "Unexpected error while looking up Facebook interest '{}': message={}",
                interestName,
                ex.getMessage(),
                ex
            );
        }
        return null;
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

        normalizeInterests(targeting);
        normalizeCustomAudiences(targeting);

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
