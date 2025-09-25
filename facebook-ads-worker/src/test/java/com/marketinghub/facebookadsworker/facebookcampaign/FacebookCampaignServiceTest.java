package com.marketinghub.facebookadsworker.facebookcampaign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FacebookCampaignServiceTest {
    private MockWebServer backend;
    private MockWebServer facebook;
    private FacebookCampaignService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        backend = new MockWebServer();
        backend.start();
        facebook = new MockWebServer();
        facebook.start();

        String backendUrl = backend.url("/").toString();
        String facebookUrl = facebook.url("/").toString();

        objectMapper = new ObjectMapper();
        FacebookAdsService adsService = new FacebookAdsService(
            WebClient.builder(),
            facebookUrl,
            "token",
            "v23.0",
            objectMapper
        );
        service = new FacebookCampaignService(
            adsService,
            WebClient.builder(),
            backendUrl,
            "/api",
            "1",
            "2000",
            "IMPRESSIONS",
            "LINK_CLICKS",
            "WEBSITE",
            "BR",
            "42",
            "11",
            "https://example.com",
            "Conheça %s",
            "LEARN_MORE"
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        backend.shutdown();
        facebook.shutdown();
    }

    @Test
    void createsCampaignHierarchyForEachExperiment() throws Exception {
        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\"}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueue(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        RecordedRequest get = backend.takeRequest();
        assertEquals("/api/facebook-campaigns/experiments-ready", get.getPath());

        RecordedRequest postCampaign = facebook.takeRequest();
        assertEquals("/v23.0/act_1/campaigns", postCampaign.getPath());

        RecordedRequest postAdSet = facebook.takeRequest();
        assertEquals("/v23.0/act_1/adsets", postAdSet.getPath());
        JsonNode adSetPayload = objectMapper.readTree(postAdSet.getBody().inputStream());
        assertEquals("Exp - Ad Set", adSetPayload.get("name").asText());
        assertEquals("10", adSetPayload.get("campaign_id").asText());
        assertEquals("2000", adSetPayload.get("daily_budget").asText());

        RecordedRequest postCreative = facebook.takeRequest();
        assertEquals("/v23.0/act_1/adcreatives", postCreative.getPath());
        JsonNode creativePayload = objectMapper.readTree(postCreative.getBody().inputStream());
        assertEquals("Exp - Creative", creativePayload.get("name").asText());
        JsonNode storySpec = creativePayload.get("object_story_spec");
        assertEquals("42", storySpec.get("page_id").asText());
        assertEquals("Conheça Exp", storySpec.get("link_data").get("message").asText());

        RecordedRequest postAd = facebook.takeRequest();
        assertEquals("/v23.0/act_1/ads", postAd.getPath());
        JsonNode adPayload = objectMapper.readTree(postAd.getBody().inputStream());
        assertEquals("Exp - Ad", adPayload.get("name").asText());
        assertEquals("20", adPayload.get("adset_id").asText());
        assertEquals("30", adPayload.get("creative").get("creative_id").asText());

        RecordedRequest postBackend = backend.takeRequest();
        assertEquals("/api/facebook-campaigns", postBackend.getPath());
    }

    @Test
    void ignoresConnectionIssuesFetchingExperiments() {
        backend.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        service.createCampaignsFromExperiments();

        assertEquals(1, backend.getRequestCount());
        assertEquals(0, facebook.getRequestCount());
    }

    @Test
    void stopsProcessingWhenFacebookReturnsPermissionError() {
        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\"},{\"id\":2,\"name\":\"Exp 2\"}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody("{" +
                "\"error\":{" +
                "\"type\":\"OAuthException\"," +
                "\"code\":200," +
                "\"error_subcode\":1815066," +
                "\"message\":\"Permissions error\"," +
                "\"error_user_title\":\"O usuário não tem permissão\"," +
                "\"error_user_msg\":\"O usuário não tem permissão para criar anúncios com esta conta de anúncios\"," +
                "\"fbtrace_id\":\"trace\"}" +
                "}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        assertEquals(1, backend.getRequestCount());
        assertEquals(1, facebook.getRequestCount());
    }
}
