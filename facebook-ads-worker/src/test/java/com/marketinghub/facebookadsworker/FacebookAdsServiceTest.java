package com.marketinghub.facebookadsworker;

import com.marketinghub.facebookadsworker.FacebookPermissionException;

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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FacebookAdsServiceTest {
    private MockWebServer server;
    private FacebookAdsService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        String baseUrl = server.url("/").toString();
        objectMapper = new ObjectMapper();
        service = new FacebookAdsService(WebClient.builder(), baseUrl, "v23.0", objectMapper);
        service.updateAccessToken("token");
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
            "LOWEST_COST_WITHOUT_CAP",
            "200",
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
        assertEquals("LOWEST_COST_WITHOUT_CAP", body.get("bid_strategy").asText());
        assertEquals("200", body.get("bid_amount").asText());
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
            null,
            "Mensagem",
            "hash-123",
            "https://cdn.example/img.jpg",
            "LEARN_MORE",
            "Headline",
            "Descrição"
        );
        String id = service.createAdCreative("1", request);
        RecordedRequest recorded = server.takeRequest();
        assertEquals("/v23.0/act_1/adcreatives", recorded.getPath());
        JsonNode body = objectMapper.readTree(recorded.getBody().inputStream());
        JsonNode storySpec = body.get("object_story_spec");
        assertEquals("42", storySpec.get("page_id").asText());
        assertEquals("11", storySpec.get("instagram_user_id").asText());
        JsonNode linkData = storySpec.get("link_data");
        assertEquals("https://example.com", linkData.get("link").asText());
        assertEquals("Mensagem", linkData.get("message").asText());
        assertEquals("Headline", linkData.get("name").asText());
        assertEquals("Descrição", linkData.get("description").asText());
        assertEquals("hash-123", linkData.get("image_hash").asText());
        assertEquals("LEARN_MORE", linkData.get("call_to_action").get("type").asText());
        assertEquals("https://example.com", linkData.get("call_to_action").get("value").get("link").asText());
        assertEquals("333", id);
    }

    @Test
    void createAdCreativeSupportsLeadForms() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"id\":\"999\"}")
            .addHeader("Content-Type", "application/json"));
        FacebookAdsService.AdCreativeRequest request = new FacebookAdsService.AdCreativeRequest(
            "Camp - Lead",
            "42",
            null,
            null,
            "123456789012345",
            "Mensagem",
            null,
            null,
            "SIGN_UP",
            null,
            null
        );
        String id = service.createAdCreative("1", request);
        RecordedRequest recorded = server.takeRequest();
        JsonNode body = objectMapper.readTree(recorded.getBody().inputStream());
        JsonNode callToAction = body.get("object_story_spec").get("link_data").get("call_to_action");
        assertEquals("SIGN_UP", callToAction.get("type").asText());
        assertEquals("123456789012345", callToAction.get("value").get("lead_gen_form_id").asText());
        assertEquals("999", id);
    }

    @Test
    void createAdCreativeFallsBackToPictureWhenHashMissing() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"id\":\"777\"}")
            .addHeader("Content-Type", "application/json"));
        FacebookAdsService.AdCreativeRequest request = new FacebookAdsService.AdCreativeRequest(
            "Camp - Creative",
            "42",
            null,
            "https://example.com",
            null,
            "Mensagem",
            null,
            "https://cdn.example/img.jpg",
            "LEARN_MORE",
            null,
            null
        );

        service.createAdCreative("1", request);

        RecordedRequest recorded = server.takeRequest();
        JsonNode linkData = objectMapper
            .readTree(recorded.getBody().inputStream())
            .get("object_story_spec")
            .get("link_data");
        assertEquals("https://cdn.example/img.jpg", linkData.get("picture").asText());
    }

    @Test
    void uploadAdImageReturnsFirstHash() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"images\":{\"image1\":{\"hash\":\"abc\"}}}")
            .addHeader("Content-Type", "application/json"));

        String hash = service.uploadAdImage("1", "https://cdn.example/img.jpg");

        RecordedRequest request = server.takeRequest();
        assertEquals("/v23.0/act_1/adimages", request.getPath());
        JsonNode body = objectMapper.readTree(request.getBody().inputStream());
        assertEquals("https://cdn.example/img.jpg", body.get("url").asText());
        assertEquals("abc", hash);
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
    void findInstantFormIdentifierUsesPageAccessToken() throws Exception {
        server.enqueue(new MockResponse()
            .setBody("{\"access_token\":\"page-token\"}")
            .addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse()
            .setBody("{\"data\":[{\"id\":\"form-1\",\"name\":\"Form Teste\",\"status\":\"DRAFT\"}]}")
            .addHeader("Content-Type", "application/json"));

        String identifier = service.findInstantFormIdentifier("999", "Form Teste");

        assertEquals("form-1", identifier);

        RecordedRequest pageTokenRequest = server.takeRequest();
        assertEquals("/v23.0/999?fields=access_token&access_token=token", pageTokenRequest.getPath());

        RecordedRequest formsRequest = server.takeRequest();
        assertEquals(
            "/v23.0/999/leadgen_forms?fields=id,name,status,draft_id&limit=200&access_token=page-token",
            formsRequest.getPath()
        );
    }

    @Test
    void findInstantFormIdentifierCachesPageAccessToken() throws Exception {
        server.enqueue(new MockResponse()
            .setBody("{\"access_token\":\"cached-page-token\"}")
            .addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse()
            .setBody("{\"data\":[]}")
            .addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse()
            .setBody("{\"data\":[]}")
            .addHeader("Content-Type", "application/json"));

        service.findInstantFormIdentifier("321", "Nome");
        service.findInstantFormIdentifier("321", "Nome");

        RecordedRequest first = server.takeRequest();
        assertEquals("/v23.0/321?fields=access_token&access_token=token", first.getPath());

        RecordedRequest second = server.takeRequest();
        assertEquals(
            "/v23.0/321/leadgen_forms?fields=id,name,status,draft_id&limit=200&access_token=cached-page-token",
            second.getPath()
        );

        RecordedRequest third = server.takeRequest();
        assertEquals(
            "/v23.0/321/leadgen_forms?fields=id,name,status,draft_id&limit=200&access_token=cached-page-token",
            third.getPath()
        );

        assertNull(server.takeRequest(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void findInstantFormIdentifierReturnsNullWhenPageTokenMissing() throws Exception {
        server.enqueue(new MockResponse()
            .setBody("{\"data\":{}}")
            .addHeader("Content-Type", "application/json"));

        String identifier = service.findInstantFormIdentifier("432", "Teste");

        assertNull(identifier);

        RecordedRequest request = server.takeRequest();
        assertEquals("/v23.0/432?fields=access_token&access_token=token", request.getPath());
        assertNull(server.takeRequest(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void findInstantFormIdentifierPropagatesPermissionErrors() {
        server.enqueue(new MockResponse()
            .setResponseCode(403)
            .setBody("{\"error\":{\"type\":\"OAuthException\",\"code\":200,\"message\":\"(#200) Requires pages_manage_ads permission\"}}")
            .addHeader("Content-Type", "application/json"));

        assertThrows(
            FacebookPermissionException.class,
            () -> service.findInstantFormIdentifier("765", "Form")
        );
    }

    @Test
    void publishInstantFormSkipsPostWhenAlreadyActive() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"status\":\"ACTIVE\"}")
            .addHeader("Content-Type", "application/json"));

        service.publishInstantForm("123");

        RecordedRequest statusCheck = server.takeRequest();
        assertEquals("GET", statusCheck.getMethod());
        assertEquals("/v23.0/123?access_token=token", statusCheck.getPath());
        assertNull(server.takeRequest(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void publishInstantFormPostsWhenStatusIsNotActive() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"status\":\"DRAFT\"}")
            .addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setBody("{\"success\":true}")
            .addHeader("Content-Type", "application/json"));

        service.publishInstantForm(" 123 ");

        RecordedRequest statusCheck = server.takeRequest();
        assertEquals("GET", statusCheck.getMethod());
        assertEquals("/v23.0/123?access_token=token", statusCheck.getPath());

        RecordedRequest publishRequest = server.takeRequest();
        assertEquals("POST", publishRequest.getMethod());
        assertEquals("/v23.0/123", publishRequest.getPath());
        JsonNode body = objectMapper.readTree(publishRequest.getBody().inputStream());
        assertEquals("ACTIVE", body.get("status").asText());
        assertEquals("token", body.get("access_token").asText());
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
