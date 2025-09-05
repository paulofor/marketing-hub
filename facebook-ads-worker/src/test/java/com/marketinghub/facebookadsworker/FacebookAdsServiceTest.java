package com.marketinghub.facebookadsworker;

import com.fasterxml.jackson.databind.JsonNode;
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

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        String baseUrl = server.url("/").toString();
        service = new FacebookAdsService(WebClient.builder(), baseUrl, "token");
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
        assertEquals("/v20.0/act_1/campaigns", request.getPath());
        assertEquals("123", id);
    }

    @Test
    void metricsRequestsCampaignInsights() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"data\":[{\"impressions\":\"10\"}]}")
            .addHeader("Content-Type", "application/json"));
        JsonNode node = service.getCampaignMetrics("77");
        RecordedRequest request = server.takeRequest();
        assertEquals("/v20.0/77/insights?access_token=token", request.getPath());
        assertEquals("10", node.get("data").get(0).path("impressions").asText());
    }
}
