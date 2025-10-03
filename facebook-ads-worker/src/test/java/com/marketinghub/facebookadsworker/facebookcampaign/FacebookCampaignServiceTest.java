package com.marketinghub.facebookadsworker.facebookcampaign;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookAccessTokenManager;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient.FacebookWorkerConfiguration;
import com.marketinghub.facebookadsworker.facebooktokenrenewal.FacebookTokenRenewalClient;
import com.marketinghub.facebookadsworker.facebooktokenrenewal.FacebookTokenRenewalService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FacebookCampaignServiceTest {
    private MockWebServer backend;
    private MockWebServer facebook;
    private FacebookCampaignService service;
    private ObjectMapper objectMapper;
    private FacebookAdsService adsService;
    private StubFacebookWorkerConfigurationClient configurationClient;

    @BeforeEach
    void setUp() throws IOException {
        backend = new MockWebServer();
        backend.start();
        facebook = new MockWebServer();
        facebook.start();

        String facebookUrl = facebook.url("/").toString();

        objectMapper = new ObjectMapper();
        configurationClient = new StubFacebookWorkerConfigurationClient();
        configurationClient.setConfiguration(configurationWithAccessToken("token"));
        adsService = new FacebookAdsService(
            WebClient.builder(),
            facebookUrl,
            "v23.0",
            objectMapper
        );
        adsService.updateAccessToken("token");
        FacebookTokenRenewalClient tokenRenewalClient = new FacebookTokenRenewalClient(
            WebClient.builder(),
            backend.url("/").toString(),
            "/api"
        );
        FacebookTokenRenewalService tokenRenewalService = new FacebookTokenRenewalService(
            WebClient.builder(),
            adsService,
            tokenRenewalClient,
            backend.url("/").toString(),
            "/api"
        );
        FacebookAccessTokenManager accessTokenManager = new FacebookAccessTokenManager(
            adsService,
            configurationClient,
            tokenRenewalService
        );
        service = new FacebookCampaignService(
            adsService,
            accessTokenManager,
            WebClient.builder(),
            configurationClient,
            backend.url("/").toString(),
            "/api"
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        backend.shutdown();
        facebook.shutdown();
    }

    @Test
    void createsCampaignHierarchyForEachExperiment() throws Exception {
        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"}}]")
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
        assertEquals("WEBSITE", adSetPayload.get("destination_type").asText());
        assertEquals("42", adSetPayload.get("promoted_object").get("page_id").asText());

        RecordedRequest postCreative = facebook.takeRequest();
        assertEquals("/v23.0/act_1/adcreatives", postCreative.getPath());
        JsonNode creativePayload = objectMapper.readTree(postCreative.getBody().inputStream());
        assertEquals("Exp - Creative", creativePayload.get("name").asText());
        JsonNode storySpec = creativePayload.get("object_story_spec");
        assertEquals("42", storySpec.get("page_id").asText());
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
    void createsLeadGenCampaignWhenCreativeProvidesFormId() throws Exception {
        configurationClient.setConfiguration(new FacebookWorkerConfiguration(
            1L,
            "1",
            "token",
            "app",
            "secret",
            "42",
            "11",
            null,
            "987654321098765",
            "Conheça %s",
            "SIGN_UP",
            "2000",
            "IMPRESSIONS",
            "LEAD_GENERATION",
            "WEBSITE",
            "LOWEST_COST_WITHOUT_CAP",
            "150",
            "BR"
        ));
        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueue(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SIGN_UP\",\"leadGenFormId\":\"321123321123321\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueue(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        facebook.takeRequest(); // campaign
        RecordedRequest adSetRequest = facebook.takeRequest();
        JsonNode adSetPayload = objectMapper.readTree(adSetRequest.getBody().inputStream());
        assertEquals("LEAD_GENERATION", adSetPayload.get("destination_type").asText());

        RecordedRequest creativeRequest = facebook.takeRequest();
        JsonNode creativePayload = objectMapper.readTree(creativeRequest.getBody().inputStream());
        JsonNode linkData = creativePayload.get("object_story_spec").get("link_data");
        JsonNode cta = linkData.get("call_to_action");
        assertEquals("SIGN_UP", cta.get("type").asText());
        assertEquals("321123321123321", cta.get("value").get("lead_gen_form_id").asText());

        backend.takeRequest(); // experiments-ready
        backend.takeRequest(); // creatives fetch
        RecordedRequest backendReport = backend.takeRequest();
        JsonNode reportedCreative = objectMapper.readTree(backendReport.getBody().inputStream())
            .get("adCreative");
        assertEquals("321123321123321", reportedCreative.get("leadGenFormId").asText());
    }

    @Test
    void ignoresConnectionIssuesFetchingExperiments() {
        backend.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        service.createCampaignsFromExperiments();

        assertEquals(1, backend.getRequestCount());
        assertEquals(0, facebook.getRequestCount());
    }

    @Test
    void logsConfigurationUnavailableWarningOnlyOncePerOutage() {
        configurationClient.setConfiguration(null);

        Logger logger = (Logger) LoggerFactory.getLogger(FacebookCampaignService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            service.createCampaignsFromExperiments();
            service.createCampaignsFromExperiments();

            backend.enqueue(new MockResponse().setResponseCode(404));
            configurationClient.setConfiguration(configurationWithAccessToken("token"));
            service.createCampaignsFromExperiments();

            configurationClient.setConfiguration(null);
            service.createCampaignsFromExperiments();

            List<ILoggingEvent> warnings = appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .toList();

            assertEquals(2, warnings.size());
            warnings.forEach(event -> assertEquals(
                "Facebook worker configuration is unavailable; skipping campaign creation",
                event.getFormattedMessage()
            ));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void marksExperimentAsFailedWhenFacebookReturnsPermissionError() throws Exception {
        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"}}]")
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
        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"}}]")
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
        StubFacebookAccessTokenManager renewingManager = new StubFacebookAccessTokenManager(adsService, configurationClient);
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
            configurationClient,
            backend.url("/").toString(),
            "/api"
        );

        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueue(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":{\"message\":\"Error validating access token: Session has expired\",\"type\":\"OAuthException\",\"code\":190,\"error_subcode\":463}}")
            .addHeader("Content-Type", "application/json"));

        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"}}]")
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
        configurationClient.setConfiguration(configurationWithAccessToken("fresh-token"));
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

    @Test
    void resumesProcessingAfterBackendRenewsTokenWithoutAppCredentials() throws Exception {
        StubFacebookAccessTokenManager manager = new StubFacebookAccessTokenManager(adsService, configurationClient);
        manager.enqueue(new FacebookAccessTokenManager.RenewalAttemptResult(
            FacebookAccessTokenManager.RenewalOutcome.NOT_CONFIGURED,
            null,
            null
        ));
        manager.enqueue(new FacebookAccessTokenManager.RenewalAttemptResult(
            FacebookAccessTokenManager.RenewalOutcome.NOT_CONFIGURED,
            null,
            null
        ));

        service = new FacebookCampaignService(
            adsService,
            manager,
            WebClient.builder(),
            configurationClient,
            backend.url("/").toString(),
            "/api"
        );

        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueue(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":{\"message\":\"Error validating access token: Session has expired\",\"type\":\"OAuthException\",\"code\":190,\"error_subcode\":463}}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        adsService.updateAccessToken("renewed-by-backend");
        configurationClient.setConfiguration(configurationWithAccessToken("renewed-by-backend"));

        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"}}]")
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

        RecordedRequest firstExperiment = backend.takeRequest();
        assertEquals("/api/facebook-campaigns/experiments-ready", firstExperiment.getPath());
        RecordedRequest firstCreative = backend.takeRequest();
        assertEquals("/api/experiments/1/creatives", firstCreative.getPath());
        RecordedRequest expiredCampaign = facebook.takeRequest();
        assertEquals("/v23.0/act_1/campaigns", expiredCampaign.getPath());

        RecordedRequest secondExperiment = backend.takeRequest();
        assertEquals("/api/facebook-campaigns/experiments-ready", secondExperiment.getPath());
        RecordedRequest secondCreative = backend.takeRequest();
        assertEquals("/api/experiments/1/creatives", secondCreative.getPath());
        RecordedRequest renewedCampaign = facebook.takeRequest();
        assertEquals("/v23.0/act_1/campaigns", renewedCampaign.getPath());

        JsonNode renewedPayload = objectMapper.readTree(renewedCampaign.getBody().inputStream());
        assertEquals("renewed-by-backend", renewedPayload.get("access_token").asText());

        facebook.takeRequest(); // ad set
        facebook.takeRequest(); // creative
        facebook.takeRequest(); // ad

        RecordedRequest backendPost = backend.takeRequest();
        assertEquals("/api/facebook-campaigns", backendPost.getPath());
    }

    @Test
    void fallsBackToExperimentFacebookPageWhenConfigurationDoesNotProvideOne() throws Exception {
        configurationClient.setConfiguration(configurationWithoutDefaultPageId("token"));

        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"}}]")
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

        facebook.takeRequest(); // campaign
        RecordedRequest adSetRequest = facebook.takeRequest();
        JsonNode adSetPayload = objectMapper.readTree(adSetRequest.getBody().inputStream());
        assertEquals("84", adSetPayload.get("promoted_object").get("page_id").asText());

        RecordedRequest creativeRequest = facebook.takeRequest();
        JsonNode creativePayload = objectMapper.readTree(creativeRequest.getBody().inputStream());
        assertEquals("84", creativePayload.get("object_story_spec").get("page_id").asText());
    }

    @Test
    void skipsExperimentWhenNoPageCanBeResolved() throws Exception {
        configurationClient.setConfiguration(configurationWithoutDefaultPageId("token"));

        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"pageId\":\"84\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueue(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        RecordedRequest experimentsRequest = backend.takeRequest();
        assertEquals("/api/facebook-campaigns/experiments-ready", experimentsRequest.getPath());
        RecordedRequest creativesRequest = backend.takeRequest();
        assertEquals("/api/experiments/1/creatives", creativesRequest.getPath());
        assertEquals(0, facebook.getRequestCount());
    }

    private FacebookWorkerConfiguration configurationWithAccessToken(String accessToken) {
        return new FacebookWorkerConfiguration(
            1L,
            "1",
            accessToken,
            "app",
            "secret",
            "42",
            "11",
            "https://example.com",
            null,
            "Conheça %s",
            "LEARN_MORE",
            "2000",
            "IMPRESSIONS",
            "LINK_CLICKS",
            "WEBSITE",
            "LOWEST_COST_WITHOUT_CAP",
            "150",
            "BR"
        );
    }

    private FacebookWorkerConfiguration configurationWithoutDefaultPageId(String accessToken) {
        return new FacebookWorkerConfiguration(
            1L,
            "1",
            accessToken,
            "app",
            "secret",
            null,
            "11",
            "https://example.com",
            null,
            "Conheça %s",
            "LEARN_MORE",
            "2000",
            "IMPRESSIONS",
            "LINK_CLICKS",
            "WEBSITE",
            "LOWEST_COST_WITHOUT_CAP",
            "150",
            "BR"
        );
    }

    private static class StubFacebookWorkerConfigurationClient extends FacebookWorkerConfigurationClient {
        private FacebookWorkerConfiguration configuration;

        StubFacebookWorkerConfigurationClient() {
            super(WebClient.builder(), "http://localhost", "/api");
        }

        void setConfiguration(FacebookWorkerConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public Optional<FacebookWorkerConfiguration> fetchConfiguration() {
            return Optional.ofNullable(configuration);
        }
    }

    private static class StubFacebookAccessTokenManager extends FacebookAccessTokenManager {
        private final Queue<FacebookAccessTokenManager.RenewalAttemptResult> results = new ArrayDeque<>();
        private final FacebookAdsService adsService;

        StubFacebookAccessTokenManager(
            FacebookAdsService adsService,
            FacebookWorkerConfigurationClient configurationClient
        ) {
            super(
                adsService,
                configurationClient,
                new FacebookTokenRenewalService(
                    WebClient.builder(),
                    adsService,
                    new FacebookTokenRenewalClient(WebClient.builder(), "http://localhost", "/api"),
                    "http://localhost",
                    "/api"
                )
            );
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
