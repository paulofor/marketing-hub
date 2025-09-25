package com.marketinghub.facebookadsworker;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
                "special_ad_categories", List.of(),
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

    public String createAdSet(String adAccountId, AdSetRequest request) {
        Objects.requireNonNull(request, "request");

        Map<String, Object> targeting = Map.of(
            "geo_locations", Map.of("countries", List.of(request.targetCountry()))
        );

        Map<String, Object> body = new HashMap<>();
        body.put("name", request.name());
        body.put("campaign_id", request.campaignId());
        body.put("daily_budget", request.dailyBudget());
        body.put("billing_event", request.billingEvent());
        body.put("optimization_goal", request.optimizationGoal());
        body.put("status", "PAUSED");
        body.put("destination_type", request.destinationType());
        body.put("targeting", targeting);
        if (request.pageId() != null && !request.pageId().isBlank()) {
            body.put("promoted_object", Map.of("page_id", request.pageId()));
        }
        body.put("access_token", accessToken);

        JsonNode response = webClient.post()
            .uri("/v20.0/act_" + adAccountId + "/adsets")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        return response.path("id").asText();
    }

    public String createAdCreative(String adAccountId, AdCreativeRequest request) {
        Objects.requireNonNull(request, "request");

        Map<String, Object> linkData = new HashMap<>();
        linkData.put("link", request.websiteUrl());
        linkData.put("message", request.message());
        linkData.put("call_to_action", Map.of(
            "type", request.callToActionType(),
            "value", Map.of("link", request.websiteUrl())
        ));

        Map<String, Object> objectStorySpec = new HashMap<>();
        objectStorySpec.put("page_id", request.pageId());
        if (request.instagramActorId() != null && !request.instagramActorId().isBlank()) {
            objectStorySpec.put("instagram_actor_id", request.instagramActorId());
        }
        objectStorySpec.put("link_data", linkData);

        Map<String, Object> body = new HashMap<>();
        body.put("name", request.name());
        body.put("object_story_spec", objectStorySpec);
        body.put("access_token", accessToken);

        JsonNode response = webClient.post()
            .uri("/v20.0/act_" + adAccountId + "/adcreatives")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        return response.path("id").asText();
    }

    public String createAd(String adAccountId, AdRequest request) {
        Objects.requireNonNull(request, "request");

        Map<String, Object> body = new HashMap<>();
        body.put("name", request.name());
        body.put("adset_id", request.adSetId());
        body.put("creative", Map.of("creative_id", request.creativeId()));
        body.put("status", "PAUSED");
        body.put("access_token", accessToken);

        JsonNode response = webClient.post()
            .uri("/v20.0/act_" + adAccountId + "/ads")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        return response.path("id").asText();
    }

    public JsonNode getCampaignMetrics(String campaignId) {
        return webClient.get()
            .uri("/v20.0/" + campaignId + "/insights?access_token=" + accessToken)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    }

    public record AdSetRequest(
        String name,
        String campaignId,
        String dailyBudget,
        String billingEvent,
        String optimizationGoal,
        String destinationType,
        String pageId,
        String targetCountry
    ) {}

    public record AdCreativeRequest(
        String name,
        String pageId,
        String instagramActorId,
        String websiteUrl,
        String message,
        String callToActionType
    ) {}

    public record AdRequest(
        String name,
        String adSetId,
        String creativeId
    ) {}
}
