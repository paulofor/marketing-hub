package com.marketinghub.worker.adsetplaybook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * Applies deterministic adjustments when reach estimate is outside configured thresholds.
 */
@Component
public class AdSetSpecRecalibrator {
    private final ObjectMapper mapper;

    public AdSetSpecRecalibrator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ObjectNode recalibrate(JsonNode payload) {
        ObjectNode result = mapper.createObjectNode();
        JsonNode targetingNode = payload != null ? payload.get("targetingSpec") : null;
        if (!(targetingNode instanceof ObjectNode targeting)) {
            return result;
        }

        long lower = payload.path("usersLowerBound").asLong(-1L);
        long upper = payload.path("usersUpperBound").asLong(-1L);
        long minLower = payload.path("reachMinLowerBound").asLong(200_000L);
        long maxUpper = payload.path("reachMaxUpperBound").asLong(20_000_000L);

        int ageMin = payload.path("ageMin").asInt(targeting.path("age_min").asInt(18));
        int ageMax = payload.path("ageMax").asInt(targeting.path("age_max").asInt(55));

        if (lower >= 0 && lower < minLower) {
            ageMin = Math.max(18, ageMin - 2);
            ageMax = Math.min(65, ageMax + 3);
            removeOneFlexibleBlock(targeting);
        } else if (upper > maxUpper) {
            ageMin = Math.min(ageMax - 1, ageMin + 2);
            ageMax = Math.max(ageMin + 1, ageMax - 2);
            trimFlexibleBlockItems(targeting, 2);
        }

        targeting.put("age_min", ageMin);
        targeting.put("age_max", ageMax);

        result.put("specId", payload.path("specId").asLong());
        result.put("ageMin", ageMin);
        result.put("ageMax", ageMax);
        result.set("targetingSpec", targeting);
        return result;
    }

    private void removeOneFlexibleBlock(ObjectNode targeting) {
        JsonNode flexibleNode = targeting.get("flexible_spec");
        if (!(flexibleNode instanceof ArrayNode flexible) || flexible.size() <= 1) {
            return;
        }
        flexible.remove(flexible.size() - 1);
    }

    private void trimFlexibleBlockItems(ObjectNode targeting, int maxItems) {
        JsonNode flexibleNode = targeting.get("flexible_spec");
        if (!(flexibleNode instanceof ArrayNode flexible)) {
            return;
        }
        for (JsonNode blockNode : flexible) {
            if (!(blockNode instanceof ObjectNode block)) {
                continue;
            }
            block.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value instanceof ArrayNode arr) {
                    while (arr.size() > maxItems) {
                        arr.remove(arr.size() - 1);
                    }
                }
            });
        }
    }
}
