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
    private static final Set<String> DISCOVERY_GENERIC_ANCHOR_TERMS = Set.of(
            "unspecified",
            "unknown",
            "not specified",
            "n/a",
            "nao especificado",
            "não especificado",
            "nao informado",
            "não informado",
            "desconhecido",
            "indefinido"
    );
    private static final Set<String> DISCOVERY_BROAD_TERMS = Set.of(
            "marketing",
            "business",
            "negocios",
            "negócios",
            "empresa",
            "empresas",
            "general",
            "geral"
    );
    private static final int DISCOVERY_MIN_RELEVANCE_SCORE = 25;

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
        DiscoveryAccumulator accumulator = new DiscoveryAccumulator(objectMapper, plan);
        for (DiscoveryTypedQuery typedQuery : plan.typedQueries()) {
            for (String locale : plan.locales()) {
                for (DiscoverySeed seed : typedQuery.seeds()) {
                    for (String variant : seed.variants()) {
                        TargetingSearchRequest discoveryRequest = new TargetingSearchRequest(
                                typedQuery.type(),
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
                        accumulator.addCall(seed.original(), variant, locale, "targetingsearch:" + typedQuery.type().graphType(), discoveryResults);
                    }
                }
            }
        }
        ObjectNode result = accumulator.build(plan);
        FacebookTargetingSearchResult anchor = accumulator.bestInterest();
        if (anchor == null || !StringUtils.hasText(anchor.id())) {
            throw new IllegalStateException("Etapa 4: nenhum anchor de interesse atingiu o score mínimo de relevância");
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

    private static List<DiscoverySeed> buildSeeds(JsonNode payload, String primaryField, String secondaryField, String fallbackField, int maxSeeds) {
        LinkedHashSet<String> rawTerms = new LinkedHashSet<>();
        appendTerms(rawTerms, payload, primaryField);
        appendTerms(rawTerms, payload, secondaryField);
        appendTerms(rawTerms, payload, fallbackField);
        List<DiscoverySeed> seeds = rawTerms.stream()
                .filter(StringUtils::hasText)
                .map(DiscoverySeed::from)
                .limit(maxSeeds)
                .toList();
        return seeds.isEmpty() ? List.of(DiscoverySeed.from("marketing")) : seeds;
    }

    private static void appendTerms(LinkedHashSet<String> terms, JsonNode payload, String fieldName) {
        if (payload == null || !StringUtils.hasText(fieldName)) {
            return;
        }
        JsonNode node = payload.path(fieldName);
        if (node != null && node.isArray()) {
            for (JsonNode value : node) {
                if (value != null && value.isTextual() && StringUtils.hasText(value.asText())) {
                    terms.add(value.asText().trim());
                }
            }
            return;
        }
        String value = node != null ? node.asText(null) : null;
        if (StringUtils.hasText(value)) {
            terms.add(value.trim());
        }
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

        private final List<DiscoveryTypedQuery> typedQueries;
        private final List<String> locales;
        private final String adAccountId;
        private final String country;
        private final int limit;

        private DiscoveryPlan(List<DiscoveryTypedQuery> typedQueries, List<String> locales, String adAccountId, String country, int limit) {
            this.typedQueries = typedQueries;
            this.locales = locales;
            this.adAccountId = adAccountId;
            this.country = country;
            this.limit = limit;
        }

        static DiscoveryPlan fromPayload(JsonNode payload) {
            List<DiscoveryTypedQuery> typedQueries = List.of(
                    new DiscoveryTypedQuery(
                            TargetingSearchType.AD_INTEREST,
                            buildSeeds(payload, "interestQueries", "searchTerms", "seedKeyword", MAX_SEEDS)
                    ),
                    new DiscoveryTypedQuery(
                            TargetingSearchType.AD_BEHAVIOR,
                            buildSeeds(payload, "behaviorQueries", "behaviors", null, MAX_SEEDS)
                    ),
                    new DiscoveryTypedQuery(
                            TargetingSearchType.AD_WORK_POSITION,
                            buildSeeds(payload, "workPositionQueries", "positionQueries", null, MAX_SEEDS)
                    )
            );

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
            return new DiscoveryPlan(typedQueries, locales, adAccountId, country, limit);
        }

        List<DiscoveryTypedQuery> typedQueries() {
            return typedQueries;
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
    }

    private record DiscoveryTypedQuery(TargetingSearchType type, List<DiscoverySeed> seeds) {
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
        private final Set<String> normalizedSeedTerms;
        private final Set<String> normalizedIcpTerms;
        private final ArrayNode rejectedCandidates;
        private long rawItems = 0;
        private FacebookTargetingSearchResult bestInterest;
        private int bestInterestScore = Integer.MIN_VALUE;
        private String anchorSelectionReason = "Nenhum candidato de interesse avaliado até o momento";

        DiscoveryAccumulator(ObjectMapper mapper, DiscoveryPlan plan) {
            this.mapper = mapper;
            this.rawCalls = mapper.createArrayNode();
            this.dedup = new LinkedHashMap<>();
            this.typeCounts = new LinkedHashMap<>();
            this.normalizedSeedTerms = extractSeedTerms(plan);
            this.normalizedIcpTerms = extractIcpTerms(plan);
            this.rejectedCandidates = mapper.createArrayNode();
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
            ObjectNode seedTermsByType = mapper.createObjectNode();
            for (DiscoveryTypedQuery typedQuery : plan.typedQueries()) {
                ArrayNode seedArray = mapper.createArrayNode();
                typedQuery.seeds().forEach(seed -> seedArray.add(seed.original()));
                seedTermsByType.set(typedQuery.type().graphType(), seedArray);
            }
            root.set("seedTerms", seedTermsByType);
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
            root.put("anchorSelectionReason", anchorSelectionReason);
            root.set("rejectedCandidates", rejectedCandidates);
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
            if (candidate == null || !"INTEREST".equals(normalizeType(candidate.type()))) {
                return;
            }
            String rejectReason = isAnchorAllowed(candidate);
            if (rejectReason != null) {
                registerRejectedCandidate(candidate, rejectReason, Integer.MIN_VALUE);
                return;
            }
            int candidateScore = relevanceScore(candidate);
            if (candidateScore < DISCOVERY_MIN_RELEVANCE_SCORE) {
                registerRejectedCandidate(candidate,
                        "Score de relevância insuficiente (" + candidateScore + " < " + DISCOVERY_MIN_RELEVANCE_SCORE + ")",
                        candidateScore);
                return;
            }
            if (bestInterest == null) {
                bestInterest = candidate;
                bestInterestScore = candidateScore;
                anchorSelectionReason = "Selecionado por maior score de relevância inicial=" + candidateScore;
                return;
            }
            if (candidateScore > bestInterestScore) {
                bestInterest = candidate;
                bestInterestScore = candidateScore;
                anchorSelectionReason = "Candidato substituído por score de relevância maior=" + candidateScore;
                return;
            }
            if (candidateScore == bestInterestScore) {
                if (hasGreaterAudience(candidate, bestInterest)) {
                    bestInterest = candidate;
                    anchorSelectionReason = "Empate de score resolvido por maior audiência";
                } else {
                    anchorSelectionReason = "Anchor mantido por score equivalente e audiência não superior";
                }
            }
        }

        private String isAnchorAllowed(FacebookTargetingSearchResult candidate) {
            if (candidate == null || !StringUtils.hasText(candidate.name())) {
                return "Nome ausente";
            }
            String normalized = normalizeText(candidate.name());
            if (DISCOVERY_GENERIC_ANCHOR_TERMS.stream().anyMatch(normalized::contains)) {
                return "Nome genérico/rejeitado: " + candidate.name();
            }
            boolean blocklisted = DISCOVERY_ANCHOR_BLOCKLIST.stream()
                    .map(term -> term.toLowerCase(Locale.ROOT))
                    .anyMatch(normalized::contains);
            return blocklisted ? "Termo bloqueado pela blocklist" : null;
        }

        private int relevanceScore(FacebookTargetingSearchResult candidate) {
            int score = 0;
            Set<String> candidateTerms = candidateTerms(candidate);
            if (!normalizedSeedTerms.isEmpty() && candidateTerms.stream().anyMatch(normalizedSeedTerms::contains)) {
                score += 40;
            }
            if (!normalizedIcpTerms.isEmpty() && pathMatchesIcp(candidate)) {
                score += 25;
            }
            if (containsBroadTerm(candidateTerms)) {
                score -= 30;
            }
            if (StringUtils.hasText(candidate.topic())) {
                String topic = normalizeText(candidate.topic());
                if (normalizedIcpTerms.stream().anyMatch(topic::contains)) {
                    score += 10;
                }
            }
            return score;
        }

        private boolean pathMatchesIcp(FacebookTargetingSearchResult candidate) {
            if (candidate.path() == null || candidate.path().isEmpty()) {
                return false;
            }
            return candidate.path().stream()
                    .filter(StringUtils::hasText)
                    .map(this::normalizeText)
                    .anyMatch(pathEntry -> normalizedIcpTerms.stream().anyMatch(pathEntry::contains));
        }

        private boolean containsBroadTerm(Set<String> candidateTerms) {
            return candidateTerms.stream().anyMatch(term -> DISCOVERY_BROAD_TERMS.contains(term));
        }

        private boolean hasGreaterAudience(FacebookTargetingSearchResult candidate, FacebookTargetingSearchResult current) {
            Long currentUpper = current.audienceSizeUpperBound();
            Long candidateUpper = candidate.audienceSizeUpperBound();
            if (candidateUpper != null && (currentUpper == null || candidateUpper > currentUpper)) {
                return true;
            }
            if (candidateUpper == null && currentUpper == null) {
                Long currentLower = current.audienceSizeLowerBound();
                Long candidateLower = candidate.audienceSizeLowerBound();
                return candidateLower != null && (currentLower == null || candidateLower > currentLower);
            }
            return false;
        }

        private void registerRejectedCandidate(FacebookTargetingSearchResult candidate, String reason, int score) {
            ObjectNode rejected = mapper.createObjectNode();
            if (StringUtils.hasText(candidate.id())) {
                rejected.put("id", candidate.id());
            }
            rejected.put("name", candidate.name());
            rejected.put("reason", reason);
            if (score != Integer.MIN_VALUE) {
                rejected.put("relevanceScore", score);
            }
            rejectedCandidates.add(rejected);
        }

        private Set<String> extractSeedTerms(DiscoveryPlan plan) {
            return plan.typedQueries().stream()
                    .flatMap(query -> query.seeds().stream())
                    .flatMap(seed -> seed.variants().stream())
                    .map(this::normalizeText)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        private Set<String> extractIcpTerms(DiscoveryPlan plan) {
            return plan.typedQueries().stream()
                    .flatMap(query -> query.seeds().stream())
                    .map(DiscoverySeed::original)
                    .map(this::normalizeText)
                    .flatMap(value -> List.of(value.split("\\s+")).stream())
                    .filter(token -> token.length() >= 4)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        private Set<String> candidateTerms(FacebookTargetingSearchResult candidate) {
            LinkedHashSet<String> terms = new LinkedHashSet<>();
            if (StringUtils.hasText(candidate.name())) {
                String normalizedName = normalizeText(candidate.name());
                terms.add(normalizedName);
                terms.addAll(List.of(normalizedName.split("\\s+")));
            }
            if (StringUtils.hasText(candidate.topic())) {
                String normalizedTopic = normalizeText(candidate.topic());
                terms.add(normalizedTopic);
                terms.addAll(List.of(normalizedTopic.split("\\s+")));
            }
            if (candidate.path() != null) {
                candidate.path().stream()
                        .filter(StringUtils::hasText)
                        .map(this::normalizeText)
                        .forEach(pathValue -> {
                            terms.add(pathValue);
                            terms.addAll(List.of(pathValue.split("\\s+")));
                        });
            }
            return terms;
        }

        private String normalizeText(String value) {
            if (!StringUtils.hasText(value)) {
                return "";
            }
            return Normalizer.normalize(value, Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("[^\\p{Alnum}\\s]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
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
