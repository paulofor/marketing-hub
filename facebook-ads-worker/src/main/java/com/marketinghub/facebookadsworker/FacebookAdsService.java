package com.marketinghub.facebookadsworker;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class FacebookAdsService {
    private final WebClient webClient;
    private final String accessToken;

    public FacebookAdsService(WebClient.Builder builder,
                              @Value("${facebook.graph-api.base-url:https://graph.facebook.com}") String baseUrl,
                              @Value("${facebook.access-token}") String accessToken) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.accessToken = accessToken;
    }

    public String createInstagramCampaign(String adAccountId, String name) {
        JsonNode response = webClient.post()
            .uri("/v20.0/act_" + adAccountId + "/campaigns")
            .bodyValue(Map.of(
                "name", name,
                "objective", "OUTCOME_TRAFFIC",
                "status", "PAUSED",
                "special_ad_categories", List.of("NONE"),
                "access_token", accessToken
            ))
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
        return response.path("id").asText();
    }

    public String createCampaign(String adAccountId, String name) {
        return createInstagramCampaign(adAccountId, name);
    }

    public JsonNode getCampaignMetrics(String campaignId) {
        return webClient.get()
            .uri("/v20.0/" + campaignId + "/insights?access_token=" + accessToken)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    }
}
