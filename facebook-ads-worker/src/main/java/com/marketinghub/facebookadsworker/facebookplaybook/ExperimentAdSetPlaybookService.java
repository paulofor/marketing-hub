package com.marketinghub.facebookadsworker.facebookplaybook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.FacebookAdsService.FacebookTargetingSearchResult;
import com.marketinghub.facebookadsworker.FacebookAdsService.FacebookTargetingSuggestionResult;
import com.marketinghub.facebookadsworker.FacebookAdsService.TargetingSearchRequest;
import com.marketinghub.facebookadsworker.FacebookAdsService.TargetingSearchType;
import com.marketinghub.facebookadsworker.FacebookAdsService.TargetingSuggestionsRequest;
import com.marketinghub.facebookadsworker.FacebookAdsService.TargetingSuggestionSeed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Executes Facebook-specific jobs from the playbook queue.
 */
@Service
public class ExperimentAdSetPlaybookService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentAdSetPlaybookService.class);
    private static final List<String> DISCOVERY_ANCHOR_BLOCKLIST = List.of(
            "friends of",
            "amigos de",
            "birthday",
            "aniversário",
            "expat",
            "expatriado",
            "canvas gaming"
    );

    private static final List<TargetingSearchType> DISCOVERY_CATALOG_TYPES = List.of(
            TargetingSearchType.AD_INTEREST,
            TargetingSearchType.AD_WORK_POSITION,
            TargetingSearchType.AD_BEHAVIOR,
            TargetingSearchType.AD_INDUSTRY,
            TargetingSearchType.AD_WORK_EMPLOYER
    );

    private final ExperimentAdSetPlaybookClient client;
    private final FacebookAdsService facebookAdsService;
    private final ObjectMapper objectMapper;
    private final String workerId;

    public ExperimentAdSetPlaybookService(ExperimentAdSetPlaybookClient client,
                                          FacebookAdsService facebookAdsService,
                                          ObjectMapper objectMapper,
                                          @Value("${facebook.playbook.worker-id:}") String configuredWorkerId) {
        this.client = client;
        this.facebookAdsService = facebookAdsService;
        this.objectMapper = objectMapper;
        this.workerId = configuredWorkerId != null && !configuredWorkerId.isBlank()
                ? configuredWorkerId
                : buildWorkerId();
    }

    public void processQueue() {
        List<PlaybookJob> jobs = client.claimJobs(workerId, 5);
        if (jobs.isEmpty()) {
            return;
        }
        LOGGER.info("Facebook worker processando {} jobs do playbook", jobs.size());
        for (PlaybookJob job : jobs) {
            List<ApiCallLog> apiLogs = new ArrayList<>();
            try {
                JsonNode result = switch (job.type()) {
                    case FACEBOOK_SEED_LOOKUP -> handleSeedLookup(job.payload(), apiLogs);
                    case FACEBOOK_TARGETING_SUGGESTIONS -> handleSuggestions(job.payload(), apiLogs);
                    case FACEBOOK_SOCIAL_POSITIONS -> handlePositions(job.payload(), apiLogs);
                    case FACEBOOK_VALIDATE_SPEC -> handleValidation(job.payload(), apiLogs);
                    case FACEBOOK_REACH_ESTIMATE -> handleReachEstimate(job.payload(), apiLogs);
                    default -> {
                        LOGGER.warn("Job {} do tipo {} não é processado pelo worker do Facebook", job.id(), job.type());
                        yield objectMapper.createObjectNode();
                    }
                };
                client.completeJob(job.id(), result, buildApiCallPayloads(apiLogs));
            } catch (Exception ex) {
                LOGGER.error("Erro ao executar job {} do tipo {}", job.id(), job.type(), ex);
                client.failJob(job.id(), ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido", buildApiCallPayloads(apiLogs));
            }
        }
    }

    private JsonNode handleSeedLookup(JsonNode payload, List<ApiCallLog> apiLogs) {
        DiscoveryPlan plan = DiscoveryPlan.fromPayload(payload);
        DiscoveryAccumulator accumulator = new DiscoveryAccumulator(objectMapper);
        for (DiscoverySeed seed : plan.seeds()) {
            for (String locale : plan.locales()) {
                for (String variant : seed.variants()) {
                    TargetingSearchRequest discoveryRequest = new TargetingSearchRequest(
                            TargetingSearchType.ANY,
                            variant,
                            plan.adAccountId(),
                            locale,
                            plan.country(),
                            plan.limit()
                    );
                    List<FacebookTargetingSearchResult> discoveryResults = recordFacebookCall(
                            apiLogs,
                            "/targetingsearch",
                            "GET",
                            discoveryRequest,
                            () -> facebookAdsService.searchTargetingOptions(discoveryRequest),
                            response -> toJsonNode(response)
                    );
                    accumulator.addCall(seed.original(), variant, locale, "targetingsearch", discoveryResults);
                }
                for (TargetingSearchType type : plan.catalogTypes()) {
                    for (String variant : seed.variants()) {
                        TargetingSearchRequest catalogRequest = new TargetingSearchRequest(
                                type,
                                variant,
                                plan.adAccountId(),
                                locale,
                                plan.country(),
                                plan.limit()
                        );
                        List<FacebookTargetingSearchResult> catalogResults = recordFacebookCall(
                                apiLogs,
                                "/search",
                                "GET",
                                catalogRequest,
                                () -> facebookAdsService.searchGlobalTargetingOptions(catalogRequest),
                                response -> toJsonNode(response)
                        );
                        accumulator.addCall(seed.original(), variant, locale, "search:" + type.graphType(), catalogResults);
                    }
                }
            }
        }
        ObjectNode result = accumulator.build(plan);
        FacebookTargetingSearchResult anchor = accumulator.bestInterest();
        if (anchor == null || !StringUtils.hasText(anchor.id())) {
            throw new IllegalStateException("Nenhum interesse relevante encontrado para o discovery");
        }
        result.put("interestId", anchor.id());
        result.put("interestName", anchor.name());
        if (anchor.audienceSizeLowerBound() != null) {
            result.put("audienceLowerBound", anchor.audienceSizeLowerBound());
        }
        if (anchor.audienceSizeUpperBound() != null) {
            result.put("audienceUpperBound", anchor.audienceSizeUpperBound());
        } else if (anchor.audienceSizeLowerBound() != null) {
            result.put("audienceUpperBound", anchor.audienceSizeLowerBound());
        }
        return result;
    }

    private JsonNode handleSuggestions(JsonNode payload, List<ApiCallLog> apiLogs) {
        String seedInterestId = text(payload, "seedInterestId");
        String locale = text(payload, "locale", "pt_BR");
        String country = text(payload, "country", "BR");
        String adAccountId = text(payload, "adAccountId", null);
        int limit = payload.path("limit").asInt(100);
        TargetingSuggestionsRequest request = new TargetingSuggestionsRequest(
                adAccountId,
                List.of(new TargetingSuggestionSeed(seedInterestId, TargetingSearchType.AD_INTEREST.graphType())),
                locale,
                country,
                limit
        );
        List<FacebookTargetingSuggestionResult> suggestions = recordFacebookCall(
                apiLogs,
                "/targetingsuggestions",
                "GET",
                request,
                () -> facebookAdsService.suggestTargetingOptions(request),
                response -> toJsonNode(response)
        );
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode items = objectMapper.createArrayNode();
        for (FacebookTargetingSuggestionResult suggestion : suggestions) {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("id", suggestion.id());
            item.put("name", suggestion.name());
            item.put("type", "INTEREST");
            if (suggestion.audienceSize() != null) {
                item.put("audienceSize", suggestion.audienceSize());
            }
            ArrayNode path = objectMapper.createArrayNode();
            if (suggestion.path() != null) {
                suggestion.path().forEach(path::add);
            }
            item.set("path", path);
            items.add(item);
        }
        result.set("items", items);
        return result;
    }

    private JsonNode handlePositions(JsonNode payload, List<ApiCallLog> apiLogs) {
        ArrayNode queries = payload != null && payload.has("queries") && payload.get("queries").isArray()
                ? (ArrayNode) payload.get("queries")
                : objectMapper.createArrayNode();
        String locale = text(payload, "locale", "pt_BR");
        String adAccountId = text(payload, "adAccountId", null);
        Set<String> seen = new LinkedHashSet<>();
        List<FacebookTargetingSearchResult> collected = new ArrayList<>();
        for (JsonNode node : queries) {
            String query = node.asText(null);
            if (!StringUtils.hasText(query)) {
                continue;
            }
            TargetingSearchRequest request = new TargetingSearchRequest(
                    TargetingSearchType.AD_WORK_POSITION,
                    query,
                    adAccountId,
                    locale,
                    null,
                    payload.path("limit").asInt(25)
            );
            List<FacebookTargetingSearchResult> results = recordFacebookCall(
                    apiLogs,
                    "/targetingsearch",
                    "GET",
                    request,
                    () -> facebookAdsService.searchTargetingOptions(request),
                    response -> toJsonNode(response)
            );
            results.forEach(result -> {
                if (seen.add(result.id())) {
                    collected.add(result);
                }
            });
        }
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode items = objectMapper.createArrayNode();
        collected.stream().limit(6).forEach(option -> {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("id", option.id());
            item.put("name", option.name());
            items.add(item);
        });
        result.set("items", items);
        return result;
    }

    private JsonNode handleValidation(JsonNode payload, List<ApiCallLog> apiLogs) {
        String adAccountId = text(payload, "adAccountId", null);
        JsonNode targeting = payload.path("targetingSpec");
        FacebookAdsService.TargetingValidationRequest request =
                new FacebookAdsService.TargetingValidationRequest(adAccountId, targeting);
        JsonNode apiResponse = recordFacebookCall(
                apiLogs,
                "/" + normalizeAdAccountId(adAccountId) + "/targetingvalidation",
                "GET",
                buildTargetingValidationPayload(targeting),
                () -> facebookAdsService.validateTargetingSpec(request),
                Function.identity());
        boolean isValid = apiResponse != null
                && apiResponse.path("data").isArray()
                && apiResponse.path("data").size() > 0
                && apiResponse.path("data").get(0).path("is_valid").asBoolean(true);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("status", isValid ? "VALID" : "INVALID");
        result.set("details", apiResponse);
        return result;
    }

    private JsonNode handleReachEstimate(JsonNode payload, List<ApiCallLog> apiLogs) {
        String adAccountId = text(payload, "adAccountId", null);
        JsonNode targeting = payload.path("targetingSpec");
        FacebookAdsService.ReachEstimateRequest request =
                new FacebookAdsService.ReachEstimateRequest(adAccountId, targeting);
        JsonNode apiResponse = recordFacebookCall(
                apiLogs,
                "/" + normalizeAdAccountId(adAccountId) + "/reachestimate",
                "GET",
                buildTargetingValidationPayload(targeting),
                () -> facebookAdsService.estimateReach(request),
                Function.identity());
        JsonNode dataNode = apiResponse != null && apiResponse.path("data").isArray() && apiResponse.path("data").size() > 0
                ? apiResponse.path("data").get(0)
                : objectMapper.createObjectNode();
        ObjectNode result = objectMapper.createObjectNode();
        result.put("status", "READY");
        if (dataNode.has("users_lower_bound")) {
            result.put("usersLowerBound", dataNode.get("users_lower_bound").asLong());
        }
        if (dataNode.has("users_upper_bound")) {
            result.put("usersUpperBound", dataNode.get("users_upper_bound").asLong());
        }
        result.set("details", apiResponse);
        return result;
    }

    private String text(JsonNode node, String field) {
        return text(node, field, null);
    }

    private String text(JsonNode node, String field, String defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text : defaultValue;
    }

    private ObjectNode buildTargetingValidationPayload(JsonNode targetingSpec) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("targeting_spec", targetingSpec == null || targetingSpec.isNull()
                ? objectMapper.createObjectNode()
                : targetingSpec);
        return payload;
    }

    private String normalizeAdAccountId(String adAccountId) {
        if (!StringUtils.hasText(adAccountId)) {
            return "act_<missing>";
        }
        String trimmed = adAccountId.trim();
        return trimmed.startsWith("act_") ? trimmed : "act_" + trimmed;
    }

    private List<ExperimentAdSetPlaybookClient.ApiCallPayload> buildApiCallPayloads(List<ApiCallLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        return logs.stream()
                .map(log -> new ExperimentAdSetPlaybookClient.ApiCallPayload(
                        log.provider(),
                        log.endpoint(),
                        log.httpMethod(),
                        log.statusCode(),
                        log.requestPayload(),
                        log.responsePayload(),
                        log.errorMessage(),
                        log.requestedAt(),
                        log.respondedAt()
                ))
                .toList();
    }

    private <T> T recordFacebookCall(List<ApiCallLog> logs,
                                     String endpoint,
                                     String httpMethod,
                                     Object requestPayload,
                                     Supplier<T> executor,
                                     Function<T, JsonNode> responseMapper) {
        Instant startedAt = Instant.now();
        facebookAdsService.clearLastApiCallDebugInfo();
        try {
            T response = executor.get();
            JsonNode responsePayload = responseMapper != null ? responseMapper.apply(response) : null;
            appendApiCallLog(logs, endpoint, httpMethod, requestPayload, responsePayload, null, null, startedAt);
            return response;
        } catch (RuntimeException ex) {
            appendApiCallLog(logs, endpoint, httpMethod, requestPayload, null, null, ex.getMessage(), startedAt);
            throw ex;
        }
    }

    private void appendApiCallLog(List<ApiCallLog> logs,
                                  String fallbackEndpoint,
                                  String httpMethod,
                                  Object requestPayload,
                                  JsonNode fallbackResponsePayload,
                                  Integer fallbackStatusCode,
                                  String errorMessage,
                                  Instant startedAt) {
        FacebookAdsService.FacebookApiCallDebugInfo debugInfo = facebookAdsService.consumeLastApiCallDebugInfo();
        if (debugInfo != null) {
            logs.add(new ApiCallLog(
                    "FACEBOOK",
                    debugInfo.endpoint(),
                    debugInfo.httpMethod(),
                    parseJsonPayload(debugInfo.requestBody()),
                    parseJsonPayload(debugInfo.responseBody()),
                    debugInfo.statusCode(),
                    debugInfo.errorMessage(),
                    debugInfo.requestedAt(),
                    debugInfo.respondedAt()
            ));
            return;
        }
        logs.add(new ApiCallLog(
                "FACEBOOK",
                fallbackEndpoint,
                httpMethod,
                toJsonNode(requestPayload),
                fallbackResponsePayload,
                fallbackStatusCode,
                errorMessage,
                startedAt,
                Instant.now()
        ));
    }

    private static class DiscoveryPlan {
        private static final int MAX_SEEDS = 8;
        private static final int MAX_LOCALES = 4;
        private static final int DEFAULT_LIMIT = 200;

        private final List<DiscoverySeed> seeds;
        private final List<String> locales;
        private final String adAccountId;
        private final String country;
        private final int limit;

        private DiscoveryPlan(List<DiscoverySeed> seeds, List<String> locales, String adAccountId, String country, int limit) {
            this.seeds = seeds;
            this.locales = locales;
            this.adAccountId = adAccountId;
            this.country = country;
            this.limit = limit;
        }

        static DiscoveryPlan fromPayload(JsonNode payload) {
            LinkedHashSet<String> rawTerms = new LinkedHashSet<>();
            if (payload != null) {
                JsonNode searchTerms = payload.path("searchTerms");
                if (searchTerms != null && searchTerms.isArray()) {
                    for (JsonNode node : searchTerms) {
                        if (node != null && node.isTextual() && StringUtils.hasText(node.asText())) {
                            rawTerms.add(node.asText().trim());
                        }
                    }
                }
                String query = payload.path("query").asText(null);
                if (StringUtils.hasText(query)) {
                    rawTerms.add(query.trim());
                }
                String seedKeyword = payload.path("seedKeyword").asText(null);
                if (StringUtils.hasText(seedKeyword)) {
                    rawTerms.add(seedKeyword.trim());
                }
            }
            if (rawTerms.isEmpty()) {
                rawTerms.add("marketing");
            }
            List<DiscoverySeed> seeds = rawTerms.stream()
                    .filter(StringUtils::hasText)
                    .map(DiscoverySeed::from)
                    .limit(MAX_SEEDS)
                    .toList();
            if (seeds.isEmpty()) {
                seeds = List.of(DiscoverySeed.from("marketing"));
            }

            LinkedHashSet<String> localeSet = new LinkedHashSet<>();
            String preferredLocale = payload != null ? payload.path("locale").asText(null) : null;
            if (!StringUtils.hasText(preferredLocale)) {
                preferredLocale = "pt_BR";
            }
            localeSet.add(preferredLocale.trim());
            localeSet.add("en_US");
            if (payload != null) {
                JsonNode localeNode = payload.path("locales");
                if (localeNode != null && localeNode.isArray()) {
                    for (JsonNode node : localeNode) {
                        if (node != null && node.isTextual() && StringUtils.hasText(node.asText())) {
                            localeSet.add(node.asText().trim());
                        }
                    }
                }
            }
            List<String> locales = localeSet.stream()
                    .filter(StringUtils::hasText)
                    .limit(MAX_LOCALES)
                    .toList();
            if (locales.isEmpty()) {
                locales = List.of("pt_BR", "en_US");
            }

            String adAccountId = payload != null && payload.hasNonNull("adAccountId")
                    ? payload.get("adAccountId").asText(null)
                    : null;
            String country = payload != null && payload.hasNonNull("country")
                    ? payload.get("country").asText("BR")
                    : "BR";
            if (!StringUtils.hasText(country)) {
                country = "BR";
            }
            int limit = payload != null && payload.has("limit")
                    ? Math.max(1, Math.min(DEFAULT_LIMIT, payload.get("limit").asInt(DEFAULT_LIMIT)))
                    : DEFAULT_LIMIT;
            return new DiscoveryPlan(seeds, locales, adAccountId, country, limit);
        }

        List<DiscoverySeed> seeds() {
            return seeds;
        }

        List<String> locales() {
            return locales;
        }

        String adAccountId() {
            return adAccountId;
        }

        String country() {
            return country;
        }

        int limit() {
            return limit;
        }

        List<TargetingSearchType> catalogTypes() {
            return DISCOVERY_CATALOG_TYPES;
        }
    }

    private static class DiscoverySeed {
        private final String original;
        private final List<String> variants;

        private DiscoverySeed(String original, List<String> variants) {
            this.original = original;
            this.variants = variants;
        }

        static DiscoverySeed from(String raw) {
            String value = StringUtils.hasText(raw) ? raw.trim() : "marketing";
            LinkedHashSet<String> variants = new LinkedHashSet<>();
            variants.add(value);
            String ascii = Normalizer.normalize(value, Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                    .replaceAll("[^\\p{Alnum}\\s]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (StringUtils.hasText(ascii)) {
                variants.add(ascii);
            }
            variants.add(value.toLowerCase(Locale.ROOT));
            List<String> sanitized = variants.stream()
                    .filter(StringUtils::hasText)
                    .limit(3)
                    .toList();
            return new DiscoverySeed(value, sanitized.isEmpty() ? List.of(value) : sanitized);
        }

        String original() {
            return original;
        }

        List<String> variants() {
            return variants;
        }
    }

    private class DiscoveryAccumulator {
        private final ObjectMapper mapper;
        private final ArrayNode rawCalls;
        private final LinkedHashMap<String, ObjectNode> dedup;
        private final Map<String, Integer> typeCounts;
        private long rawItems = 0;
        private FacebookTargetingSearchResult bestInterest;

        DiscoveryAccumulator(ObjectMapper mapper) {
            this.mapper = mapper;
            this.rawCalls = mapper.createArrayNode();
            this.dedup = new LinkedHashMap<>();
            this.typeCounts = new LinkedHashMap<>();
        }

        void addCall(String seed, String variant, String locale, String source, List<FacebookTargetingSearchResult> results) {
            if (results == null || results.isEmpty()) {
                return;
            }
            ObjectNode callNode = mapper.createObjectNode();
            callNode.put("seed", seed);
            callNode.put("variant", variant);
            callNode.put("locale", locale);
            callNode.put("source", source);
            ArrayNode items = mapper.createArrayNode();
            for (FacebookTargetingSearchResult result : results) {
                ObjectNode itemNode = mapper.createObjectNode();
                itemNode.put("id", result.id());
                itemNode.put("name", result.name());
                itemNode.put("type", result.type());
                if (StringUtils.hasText(result.topic())) {
                    itemNode.put("topic", result.topic());
                }
                if (result.audienceSizeLowerBound() != null) {
                    itemNode.put("audienceLowerBound", result.audienceSizeLowerBound());
                }
                if (result.audienceSizeUpperBound() != null) {
                    itemNode.put("audienceUpperBound", result.audienceSizeUpperBound());
                }
                ArrayNode pathNode = mapper.createArrayNode();
                if (result.path() != null) {
                    result.path().forEach(pathNode::add);
                }
                itemNode.set("path", pathNode);
                items.add(itemNode);
                mergeDedup(result, seed, locale, source);
                updateBestInterest(result);
            }
            rawItems += results.size();
            callNode.set("items", items);
            rawCalls.add(callNode);
        }

        ObjectNode build(DiscoveryPlan plan) {
            ObjectNode root = mapper.createObjectNode();
            root.put("strategy", "discovery_v2");
            ArrayNode seedArray = mapper.createArrayNode();
            plan.seeds().forEach(seed -> seedArray.add(seed.original()));
            root.set("seedTerms", seedArray);
            ArrayNode localeArray = mapper.createArrayNode();
            plan.locales().forEach(localeArray::add);
            root.set("locales", localeArray);
            root.put("country", plan.country());
            root.set("raw", rawCalls);
            ArrayNode dedupArray = mapper.createArrayNode();
            dedup.values().forEach(dedupArray::add);
            root.set("dedup", dedupArray);
            ObjectNode stats = mapper.createObjectNode();
            stats.put("rawCalls", rawCalls.size());
            stats.put("rawItems", rawItems);
            stats.put("dedupItems", dedup.size());
            ObjectNode byTypeNode = mapper.createObjectNode();
            typeCounts.forEach(byTypeNode::put);
            stats.set("byType", byTypeNode);
            root.set("stats", stats);
            ArrayNode anchors = mapper.createArrayNode();
            dedup.values().stream()
                    .filter(node -> "INTEREST".equalsIgnoreCase(node.path("type").asText()))
                    .limit(15)
                    .forEach(anchors::add);
            root.set("anchorCandidates", anchors);
            return root;
        }

        FacebookTargetingSearchResult bestInterest() {
            return bestInterest;
        }

        private void mergeDedup(FacebookTargetingSearchResult result, String seed, String locale, String source) {
            String normalizedType = normalizeType(result != null ? result.type() : null);
            String keyBase = StringUtils.hasText(result != null ? result.id() : null)
                    ? result.id().trim()
                    : (StringUtils.hasText(result != null ? result.name() : null) ? result.name().trim().toLowerCase(Locale.ROOT) : seed.toLowerCase(Locale.ROOT));
            String key = keyBase + "|" + normalizedType;
            ObjectNode entry = dedup.get(key);
            if (entry == null) {
                entry = mapper.createObjectNode();
                if (StringUtils.hasText(result.id())) {
                    entry.put("id", result.id());
                }
                if (StringUtils.hasText(result.name())) {
                    entry.put("name", result.name());
                }
                entry.put("type", normalizedType);
                if (StringUtils.hasText(result.topic())) {
                    entry.put("topic", result.topic());
                }
                if (result.audienceSizeLowerBound() != null) {
                    entry.put("audienceLowerBound", result.audienceSizeLowerBound());
                }
                if (result.audienceSizeUpperBound() != null) {
                    entry.put("audienceUpperBound", result.audienceSizeUpperBound());
                }
                ArrayNode pathNode = mapper.createArrayNode();
                if (result.path() != null) {
                    result.path().forEach(pathNode::add);
                }
                entry.set("path", pathNode);
                entry.set("sources", mapper.createArrayNode());
                entry.set("locales", mapper.createArrayNode());
                entry.set("terms", mapper.createArrayNode());
                dedup.put(key, entry);
            }
            appendUnique((ArrayNode) entry.get("sources"), source);
            appendUnique((ArrayNode) entry.get("locales"), locale);
            appendUnique((ArrayNode) entry.get("terms"), seed);
            typeCounts.merge(normalizedType, 1, Integer::sum);
        }

        private void updateBestInterest(FacebookTargetingSearchResult candidate) {
            if (candidate == null || !"INTEREST".equals(normalizeType(candidate.type())) || !isAnchorAllowed(candidate)) {
                return;
            }
            if (bestInterest == null) {
                bestInterest = candidate;
                return;
            }
            Long currentUpper = bestInterest.audienceSizeUpperBound();
            Long candidateUpper = candidate.audienceSizeUpperBound();
            if (candidateUpper != null && (currentUpper == null || candidateUpper > currentUpper)) {
                bestInterest = candidate;
                return;
            }
            if (candidateUpper == null && currentUpper == null) {
                Long currentLower = bestInterest.audienceSizeLowerBound();
                Long candidateLower = candidate.audienceSizeLowerBound();
                if (candidateLower != null && (currentLower == null || candidateLower > currentLower)) {
                    bestInterest = candidate;
                }
            }
        }

        private boolean isAnchorAllowed(FacebookTargetingSearchResult candidate) {
            if (candidate == null || !StringUtils.hasText(candidate.name())) {
                return false;
            }
            String normalized = candidate.name().toLowerCase(Locale.ROOT);
            return DISCOVERY_ANCHOR_BLOCKLIST.stream()
                    .map(term -> term.toLowerCase(Locale.ROOT))
                    .noneMatch(normalized::contains);
        }

        private String normalizeType(String raw) {
            if (!StringUtils.hasText(raw)) {
                return "INTEREST";
            }
            String normalized = raw.trim().toUpperCase(Locale.ROOT);
            if (normalized.contains("BEHAV")) {
                return "BEHAVIOR";
            }
            if (normalized.contains("WORK_EMPLOYER")) {
                return "WORK_EMPLOYER";
            }
            if (normalized.contains("WORK_POSITION")) {
                return "WORK_POSITION";
            }
            if (normalized.contains("INDUSTR")) {
                return "INDUSTRY";
            }
            return "INTEREST";
        }

        private void appendUnique(ArrayNode array, String value) {
            if (!StringUtils.hasText(value) || array == null) {
                return;
            }
            for (JsonNode node : array) {
                if (value.equalsIgnoreCase(node.asText())) {
                    return;
                }
            }
            array.add(value);
        }
    }

    private JsonNode parseJsonPayload(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (JsonProcessingException ex) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("raw", raw);
            return node;
        }
    }

    private JsonNode toJsonNode(Object value) {
        if (value == null) {
            return objectMapper.nullNode();
        }
        try {
            return objectMapper.valueToTree(value);
        } catch (IllegalArgumentException ex) {
            return objectMapper.createObjectNode().put("error", ex.getMessage());
        }
    }

    private record ApiCallLog(
            String provider,
            String endpoint,
            String httpMethod,
            JsonNode requestPayload,
            JsonNode responsePayload,
            Integer statusCode,
            String errorMessage,
            Instant requestedAt,
            Instant respondedAt
    ) {}

    private String buildWorkerId() {
        try {
            return "facebook-playbook-" + InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "facebook-playbook";
        }
    }
}
