package com.marketinghub.facebookadsworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.facebookads.FacebookAdsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FacebookAdsService {
    private final FacebookAdsClient client;

    public String createInstagramCampaign(String adAccountId, String name) {
        return client.createCampaign(adAccountId, name, "OUTCOME_TRAFFIC").path("id").asText();
    }

    public JsonNode getCampaignMetrics(String campaignId) {
        return client.getCampaignInsights(campaignId);
    }
}
