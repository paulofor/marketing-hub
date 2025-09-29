package com.marketinghub.facebookadsworker.facebookcampaign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookAccessTokenManager;
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
import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FacebookCampaignServiceTest {
    private MockWebServer backend;
    private MockWebServer facebook;
    private FacebookCampaignService service;
    private ObjectMapper objectMapper;
    private FacebookAdsService adsService;

    @BeforeEach
    void setUp() throws IOException {
        backend = new MockWebServer();
        backend.start();
        facebook = new MockWebServer();
        facebook.start();

        String backendUrl = backend.url("/").toString();
        String facebookUrl = facebook.url("/").toString();

        objectMapper = new ObjectMapper();
        adsService = new FacebookAdsService(
            WebClient.builder(),
            facebookUrl,
            "token",
            "v23.0",
            objectMapper
        );
        FacebookAccessTokenManager accessTokenManager = new FacebookAccessTokenManager(
            adsService,
            "",
            ""
        );
        service = new FacebookCampaignService(
            adsService,
            accessTokenManager,
            WebClient.builder(),
            backendUrl,
            "/api",
            "1",
            "2000",
            "IMPRESSIONS",
            "LINK_CLICKS",
            "WEBSITE",
            "LOWEST_COST_WITHOUT_CAP",
            "150",
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
        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"pageId\":\"84\"}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueue(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueue(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        RecordedRequest get = backend.takeRequest();
        assertEquals("/api/facebook-campaigns/experiments-ready", get.getPath());

        RecordedRequest creativesGet = backend.takeRequest();
        assertEquals("/api/experiments/1/creatives", creativesGet.getPath());

        RecordedRequest postCampaign = facebook.takeRequest();
        assertEquals("/v23.0/act_1/campaigns", postCampaign.getPath());

        RecordedRequest postAdSet = facebook.takeRequest();
        assertEquals("/v23.0/act_1/adsets", postAdSet.getPath());
        JsonNode adSetPayload = objectMapper.readTree(postAdSet.getBody().inputStream());
        assertEquals("Exp - Ad Set", adSetPayload.get("name").asText());
        assertEquals("10", adSetPayload.get("campaign_id").asText());
        assertEquals("2000", adSetPayload.get("daily_budget").asText());
        assertEquals("LOWEST_COST_WITHOUT_CAP", adSetPayload.get("bid_strategy").asText());
        assertEquals("150", adSetPayload.get("bid_amount").asText());
        assertEquals("84", adSetPayload.get("promoted_object").get("page_id").asText());

        RecordedRequest postCreative = facebook.takeRequest();
        assertEquals("/v23.0/act_1/adcreatives", postCreative.getPath());
        JsonNode creativePayload = objectMapper.readTree(postCreative.getBody().inputStream());
        assertEquals("Exp - Creative", creativePayload.get("name").asText());
        JsonNode storySpec = creativePayload.get("object_story_spec");
        assertEquals("84", storySpec.get("page_id").asText());
        JsonNode linkData = storySpec.get("link_data");
        assertEquals("Texto Criativo", linkData.get("message").asText());
        assertEquals("https://exp.example/landing", linkData.get("link").asText());
        assertEquals("SHOP_NOW", linkData.get("call_to_action").get("type").asText());
        assertEquals("HL", linkData.get("name").asText());
        assertEquals("Desc", linkData.get("description").asText());

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
    void marksExperimentAsFailedWhenFacebookReturnsPermissionError() throws Exception {
        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"pageId\":\"84\"}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":{\"message\":\"Permissions error\",\"type\":\"OAuthException\",\"code\":200,\"error_subcode\":1815066,\"error_user_msg\":\"O usuário não tem permissão para criar anúncios com esta conta de anúncios\"}}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueue(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueue(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        RecordedRequest get = backend.takeRequest();
        assertEquals("GET", get.getMethod());
        assertEquals("/api/facebook-campaigns/experiments-ready", get.getPath());

        RecordedRequest creativesGet = backend.takeRequest();
        assertEquals("GET", creativesGet.getMethod());
        assertEquals("/api/experiments/1/creatives", creativesGet.getPath());

        RecordedRequest postCampaign = facebook.takeRequest();
        assertEquals("POST", postCampaign.getMethod());
        assertEquals("/v23.0/act_1/campaigns", postCampaign.getPath());
        assertEquals(1, facebook.getRequestCount());

        RecordedRequest patch = backend.takeRequest();
        assertEquals("PATCH", patch.getMethod());
        assertEquals("/api/experiments/1/status?status=FAILED", patch.getPath());
        assertEquals(3, backend.getRequestCount());
    }

    @Test
    void skipsExperimentAfterPermissionErrorEvenIfBackendKeepsReturningIt() throws Exception {
        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"pageId\":\"84\"}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":{\"message\":\"Permissions error\",\"type\":\"OAuthException\",\"code\":200,\"error_subcode\":1815066}}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueue(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueue(new MockResponse().setResponseCode(500));
        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\"}]")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();
        service.createCampaignsFromExperiments();

        RecordedRequest firstGet = backend.takeRequest();
        assertEquals("GET", firstGet.getMethod());
        assertEquals("/api/facebook-campaigns/experiments-ready", firstGet.getPath());

        RecordedRequest firstCreativesGet = backend.takeRequest();
        assertEquals("GET", firstCreativesGet.getMethod());
        assertEquals("/api/experiments/1/creatives", firstCreativesGet.getPath());

        RecordedRequest campaignPost = facebook.takeRequest();
        assertEquals("POST", campaignPost.getMethod());
        assertEquals("/v23.0/act_1/campaigns", campaignPost.getPath());

        RecordedRequest failedPatch = backend.takeRequest();
        assertEquals("PATCH", failedPatch.getMethod());
        assertEquals("/api/experiments/1/status?status=FAILED", failedPatch.getPath());

        RecordedRequest secondGet = backend.takeRequest();
        assertEquals("GET", secondGet.getMethod());
        assertEquals("/api/facebook-campaigns/experiments-ready", secondGet.getPath());

        assertEquals(1, facebook.getRequestCount());
        assertEquals(4, backend.getRequestCount());
    }

    @Test
    void resumesProcessingAfterAutomaticTokenRenewalWhenPreviouslyExpired() throws Exception {
        StubFacebookAccessTokenManager renewingManager = new StubFacebookAccessTokenManager(adsService);
        renewingManager.enqueue(new FacebookAccessTokenManager.RenewalAttemptResult(
            FacebookAccessTokenManager.RenewalOutcome.NOT_CONFIGURED,
            null,
            null
        ));
        renewingManager.enqueue(new FacebookAccessTokenManager.RenewalAttemptResult(
            FacebookAccessTokenManager.RenewalOutcome.SUCCESS,
            "fresh-token",
            null
        ));

        service = new FacebookCampaignService(
            adsService,
            renewingManager,
            WebClient.builder(),
            backend.url("/").toString(),
            "/api",
            "1",
            "2000",
            "IMPRESSIONS",
            "LINK_CLICKS",
            "WEBSITE",
            "LOWEST_COST_WITHOUT_CAP",
            "150",
            "BR",
            "42",
            "11",
            "https://example.com",
            "Conheça %s",
            "LEARN_MORE"
        );

        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"pageId\":\"84\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueue(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":{\"message\":\"Error validating access token: Session has expired\",\"type\":\"OAuthException\",\"code\":190,\"error_subcode\":463}}")
            .addHeader("Content-Type", "application/json"));

        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"pageId\":\"84\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueue(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
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
        service.createCampaignsFromExperiments();

        RecordedRequest firstExperimentRequest = backend.takeRequest();
        assertEquals("/api/facebook-campaigns/experiments-ready", firstExperimentRequest.getPath());
        RecordedRequest firstCreativeRequest = backend.takeRequest();
        assertEquals("/api/experiments/1/creatives", firstCreativeRequest.getPath());
        RecordedRequest expiredCampaignRequest = facebook.takeRequest();
        assertEquals("/v23.0/act_1/campaigns", expiredCampaignRequest.getPath());

        RecordedRequest secondExperimentRequest = backend.takeRequest();
        assertEquals("/api/facebook-campaigns/experiments-ready", secondExperimentRequest.getPath());
        RecordedRequest secondCreativeRequest = backend.takeRequest();
        assertEquals("/api/experiments/1/creatives", secondCreativeRequest.getPath());
        RecordedRequest renewedCampaignRequest = facebook.takeRequest();
        assertEquals("/v23.0/act_1/campaigns", renewedCampaignRequest.getPath());

        JsonNode renewedPayload = objectMapper.readTree(renewedCampaignRequest.getBody().inputStream());
        assertEquals("fresh-token", renewedPayload.get("access_token").asText());

        RecordedRequest adSetRequest = facebook.takeRequest();
        assertEquals("/v23.0/act_1/adsets", adSetRequest.getPath());
        RecordedRequest creativeRequest = facebook.takeRequest();
        assertEquals("/v23.0/act_1/adcreatives", creativeRequest.getPath());
        RecordedRequest adRequest = facebook.takeRequest();
        assertEquals("/v23.0/act_1/ads", adRequest.getPath());

        RecordedRequest finalBackendPost = backend.takeRequest();
        assertEquals("/api/facebook-campaigns", finalBackendPost.getPath());
    }

    private static class StubFacebookAccessTokenManager extends FacebookAccessTokenManager {
        private final Queue<FacebookAccessTokenManager.RenewalAttemptResult> results = new ArrayDeque<>();
        private final FacebookAdsService adsService;

        StubFacebookAccessTokenManager(FacebookAdsService adsService) {
            super(adsService, "", "");
            this.adsService = adsService;
        }

        void enqueue(FacebookAccessTokenManager.RenewalAttemptResult result) {
            results.add(result);
        }

        @Override
        public FacebookAccessTokenManager.RenewalAttemptResult tryRenewAccessTokenIfPossible() {
            FacebookAccessTokenManager.RenewalAttemptResult result = results.isEmpty()
                ? new FacebookAccessTokenManager.RenewalAttemptResult(
                    FacebookAccessTokenManager.RenewalOutcome.NOT_CONFIGURED,
                    null,
                    null
                )
                : results.poll();
            if (result.outcome() == FacebookAccessTokenManager.RenewalOutcome.SUCCESS && result.newToken() != null) {
                adsService.updateAccessToken(result.newToken());
            }
            return result;
        }
    }
}
