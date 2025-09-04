package com.marketinghub.facebookads;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Implementação que devolve dados estáticos para testes.
 */
@Component
@Profile("dummy")
public class DummyFacebookAdsClient implements FacebookAdsClient {
    private static final Logger log = LoggerFactory.getLogger(DummyFacebookAdsClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public JsonNode getAdAccounts() {
        log.debug("Returning dummy Facebook accounts");
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode data = root.putArray("data");
        ObjectNode acc = MAPPER.createObjectNode();
        acc.put("id", "123");
        acc.put("name", "Dummy Account");
        data.add(acc);
        return root;
    }

    @Override
    public JsonNode createCampaign(String adAccountId, String name, String objective) {
        log.debug("Returning dummy campaign");
        ObjectNode root = MAPPER.createObjectNode();
        root.put("id", "999");
        return root;
    }

    @Override
    public JsonNode getCampaignInsights(String campaignId) {
        log.debug("Returning dummy insights");
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode data = root.putArray("data");
        ObjectNode metric = MAPPER.createObjectNode();
        metric.put("campaign_id", campaignId);
        metric.put("impressions", 0);
        data.add(metric);
        return root;
    }
}
