package com.marketinghub.facebookadsworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FacebookAdsServiceTest {
    private MockWebServer server;
    private FacebookAdsService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        String baseUrl = server.url("/").toString();
        service = new FacebookAdsService(WebClient.builder(), baseUrl, "token", "v23.0");
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void createCampaignPostsCorrectRequest() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"id\":\"123\"}")
            .addHeader("Content-Type", "application/json"));
        String id = service.createInstagramCampaign("1", "Camp");
        RecordedRequest request = server.takeRequest();
        assertEquals("/v23.0/act_1/campaigns", request.getPath());
        JsonNode body = objectMapper.readTree(request.getBody().inputStream());
        assertEquals("Camp", body.get("name").asText());
        assertEquals("OUTCOME_TRAFFIC", body.get("objective").asText());
        assertEquals("PAUSED", body.get("status").asText());
        assertEquals(0, body.get("special_ad_categories").size());
        assertEquals("123", id);
    }

    @Test
    void createAdSetPostsCorrectRequest() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"id\":\"222\"}")
            .addHeader("Content-Type", "application/json"));
        FacebookAdsService.AdSetRequest request = new FacebookAdsService.AdSetRequest(
            "Camp - Ad Set",
            "123",
            "1500",
            "IMPRESSIONS",
            "LINK_CLICKS",
            "WEBSITE",
            "42",
            "BR"
        );
        String id = service.createAdSet("1", request);
        RecordedRequest recorded = server.takeRequest();
        assertEquals("/v23.0/act_1/adsets", recorded.getPath());
        JsonNode body = objectMapper.readTree(recorded.getBody().inputStream());
        assertEquals("Camp - Ad Set", body.get("name").asText());
        assertEquals("123", body.get("campaign_id").asText());
        assertEquals("1500", body.get("daily_budget").asText());
        assertEquals("IMPRESSIONS", body.get("billing_event").asText());
        assertEquals("LINK_CLICKS", body.get("optimization_goal").asText());
        assertEquals("PAUSED", body.get("status").asText());
        assertEquals("WEBSITE", body.get("destination_type").asText());
        assertEquals("BR", body.get("targeting").get("geo_locations").get("countries").get(0).asText());
        assertEquals("42", body.get("promoted_object").get("page_id").asText());
        assertEquals("222", id);
    }

    @Test
    void createAdCreativePostsCorrectRequest() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"id\":\"333\"}")
            .addHeader("Content-Type", "application/json"));
        FacebookAdsService.AdCreativeRequest request = new FacebookAdsService.AdCreativeRequest(
            "Camp - Creative",
            "42",
            "11",
            "https://example.com",
            "Mensagem",
            "LEARN_MORE"
        );
        String id = service.createAdCreative("1", request);
        RecordedRequest recorded = server.takeRequest();
        assertEquals("/v23.0/act_1/adcreatives", recorded.getPath());
        JsonNode body = objectMapper.readTree(recorded.getBody().inputStream());
        JsonNode storySpec = body.get("object_story_spec");
        assertEquals("42", storySpec.get("page_id").asText());
        assertEquals("11", storySpec.get("instagram_actor_id").asText());
        JsonNode linkData = storySpec.get("link_data");
        assertEquals("https://example.com", linkData.get("link").asText());
        assertEquals("Mensagem", linkData.get("message").asText());
        assertEquals("LEARN_MORE", linkData.get("call_to_action").get("type").asText());
        assertEquals("https://example.com", linkData.get("call_to_action").get("value").get("link").asText());
        assertEquals("333", id);
    }

    @Test
    void createAdPostsCorrectRequest() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"id\":\"444\"}")
            .addHeader("Content-Type", "application/json"));
        FacebookAdsService.AdRequest request = new FacebookAdsService.AdRequest(
            "Camp - Ad",
            "adset",
            "creative"
        );
        String id = service.createAd("1", request);
        RecordedRequest recorded = server.takeRequest();
        assertEquals("/v23.0/act_1/ads", recorded.getPath());
        JsonNode body = objectMapper.readTree(recorded.getBody().inputStream());
        assertEquals("Camp - Ad", body.get("name").asText());
        assertEquals("adset", body.get("adset_id").asText());
        assertEquals("creative", body.get("creative").get("creative_id").asText());
        assertEquals("PAUSED", body.get("status").asText());
        assertEquals("444", id);
    }

    @Test
    void metricsRequestsCampaignInsights() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"data\":[{\"impressions\":\"10\"}]}")
            .addHeader("Content-Type", "application/json"));
        JsonNode node = service.getCampaignMetrics("77");
        RecordedRequest request = server.takeRequest();
        assertEquals("/v23.0/77/insights?access_token=token", request.getPath());
        assertEquals("10", node.get("data").get(0).path("impressions").asText());
    }
}
