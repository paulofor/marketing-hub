package com.marketinghub.facebookadsworker.facebookplaybook;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
            try {
                JsonNode result = switch (job.type()) {
                    case FACEBOOK_SEED_LOOKUP -> handleSeedLookup(job.payload());
                    case FACEBOOK_TARGETING_SUGGESTIONS -> handleSuggestions(job.payload());
                    case FACEBOOK_SOCIAL_POSITIONS -> handlePositions(job.payload());
                    case FACEBOOK_VALIDATE_SPEC -> handleValidation(job.payload());
                    case FACEBOOK_REACH_ESTIMATE -> handleReachEstimate(job.payload());
                    default -> {
                        LOGGER.warn("Job {} do tipo {} não é processado pelo worker do Facebook", job.id(), job.type());
                        yield objectMapper.createObjectNode();
                    }
                };
                client.completeJob(job.id(), result);
            } catch (Exception ex) {
                LOGGER.error("Erro ao executar job {} do tipo {}", job.id(), job.type(), ex);
                client.failJob(job.id(), ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido");
            }
        }
    }

    private JsonNode handleSeedLookup(JsonNode payload) {
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
        List<FacebookTargetingSearchResult> results = facebookAdsService.searchTargetingOptions(request);
        FacebookTargetingSearchResult first = results.stream()
                .max(Comparator.comparing(FacebookTargetingSearchResult::audienceSize, Comparator.nullsLast(Long::compareTo)))
                .orElseThrow(() -> new IllegalStateException("Nenhum interesse encontrado para " + query));
        ObjectNode result = objectMapper.createObjectNode();
        result.put("interestId", first.id());
        result.put("interestName", first.name());
        if (first.audienceSize() != null) {
            result.put("audienceLowerBound", first.audienceSize());
            result.put("audienceUpperBound", first.audienceSize());
        }
        if (first.path() != null) {
            ArrayNode path = objectMapper.createArrayNode();
            first.path().forEach(path::add);
            result.set("path", path);
        }
        return result;
    }

    private JsonNode handleSuggestions(JsonNode payload) {
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
        List<FacebookTargetingSuggestionResult> suggestions = facebookAdsService.suggestTargetingOptions(request);
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

    private JsonNode handlePositions(JsonNode payload) {
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
            facebookAdsService.searchTargetingOptions(request).forEach(result -> {
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

    private JsonNode handleValidation(JsonNode payload) {
        String adAccountId = text(payload, "adAccountId", null);
        JsonNode targeting = payload.path("targetingSpec");
        JsonNode apiResponse = facebookAdsService.validateTargetingSpec(
                new FacebookAdsService.TargetingValidationRequest(adAccountId, targeting));
        boolean isValid = apiResponse != null
                && apiResponse.path("data").isArray()
                && apiResponse.path("data").size() > 0
                && apiResponse.path("data").get(0).path("is_valid").asBoolean(true);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("status", isValid ? "VALID" : "INVALID");
        result.set("details", apiResponse);
        return result;
    }

    private JsonNode handleReachEstimate(JsonNode payload) {
        String adAccountId = text(payload, "adAccountId", null);
        JsonNode targeting = payload.path("targetingSpec");
        JsonNode apiResponse = facebookAdsService.estimateReach(
                new FacebookAdsService.ReachEstimateRequest(adAccountId, targeting));
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

    private String buildWorkerId() {
        try {
            return "facebook-playbook-" + InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "facebook-playbook";
        }
    }
}
