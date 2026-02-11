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
        String query = text(payload, "query");
        String locale = text(payload, "locale", "pt_BR");
        String country = text(payload, "country", "BR");
        int limit = payload.path("limit").asInt(25);
        String adAccountId = text(payload, "adAccountId", null);
        TargetingSearchRequest request = new TargetingSearchRequest(
                TargetingSearchType.AD_INTEREST,
                query,
                adAccountId,
                locale,
                country,
                limit
        );
        List<FacebookTargetingSearchResult> results = recordFacebookCall(
                apiLogs,
                "/targetingsearch",
                "GET",
                request,
                () -> facebookAdsService.searchTargetingOptions(request),
                response -> toJsonNode(response)
        );
        FacebookTargetingSearchResult first = results.stream()
                .max(Comparator.comparing(FacebookTargetingSearchResult::audienceSize, Comparator.nullsLast(Long::compareTo)))
                .orElseThrow(() -> new IllegalStateException("Nenhum interesse encontrado para " + query));
        ObjectNode result = objectMapper.createObjectNode();
        result.put("interestId", first.id());
        result.put("interestName", first.name());
        if (first.audienceSizeLowerBound() != null) {
            result.put("audienceLowerBound", first.audienceSizeLowerBound());
        }
        if (first.audienceSizeUpperBound() != null) {
            result.put("audienceUpperBound", first.audienceSizeUpperBound());
        } else if (first.audienceSizeLowerBound() != null) {
            result.put("audienceUpperBound", first.audienceSizeLowerBound());
        }
        if (first.path() != null) {
            ArrayNode path = objectMapper.createArrayNode();
            first.path().forEach(path::add);
            result.set("path", path);
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
