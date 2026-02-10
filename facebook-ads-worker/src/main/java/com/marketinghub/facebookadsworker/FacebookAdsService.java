package com.marketinghub.facebookadsworker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.facebookadsworker.util.InstantFormPublicationHelper;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingResolverProperties;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingCandidateType;
import com.marketinghub.facebookadsworker.util.JsonLogFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.EnumMap;
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
    public static final String BRAZIL_COUNTRY_CODE = "BR";

    private final WebClient webClient;
    private final AtomicReference<String> accessToken;
    private final String apiVersion;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, String> pageAccessTokens;
    private final ConcurrentMap<String, String> interestIdCache;
    private final ConcurrentMap<TargetingCategoryCacheKey, String> targetingCategoryIdCache;
    private final Duration targetingSearchCacheTtl;
    private final ConcurrentMap<TargetingSearchCacheKey, CachedTargetingSearchResults> targetingSearchCache;
    private final String defaultAdAccountId;
    private final ThreadLocal<FacebookApiCallDebugInfo> lastApiCallDebugInfo = new ThreadLocal<>();
    private static final Set<String> UNSUPPORTED_TARGETING_FIELDS = Set.of(
        "detailed_targeting_description"
    );

    public FacebookAdsService(WebClient.Builder builder,
                              @Value("${facebook.graph-api.base-url:https://graph.facebook.com}") String baseUrl,
                              @Value("${facebook.graph-api.version:v23.0}") String apiVersion,
                              ObjectMapper objectMapper,
                              TargetingResolverProperties resolverProperties) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.accessToken = new AtomicReference<>(null);
        this.apiVersion = normalizeVersion(apiVersion);
        this.objectMapper = objectMapper;
        this.pageAccessTokens = new ConcurrentHashMap<>();
        this.interestIdCache = new ConcurrentHashMap<>();
        this.targetingCategoryIdCache = new ConcurrentHashMap<>();
        Duration configuredTtl = resolverProperties != null ? resolverProperties.getCacheTtl() : null;
        this.targetingSearchCacheTtl = configuredTtl != null && !configuredTtl.isNegative() && !configuredTtl.isZero()
            ? configuredTtl
            : Duration.ofMinutes(30);
        this.targetingSearchCache = new ConcurrentHashMap<>();
        this.defaultAdAccountId = normalizeAdAccountId(
            resolverProperties != null ? resolverProperties.getDefaultAdAccountId() : null
        );
        LOGGER.info("Configured Facebook Graph API version: {}", this.apiVersion);
    }

    public String getCurrentAccessToken() {
        return accessToken.get();
    }

    public void clearLastApiCallDebugInfo() {
        lastApiCallDebugInfo.remove();
    }

    public FacebookApiCallDebugInfo consumeLastApiCallDebugInfo() {
        FacebookApiCallDebugInfo info = lastApiCallDebugInfo.get();
        if (info != null) {
            lastApiCallDebugInfo.remove();
        }
        return info;
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

    public String createPixel(String adAccountId, String name) {
        if (!hasText(adAccountId)) {
            throw new IllegalArgumentException("adAccountId must not be blank");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("access_token", requireAccessToken());
        String path = buildVersionedPath("/act_" + adAccountId + "/adspixels");
        JsonNode response = executePost(path, body);
        return response.path("id").asText();
    }

    public String fetchPixelCode(String pixelId) {
        if (!hasText(pixelId)) {
            return null;
        }
        String path = UriComponentsBuilder.fromPath(buildVersionedPath("/" + pixelId))
            .queryParam("fields", "code")
            .queryParam("access_token", requireAccessToken())
            .build()
            .toString();
        FacebookApiResponse response = executeGet(path);
        JsonNode body = response.body();
        if (body == null) {
            return null;
        }
        String code = body.path("code").asText(null);
        return hasText(code) ? code : null;
    }

    public void sendPurchaseEvent(String pixelId, String eventId, java.math.BigDecimal value, String currency, java.time.Instant eventTime) {
        if (!hasText(pixelId)) {
            throw new IllegalArgumentException("pixelId must not be blank");
        }
        Map<String, Object> customData = new HashMap<>();
        if (value != null) {
            customData.put("value", value);
        }
        if (hasText(currency)) {
            customData.put("currency", currency.trim().toUpperCase(Locale.ROOT));
        }
        Map<String, Object> event = new HashMap<>();
        event.put("event_name", "Purchase");
        event.put("event_time", (eventTime != null ? eventTime : java.time.Instant.now()).getEpochSecond());
        if (hasText(eventId)) {
            event.put("event_id", eventId);
        }
        event.put("custom_data", customData);

        Map<String, Object> body = new HashMap<>();
        body.put("data", List.of(event));
        body.put("access_token", requireAccessToken());

        String path = buildVersionedPath("/" + pixelId + "/events");
        executePost(path, body);
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
        String bidStrategy = request.bidStrategy();
        String bidAmount = request.bidAmount();

        if (bidStrategy != null && !bidStrategy.isBlank()) {
            body.put("bid_strategy", bidStrategy);
        }

        boolean isLowestCostWithoutCap = bidStrategy != null
            && bidStrategy.equalsIgnoreCase("LOWEST_COST_WITHOUT_CAP");

        if (bidAmount != null && !bidAmount.isBlank()) {
            if (isLowestCostWithoutCap || bidStrategy == null || bidStrategy.isBlank()) {
                LOGGER.warn(
                    "Ignoring bid_amount for ad set '{}' because the lowest cost strategy does not accept manual bids",
                    request.name()
                );
            } else {
                body.put("bid_amount", bidAmount);
            }
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
        List<String> targetingErrors = new ArrayList<>();
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
        mergeTargetingOptions(targeting, request.targetingOptions());
        normalizeInterests(targeting, targetingErrors);
        normalizeWorkPositions(targeting, targetingErrors);
        normalizeBehaviors(targeting, targetingErrors);
        normalizeCustomAudiences(targeting);
        normalizeGeoLocations(targeting);

        if (!targetingErrors.isEmpty()) {
            throw new TargetingNormalizationException(targetingErrors);
        }

        forceBrazilWideTargeting(targeting);
        enableAdvantageAudience(targeting);

        return targeting;
    }

    private void enableAdvantageAudience(Map<String, Object> targeting) {
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

        automation.put("advantage_audience", 1);
        targeting.put("targeting_automation", automation);
    }

    private void forceBrazilWideTargeting(Map<String, Object> targeting) {
        if (targeting == null) {
            return;
        }

        Map<String, Object> brazilGeoLocations = new HashMap<>();
        Object existingGeo = targeting.get("geo_locations");
        if (existingGeo instanceof Map<?, ?> geoMap && !geoMap.isEmpty()) {
            brazilGeoLocations.putAll(copyGeoMapWithStringKeys(geoMap));
        }

        brazilGeoLocations.put("countries", List.of(BRAZIL_COUNTRY_CODE));
        targeting.put("geo_locations", brazilGeoLocations);
    }

    private static final Map<String, Integer> LOCALE_CODE_TO_ID = Map.of(
        "PT_BR",
        16,
        "PT_PT",
        86,
        "ES_LA",
        24,
        "ES_ES",
        23,
        "EN_US",
        6,
        "EN_GB",
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
            .map(this::resolveLocaleValueForLanguages)
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


    private Integer resolveLocaleValueForLanguages(Object value) {
        Integer resolved = resolveLocaleValue(value);
        if (resolved == null) {
            return null;
        }

        if (value instanceof String && !LOCALE_CODE_TO_ID.containsValue(resolved)) {
            LOGGER.warn("Ignoring unsupported locale code '{}' in targeting.languages", value);
            return null;
        }

        return resolved;
    }

    public FacebookInterest lookupClosestInterest(String interestName) {
        if (!hasText(interestName)) {
            return null;
        }

        try {
            return fetchInterestFromFacebook(interestName.trim());
        } catch (Exception ex) {
            LOGGER.warn(
                "Unexpected error while looking up Facebook interest '{}': message={}",
                interestName,
                ex.getMessage(),
                ex
            );
            return null;
        }
    }

    public List<FacebookTargetingSearchResult> searchTargetingOptions(TargetingSearchRequest request) {
        if (request == null || request.type() == null || !hasText(request.query())) {
            return Collections.emptyList();
        }
        String normalizedQuery = request.query().trim();
        if (!hasText(normalizedQuery)) {
            return Collections.emptyList();
        }
        int limit = Math.max(1, request.limit());
        String effectiveAdAccountId = resolveAdAccountId(request.adAccountId());
        if (!hasText(effectiveAdAccountId)) {
            LOGGER.warn("Cannot request targeting search without ad account id");
            return Collections.emptyList();
        }
        TargetingSearchCacheKey cacheKey = new TargetingSearchCacheKey(
            request.type(),
            normalizedQuery.toLowerCase(Locale.ROOT),
            normalizeCacheKeyValue(request.locale()),
            normalizeCacheKeyValue(request.country()),
            normalizeCacheKeyValue(effectiveAdAccountId),
            limit
        );
        CachedTargetingSearchResults cached = targetingSearchCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.results();
        }
        TargetingSearchRequest normalizedRequest = new TargetingSearchRequest(
            request.type(),
            normalizedQuery,
            effectiveAdAccountId,
            request.locale(),
            request.country(),
            limit
        );
        List<FacebookTargetingSearchResult> results = executeTargetingSearch(normalizedRequest);
        List<FacebookTargetingSearchResult> immutableResults = results == null ? Collections.emptyList() : List.copyOf(results);
        targetingSearchCache.put(cacheKey, new CachedTargetingSearchResults(immutableResults, Instant.now().plus(targetingSearchCacheTtl)));
        return immutableResults;
    }

    private List<FacebookTargetingSearchResult> executeTargetingSearch(TargetingSearchRequest request) {
        if (request == null || !hasText(request.adAccountId())) {
            LOGGER.warn("Cannot execute targeting search without ad account id");
            return Collections.emptyList();
        }
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromPath(buildVersionedPath("/" + request.adAccountId() + "/targetingsearch"))
            .queryParam("type", request.type().graphType())
            .queryParam("q", request.query())
            .queryParam("limit", request.limit())
            .queryParam("fields", "id,name,audience_size_lower_bound,audience_size_upper_bound,path,description,topic")
            .queryParam("access_token", requireAccessToken());
        if (hasText(request.locale())) {
            builder.queryParam("locale", request.locale());
        }
        if (hasText(request.country())) {
            builder.queryParam("country", request.country());
        }
        String pathValue = builder.build(false).toUriString();
        try {
            FacebookApiResponse response = executeGet(pathValue);
            JsonNode body = response != null ? response.body() : null;
            JsonNode data = body != null ? body.path("data") : null;
            if (data == null || !data.isArray()) {
                return Collections.emptyList();
            }
            List<FacebookTargetingSearchResult> results = new ArrayList<>();
            for (JsonNode node : data) {
                if (node == null || node.isNull()) {
                    continue;
                }
                String id = node.path("id").asText(null);
                if (!hasText(id)) {
                    continue;
                }
                String name = node.path("name").asText(null);
                Long audienceSizeLower = node.hasNonNull("audience_size_lower_bound") ? node.path("audience_size_lower_bound").asLong() : null;
                Long audienceSizeUpper = node.hasNonNull("audience_size_upper_bound") ? node.path("audience_size_upper_bound").asLong() : null;
                List<String> hierarchy = parseTargetingPath(node.path("path"));
                results.add(new FacebookTargetingSearchResult(
                    id.trim(),
                    hasText(name) ? name.trim() : null,
                    audienceSizeLower,
                    audienceSizeUpper,
                    hierarchy
                ));
            }
            return results;
        } catch (FacebookAccessTokenExpiredException | FacebookPermissionException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            LOGGER.warn(
                "Facebook targeting search failed for type={} and term '{}': {}",
                request.type().graphType(),
                request.query(),
                ex.getMessage(),
                ex
            );
            return Collections.emptyList();
        }
    }

    private List<String> parseTargetingPath(JsonNode pathNode) {
        if (pathNode == null || !pathNode.isArray() || pathNode.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> pathValues = new ArrayList<>();
        for (JsonNode element : pathNode) {
            if (element == null || element.isNull()) {
                continue;
            }
            String value = element.asText(null);
            if (hasText(value)) {
                pathValues.add(value.trim());
            }
        }
        return pathValues.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(pathValues);
    }

    private String normalizeCacheKeyValue(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeAdAccountId(String adAccountId) {
        if (!hasText(adAccountId)) {
            return null;
        }
        String trimmed = adAccountId.trim();
        if (trimmed.regionMatches(true, 0, "act_", 0, 4)) {
            return "act_" + trimmed.substring(4);
        }
        if (trimmed.chars().allMatch(Character::isDigit)) {
            return "act_" + trimmed;
        }
        return trimmed;
    }

    private String resolveAdAccountId(String adAccountId) {
        String normalized = normalizeAdAccountId(adAccountId);
        if (hasText(normalized)) {
            return normalized;
        }
        return defaultAdAccountId;
    }

    private void mergeTargetingOptions(Map<String, Object> targeting, List<TargetingOption> targetingOptions) {
        if (targeting == null || CollectionUtils.isEmpty(targetingOptions)) {
            return;
        }
        Map<TargetingCandidateType, List<Map<String, Object>>> grouped = new EnumMap<>(TargetingCandidateType.class);
        Map<TargetingCandidateType, Set<String>> seen = new EnumMap<>(TargetingCandidateType.class);
        for (TargetingOption option : targetingOptions) {
            if (option == null || !hasText(option.facebookId())) {
                continue;
            }
            TargetingCandidateType type = option.type();
            if (type == null) {
                continue;
            }
            Set<String> dedup = seen.computeIfAbsent(type, key -> new HashSet<>());
            String facebookId = option.facebookId().trim();
            if (!dedup.add(facebookId)) {
                continue;
            }
            Map<String, Object> normalizedEntry = new HashMap<>();
            normalizedEntry.put("id", facebookId);
            if (hasText(option.name())) {
                normalizedEntry.put("name", option.name().trim());
            }
            grouped.computeIfAbsent(type, key -> new ArrayList<>()).add(normalizedEntry);
        }

        if (grouped.isEmpty()) {
            return;
        }

        targeting.remove("interests");
        targeting.remove("behaviors");
        targeting.remove("work_positions");

        grouped.forEach((type, values) -> {
            String field = switch (type) {
                case BEHAVIOR -> "behaviors";
                case WORK_POSITION -> "work_positions";
                case INTEREST -> "interests";
            };
            targeting.put(field, values);
        });
    }

    private void normalizeInterests(Map<String, Object> targeting, List<String> targetingErrors) {
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
                String id = extractTargetingId(interestMap.get("id"));
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
                    targetingErrors.add("interesse não encontrado na API da Meta: '" + interestName + "'");
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

    private void normalizeWorkPositions(Map<String, Object> targeting, List<String> targetingErrors) {
        normalizeTargetingCategoryList(targeting, "work_positions", TargetingCategoryClass.JOB_TITLE, targetingErrors);
    }

    private void normalizeBehaviors(Map<String, Object> targeting, List<String> targetingErrors) {
        normalizeTargetingCategoryList(targeting, "behaviors", TargetingCategoryClass.BEHAVIOR, targetingErrors);
    }

    private void normalizeTargetingCategoryList(
        Map<String, Object> targeting,
        String fieldName,
        TargetingCategoryClass categoryClass,
        List<String> targetingErrors
    ) {
        if (targeting == null || targeting.isEmpty() || categoryClass == null) {
            return;
        }
        Object rawValue = targeting.get(fieldName);
        if (!(rawValue instanceof List<?> values) || values.isEmpty()) {
            return;
        }

        List<Map<String, Object>> normalized = new ArrayList<>();
        boolean needsUpdate = false;

        for (Object value : values) {
            if (value instanceof Map<?, ?> valueMap) {
                String id = extractTargetingId(valueMap.get("id"));
                if (!hasText(id)) {
                    id = extractTargetingId(valueMap.get("key"));
                }
                String name = extractInterestName(valueMap.get("name"));
                if (!hasText(id) && hasText(name)) {
                    id = resolveTargetingCategoryId(name, categoryClass);
                    if (hasText(id)) {
                        needsUpdate = true;
                    }
                }
                if (!hasText(id)) {
                    needsUpdate = true;
                    LOGGER.warn(
                        "Skipping Facebook targeting entry in '{}' because no id/key could be resolved: {}",
                        fieldName,
                        JsonLogFormatter.wrap(valueMap)
                    );
                    addTargetingError(targetingErrors, fieldName, valueMap);
                    continue;
                }
                Map<String, Object> normalizedEntry = new HashMap<>();
                normalizedEntry.put("id", id);
                if (hasText(name)) {
                    normalizedEntry.put("name", name);
                }
                if (!valueMap.equals(normalizedEntry)) {
                    needsUpdate = true;
                }
                normalized.add(normalizedEntry);
            } else if (value instanceof String stringValue) {
                String id = resolveTargetingCategoryId(stringValue, categoryClass);
                if (!hasText(id)) {
                    needsUpdate = true;
                    LOGGER.warn(
                        "Skipping Facebook targeting entry '{}' in '{}' because no ID was resolved",
                        stringValue,
                        fieldName
                    );
                    addTargetingError(targetingErrors, fieldName, stringValue);
                    continue;
                }
                Map<String, Object> normalizedEntry = new HashMap<>();
                normalizedEntry.put("id", id);
                normalizedEntry.put("name", stringValue.trim());
                normalized.add(normalizedEntry);
                needsUpdate = true;
            } else if (value instanceof Number numberValue) {
                String id = numberValue.toString();
                if (!hasText(id)) {
                    needsUpdate = true;
                    continue;
                }
                Map<String, Object> normalizedEntry = new HashMap<>();
                normalizedEntry.put("id", id);
                normalized.add(normalizedEntry);
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

    private void addTargetingError(List<String> targetingErrors, String fieldName, Object value) {
        if (targetingErrors == null) {
            return;
        }
        String humanField = switch (fieldName) {
            case "work_positions" -> "cargo";
            case "behaviors" -> "comportamento";
            case "interests" -> "interesse";
            default -> fieldName;
        };
        targetingErrors.add(humanField + " não encontrado na API da Meta: " + String.valueOf(value));
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
            return extractTargetingIdFromText(stringValue);
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

    private String extractTargetingId(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number numberValue) {
            String id = numberValue.toString();
            return hasText(id) ? id : null;
        }
        if (value instanceof String stringValue) {
            return extractTargetingIdFromText(stringValue);
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

    private String extractTargetingIdFromText(String value) {
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

        String directId = extractTargetingIdFromText(normalizedName);
        if (hasText(directId)) {
            return directId;
        }

        String cacheKey = normalizedName.toLowerCase(Locale.ROOT);
        String cached = interestIdCache.get(cacheKey);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }

        FacebookInterest resolved = fetchInterestFromFacebook(normalizedName);
        String resolvedId = resolved != null ? resolved.id() : null;
        interestIdCache.put(cacheKey, resolvedId != null ? resolvedId : "");
        return resolvedId;
    }

    private String resolveTargetingCategoryId(String value, TargetingCategoryClass categoryClass) {
        if (!hasText(value) || categoryClass == null) {
            return null;
        }
        String normalizedName = value.trim();
        if (!hasText(normalizedName)) {
            return null;
        }

        String directId = extractTargetingIdFromText(normalizedName);
        if (hasText(directId)) {
            return directId;
        }

        TargetingCategoryCacheKey cacheKey = new TargetingCategoryCacheKey(
            categoryClass.apiClassName(),
            normalizedName.toLowerCase(Locale.ROOT)
        );
        String cached = targetingCategoryIdCache.get(cacheKey);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }

        FacebookTargetingCategory resolved = fetchTargetingCategoryFromFacebook(normalizedName, categoryClass);
        String resolvedId = resolved != null ? resolved.id() : null;
        targetingCategoryIdCache.put(cacheKey, hasText(resolvedId) ? resolvedId : "");
        return resolvedId;
    }

    private FacebookTargetingCategory fetchTargetingCategoryFromFacebook(
        String query,
        TargetingCategoryClass categoryClass
    ) {
        String normalizedAccount = resolveAdAccountId(null);
        if (!hasText(normalizedAccount)) {
            LOGGER.warn("Cannot look up targeting category '{}' because ad account id is not configured", query);
            return null;
        }
        String path = UriComponentsBuilder
            .fromPath(buildVersionedPath("/" + normalizedAccount + "/targetingsearch"))
            .queryParam("type", "adTargetingCategory")
            .queryParam("class", categoryClass.apiClassName())
            .queryParam("q", query)
            .queryParam("limit", 5)
            .queryParam("access_token", requireAccessToken())
            .build(false)
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
            FacebookTargetingCategory fallback = null;
            for (JsonNode node : data) {
                if (node == null || node.isNull()) {
                    continue;
                }
                String id = node.path("id").asText(null);
                if (!hasText(id)) {
                    continue;
                }
                String name = node.path("name").asText(null);
                FacebookTargetingCategory candidate = new FacebookTargetingCategory(
                    id.trim(),
                    hasText(name) ? name.trim() : null
                );
                if (name != null && name.trim().equalsIgnoreCase(query)) {
                    return candidate;
                }
                if (fallback == null) {
                    fallback = candidate;
                }
            }
            return fallback;
        } catch (FacebookAccessTokenExpiredException | FacebookPermissionException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            LOGGER.warn(
                "Facebook targeting search failed for class '{}' and query '{}': {}",
                categoryClass.apiClassName(),
                query,
                ex.getMessage(),
                ex
            );
            return null;
        }
    }


private FacebookInterest fetchInterestFromFacebook(String interestName) {
    FacebookInterest resolved = searchInterest(interestName, INTEREST_SEARCH_LOCALE);
    if (resolved == null && !"en_US".equalsIgnoreCase(INTEREST_SEARCH_LOCALE)) {
        resolved = searchInterest(interestName, "en_US");
    }
    if (resolved == null) {
        resolved = searchInterest(interestName, null);
    }
    return resolved;
}

private FacebookInterest searchInterest(String interestName, String locale) {
    String normalizedAccount = resolveAdAccountId(null);
    if (!hasText(normalizedAccount)) {
        LOGGER.warn("Cannot look up Facebook interest '{}' because no ad account id is configured", interestName);
        return null;
    }
    UriComponentsBuilder builder = UriComponentsBuilder
        .fromPath(buildVersionedPath("/" + normalizedAccount + "/targetingsearch"))
        .queryParam("type", "adinterest")
        .queryParam("q", interestName)
        .queryParam("limit", 1)
        .queryParam("fields", "id,name")
        .queryParam("access_token", requireAccessToken());
    if (hasText(locale)) {
        builder.queryParam("locale", locale);
    }
    String path = builder.build(false).toUriString();

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
                String name = node.path("name").asText(null);
                String trimmedName = hasText(name) ? name.trim() : null;
                return new FacebookInterest(id.trim(), trimmedName);
            }
        }
    } catch (WebClientResponseException ex) {
        LOGGER.warn(
            "Facebook interest lookup failed for '{}' (locale={}): status={}, message={}",
            interestName,
            locale,
            ex.getRawStatusCode(),
            ex.getMessage()
        );
    } catch (WebClientRequestException ex) {
        LOGGER.warn(
            "Facebook interest lookup failed for '{}' (locale={}): message={}",
            interestName,
            locale,
            ex.getMessage()
        );
    } catch (Exception ex) {
        LOGGER.warn(
            "Unexpected error while looking up Facebook interest '{}' (locale={}): message={}",
            interestName,
            locale,
            ex.getMessage(),
            ex
        );
    }
    return null;
}

    public record FacebookTargetingSearchResult(
        String id,
        String name,
        Long audienceSizeLowerBound,
        Long audienceSizeUpperBound,
        List<String> path
    ) {
        public Long audienceSize() {
            if (audienceSizeUpperBound != null) {
                return audienceSizeUpperBound;
            }
            return audienceSizeLowerBound;
        }
    }

    public List<FacebookTargetingSuggestionResult> suggestTargetingOptions(TargetingSuggestionsRequest request) {
        if (request == null || CollectionUtils.isEmpty(request.seeds())) {
            return Collections.emptyList();
        }
        boolean onlyInterestSeeds = request.seeds().stream()
            .allMatch(seed -> TargetingSearchType.AD_INTEREST.graphType().equalsIgnoreCase(seed.type()));
        if (onlyInterestSeeds) {
            return suggestInterestSuggestions(request);
        }
        String normalizedAccount = resolveAdAccountId(request.adAccountId());
        if (!hasText(normalizedAccount)) {
            LOGGER.warn("Cannot request targeting suggestions without ad account id");
            return Collections.emptyList();
        }
        try {
            String targetingList = objectMapper.writeValueAsString(request.seeds());
            UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath(buildVersionedPath("/" + normalizedAccount + "/targetingsuggestions"))
                .queryParam("targeting_list", targetingList)
                .queryParam("limit", Math.max(1, request.limit()))
                .queryParam("access_token", requireAccessToken());
            if (hasText(request.locale())) {
                builder.queryParam("locale", request.locale());
            }
            if (hasText(request.country())) {
                builder.queryParam("country", request.country());
            }
            FacebookApiResponse response = executeGet(builder.build(false).toUriString());
            JsonNode data = response.body() != null ? response.body().path("data") : null;
            return parseTargetingSuggestions(data);
        } catch (FacebookAccessTokenExpiredException | FacebookPermissionException ex) {
            throw ex;
        } catch (Exception ex) {
            LOGGER.warn("Facebook targeting suggestions failed for account {}: {}", request.adAccountId(), ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    public record FacebookTargetingSuggestionResult(String id, String name, Long audienceSize, List<String> path) {}

    public record TargetingSuggestionsRequest(
        String adAccountId,
        List<TargetingSuggestionSeed> seeds,
        String locale,
        String country,
        int limit
    ) {}

    public record TargetingSuggestionSeed(String id, String type) {}

    public record TargetingSearchRequest(
        TargetingSearchType type,
        String query,
        String adAccountId,
        String locale,
        String country,
        int limit
    ) {}

    public record TargetingValidationRequest(String adAccountId, JsonNode targetingSpec) {}

    public record ReachEstimateRequest(String adAccountId, JsonNode targetingSpec) {}


    public enum TargetingSearchType {
        AD_INTEREST("adinterest"),
        AD_BEHAVIOR("adbehavior"),
        AD_WORK_POSITION("adworkposition");

        private final String graphType;

        TargetingSearchType(String graphType) {
            this.graphType = graphType;
        }

        public String graphType() {
            return graphType;
        }
    }

    public JsonNode validateTargetingSpec(TargetingValidationRequest request) {
        if (request == null || !hasText(request.adAccountId()) || request.targetingSpec() == null) {
            throw new IllegalArgumentException("targetingSpec e adAccountId são obrigatórios");
        }
        String normalizedAccount = normalizeAdAccountId(request.adAccountId());
        try {
            String targetingSpec = objectMapper.writeValueAsString(request.targetingSpec());
            String path = buildVersionedPath("/" + normalizedAccount + "/targetingvalidation");
            var builder = org.springframework.web.util.UriComponentsBuilder.fromPath(path)
                    .queryParam("targeting_spec", targetingSpec)
                    .queryParam("access_token", requireAccessToken());
            FacebookApiResponse response = executeGet(builder.build(false).toUriString());
            return response.body();
        } catch (FacebookAccessTokenExpiredException | FacebookPermissionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao validar targeting spec: " + ex.getMessage(), ex);
        }
    }

    public JsonNode estimateReach(ReachEstimateRequest request) {
        if (request == null || !hasText(request.adAccountId()) || request.targetingSpec() == null) {
            throw new IllegalArgumentException("targetingSpec e adAccountId são obrigatórios");
        }
        String normalizedAccount = normalizeAdAccountId(request.adAccountId());
        try {
            String targetingSpec = objectMapper.writeValueAsString(request.targetingSpec());
            String path = buildVersionedPath("/" + normalizedAccount + "/reachestimate");
            var builder = org.springframework.web.util.UriComponentsBuilder.fromPath(path)
                    .queryParam("targeting_spec", targetingSpec)
                    .queryParam("access_token", requireAccessToken());
            FacebookApiResponse response = executeGet(builder.build(false).toUriString());
            return response.body();
        } catch (FacebookAccessTokenExpiredException | FacebookPermissionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao estimar alcance: " + ex.getMessage(), ex);
        }
    }

    private record TargetingSearchCacheKey(
        TargetingSearchType type,
        String query,
        String locale,
        String country,
        String adAccountId,
        int limit
    ) {}

    private record CachedTargetingSearchResults(
        List<FacebookTargetingSearchResult> results,
        Instant expiresAt
    ) {
        boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }
    }

    private List<FacebookTargetingSuggestionResult> suggestInterestSuggestions(TargetingSuggestionsRequest request) {
        List<String> interestSeeds = request.seeds().stream()
            .map(TargetingSuggestionSeed::id)
            .filter(value -> hasText(value))
            .map(String::trim)
            .distinct()
            .toList();
        if (interestSeeds.isEmpty()) {
            LOGGER.warn("Cannot request interest suggestions without seed terms");
            return Collections.emptyList();
        }
        try {
            String interestList = objectMapper.writeValueAsString(interestSeeds);
            UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath(buildVersionedPath("/search"))
                .queryParam("type", "adinterestsuggestion")
                .queryParam("interest_list", interestList)
                .queryParam("limit", Math.max(1, request.limit()))
                .queryParam("access_token", requireAccessToken());
            if (hasText(request.locale())) {
                builder.queryParam("locale", request.locale());
            }
            FacebookApiResponse response = executeGet(builder.build(false).toUriString());
            JsonNode data = response.body() != null ? response.body().path("data") : null;
            return parseTargetingSuggestions(data);
        } catch (FacebookAccessTokenExpiredException | FacebookPermissionException ex) {
            throw ex;
        } catch (Exception ex) {
            LOGGER.warn("Facebook interest suggestions failed: {}", ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    private List<FacebookTargetingSuggestionResult> parseTargetingSuggestions(JsonNode data) {
        if (data == null || !data.isArray()) {
            return Collections.emptyList();
        }
        List<FacebookTargetingSuggestionResult> results = new ArrayList<>();
        for (JsonNode node : data) {
            if (node == null || node.isNull()) {
                continue;
            }
            String id = node.path("id").asText(null);
            if (!hasText(id)) {
                continue;
            }
            String name = node.path("name").asText(null);
            Long audience = node.hasNonNull("audience_size") ? node.path("audience_size").asLong() : null;
            List<String> pathValues = parseTargetingPath(node.path("path"));
            results.add(new FacebookTargetingSuggestionResult(id.trim(), hasText(name) ? name.trim() : null, audience, pathValues));
        }
        return results;
    }

    public record FacebookInterest(String id, String name) {}
    private record FacebookTargetingCategory(String id, String name) {}

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

    public JsonNode getCampaignInsights(String campaignId, Map<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/" + campaignId + "/insights")
            .queryParam("access_token", requireAccessToken());
        if (queryParams != null) {
            queryParams.forEach((key, value) -> {
                if (hasText(value)) {
                    builder.queryParam(key, value);
                }
            });
        }
        String path = buildVersionedPath(builder.toUriString());
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
        Instant startedAt = Instant.now();
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
            recordApiCallDebugInfo(
                "POST",
                path,
                toJsonString(body),
                toJsonString(nonNullResponse.body()),
                nonNullResponse.statusCode() != null ? nonNullResponse.statusCode().value() : null,
                null,
                startedAt,
                Instant.now()
            );
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
            recordApiCallDebugInfo(
                "POST",
                path,
                toJsonString(body),
                responseBody,
                ex.getRawStatusCode(),
                ex.getMessage(),
                startedAt,
                Instant.now()
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
            recordApiCallDebugInfo(
                "POST",
                path,
                toJsonString(body),
                null,
                null,
                ex.getMessage(),
                startedAt,
                Instant.now()
            );
            throw ex;
        }
    }

    private FacebookApiResponse executeGet(String path) {
        Instant startedAt = Instant.now();
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
            recordApiCallDebugInfo(
                "GET",
                path,
                null,
                toJsonString(nonNullResponse.body()),
                nonNullResponse.statusCode() != null ? nonNullResponse.statusCode().value() : null,
                null,
                startedAt,
                Instant.now()
            );
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
            recordApiCallDebugInfo(
                "GET",
                path,
                null,
                responseBody,
                ex.getRawStatusCode(),
                ex.getMessage(),
                startedAt,
                Instant.now()
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
            recordApiCallDebugInfo(
                "GET",
                path,
                null,
                null,
                null,
                ex.getMessage(),
                startedAt,
                Instant.now()
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
        List<TargetingOption> targetingOptions
    ) {
        public AdSetRequest {
            if (targetingOptions == null) {
                targetingOptions = Collections.emptyList();
            }
        }
    }


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

    public record TargetingOption(
        String facebookId,
        String name,
        TargetingCandidateType type,
        Long audienceSize
    ) {}

    public record TokenRenewalResponse(String accessToken, long expiresInSeconds) {}

    private void recordApiCallDebugInfo(String method,
                                        String path,
                                        String requestBody,
                                        String responseBody,
                                        Integer statusCode,
                                        String errorMessage,
                                        Instant requestedAt,
                                        Instant respondedAt) {
        lastApiCallDebugInfo.set(new FacebookApiCallDebugInfo(
            method,
            path,
            requestBody,
            responseBody,
            statusCode,
            errorMessage,
            requestedAt,
            respondedAt
        ));
    }

    private String toJsonString(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof String str) {
                return str;
            }
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            LOGGER.warn("Failed to serialize value for Facebook API debug info: {}", ex.getMessage());
            return null;
        }
    }

    private record FacebookApiResponse(HttpStatusCode statusCode, HttpHeaders headers, JsonNode body) {}

    public record FacebookApiCallDebugInfo(
            String httpMethod,
            String endpoint,
            String requestBody,
            String responseBody,
            Integer statusCode,
            String errorMessage,
            Instant requestedAt,
            Instant respondedAt
    ) {}

    public static class TargetingNormalizationException extends RuntimeException {
        private final List<String> errors;

        public TargetingNormalizationException(List<String> errors) {
            super(errors != null && !errors.isEmpty() ? String.join("; ", errors) : "Targeting normalization failed");
            this.errors = errors != null ? List.copyOf(errors) : List.of();
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    private enum TargetingCategoryClass {
        JOB_TITLE("job_title"),
        BEHAVIOR("behaviors");

        private final String apiClassName;

        TargetingCategoryClass(String apiClassName) {
            this.apiClassName = apiClassName;
        }

        public String apiClassName() {
            return apiClassName;
        }
    }

    private record TargetingCategoryCacheKey(String categoryClass, String query) {}
}
