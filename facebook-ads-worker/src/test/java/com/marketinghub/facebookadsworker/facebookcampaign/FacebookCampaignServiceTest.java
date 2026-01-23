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
import com.marketinghub.facebookadsworker.testsupport.FailFastMockWebServer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(value = 15, unit = TimeUnit.SECONDS)
class FacebookCampaignServiceTest {
    private FailFastMockWebServer backend;
    private FailFastMockWebServer facebook;
    private FacebookCampaignService service;
    private ObjectMapper objectMapper;
    private FacebookAdsService adsService;
    private StubFacebookWorkerConfigurationClient configurationClient;
    private WebClient.Builder webClientBuilder;

    @BeforeEach
    void setUp() throws IOException {
        backend = new FailFastMockWebServer();
        backend.start();
        facebook = new FailFastMockWebServer();
        facebook.start();

        String facebookUrl = facebook.url("/").toString();

        objectMapper = new ObjectMapper();
        configurationClient = new StubFacebookWorkerConfigurationClient();
        configurationClient.setConfiguration(configurationWithAccessToken("token"));
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(5));
        webClientBuilder = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient));
        adsService = new FacebookAdsService(
            webClientBuilder,
            facebookUrl,
            "v23.0",
            objectMapper
        );
        adsService.updateAccessToken("token");
        FacebookTokenRenewalClient tokenRenewalClient = new FacebookTokenRenewalClient(
            webClientBuilder,
            backend.url("/").toString(),
            "/api"
        );
        FacebookTokenRenewalService tokenRenewalService = new FacebookTokenRenewalService(
            webClientBuilder,
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
            webClientBuilder,
            configurationClient,
            backend.url("/").toString(),
            "/api",
            objectMapper
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        backend.assertNoUnmatchedRequests();
        facebook.assertNoUnmatchedRequests();
        backend.shutdown();
        facebook.shutdown();
    }

    private RecordedRequest takeBackendRequest(String description) throws InterruptedException {
        RecordedRequest request = backend.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request, "Expected backend request (" + description + ") within timeout.");
        return request;
    }

    private RecordedRequest takeFacebookRequest(String description) throws InterruptedException {
        RecordedRequest request = facebook.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request, "Expected Facebook request (" + description + ") within timeout.");
        return request;
    }

    @Test
    void createsCampaignHierarchyForEachExperiment() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"dailyBudget\":25.0,\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        RecordedRequest get = takeBackendRequest("backend request");
        assertEquals("/api/facebook-campaigns/experiments-ready", get.getPath());

        RecordedRequest creativesGet = takeBackendRequest("backend request");
        assertEquals("/api/experiments/1/creatives", creativesGet.getPath());

        RecordedRequest adSetsGet = takeBackendRequest("backend request");
        assertEquals("/api/adsets?experimentId=1", adSetsGet.getPath());

        RecordedRequest postCampaign = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/campaigns", postCampaign.getPath());
        JsonNode campaignPayload = objectMapper.readTree(postCampaign.getBody().inputStream());
        assertEquals("OUTCOME_TRAFFIC", campaignPayload.get("objective").asText());

        RecordedRequest postAdSet = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/adsets", postAdSet.getPath());
        JsonNode adSetPayload = objectMapper.readTree(postAdSet.getBody().inputStream());
        assertEquals("Exp - Ad Set", adSetPayload.get("name").asText());
        assertEquals("10", adSetPayload.get("campaign_id").asText());
        assertEquals("2500", adSetPayload.get("daily_budget").asText());
        assertEquals("LOWEST_COST_WITHOUT_CAP", adSetPayload.get("bid_strategy").asText());
        assertEquals("150", adSetPayload.get("bid_amount").asText());
        assertEquals("WEBSITE", adSetPayload.get("destination_type").asText());
        assertEquals("42", adSetPayload.get("promoted_object").get("page_id").asText());
        JsonNode targeting = adSetPayload.get("targeting");
        assertEquals("BR", targeting.get("geo_locations").get("countries").get(0).asText());
        assertEquals(1, targeting.get("targeting_automation").get("advantage_audience").asInt());

        RecordedRequest postCreative = takeFacebookRequest("facebook request");
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
        assertEquals("https://cdn.example/img.jpg", linkData.get("picture").asText());
        assertFalse(linkData.has("image_hash"));

        RecordedRequest postAd = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/ads", postAd.getPath());
        JsonNode adPayload = objectMapper.readTree(postAd.getBody().inputStream());
        assertEquals("Exp - Ad", adPayload.get("name").asText());
        assertEquals("20", adPayload.get("adset_id").asText());
        assertEquals("30", adPayload.get("creative").get("creative_id").asText());

        RecordedRequest postBackend = takeBackendRequest("backend request");
        assertEquals("/api/facebook-campaigns", postBackend.getPath());

        RecordedRequest runningPatch = takeBackendRequest("backend request");
        assertEquals("PATCH", runningPatch.getMethod());
        assertEquals("/api/experiments/1/status?status=RUNNING", runningPatch.getPath());
    }

    @Test
    void createsSavedAudienceWhenTargetingExists() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"AUD123\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":900,\"experimentId\":1,\"location\":\"São Paulo\",\"targetingJson\":\"{\\\"geo_locations\\\":{\\\"countries\\\":[\\\"BR\\\"]}}\",\"prompt\":\"Detalhes\",\"model\":\"gpt-4\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        takeBackendRequest("backend request"); // experiments ready
        takeBackendRequest("backend request"); // creatives
        RecordedRequest adSetsRequest = takeBackendRequest("backend request");
        assertEquals("/api/adsets?experimentId=1", adSetsRequest.getPath());

        RecordedRequest campaignRequest = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/campaigns", campaignRequest.getPath());

        RecordedRequest savedAudienceRequest = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/saved_audiences", savedAudienceRequest.getPath());
        JsonNode savedAudienceBody = objectMapper.readTree(savedAudienceRequest.getBody().inputStream());
        assertEquals("Exp - Audience São Paulo", savedAudienceBody.get("name").asText());
        assertEquals("Detalhes", savedAudienceBody.get("description").asText());
        assertEquals("BR", savedAudienceBody.get("targeting").get("geo_locations").get("countries").get(0).asText());
        assertEquals(
            1,
            savedAudienceBody
                .get("targeting")
                .get("targeting_automation")
                .get("advantage_audience")
                .asInt()
        );

        RecordedRequest adSetRequest = takeFacebookRequest("facebook request");
        JsonNode targeting = objectMapper.readTree(adSetRequest.getBody().inputStream()).get("targeting");
        assertEquals("AUD123", targeting.get("saved_audience_id").asText());
        assertEquals("BR", targeting.get("geo_locations").get("countries").get(0).asText());
        assertEquals(1, targeting.get("targeting_automation").get("advantage_audience").asInt());

        takeFacebookRequest("facebook request"); // ad creative
        takeFacebookRequest("facebook request"); // ad

        takeBackendRequest("backend request"); // report campaign
        takeBackendRequest("backend request"); // mark running
    }

    @Test
    void usesInstantFormShareLinkWhenJourneyRequiresForm() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("[{"
            + "\"id\":1,\"name\":\"Exp\",\"pageId\":\"84\","
            + "\"dailyBudget\":35.5,"
            + "\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},"
            + "\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"},"
            + "\"facebookInstantForm\":{\"id\":33,\"facebookFormId\":\"987654321\",\"name\":\"Lead\",\"status\":\"DRAFT\",\"approved\":true,\"published\":false},"
            + "\"nextStepInstantForm\":true}]" )
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"status\":\"DRAFT\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"success\":true}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"status\":\"ACTIVE\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        RecordedRequest experimentRequest = takeBackendRequest("backend request");
        assertEquals("/api/facebook-campaigns/experiments-ready", experimentRequest.getPath());

        RecordedRequest creativesRequest = takeBackendRequest("backend request");
        assertEquals("/api/experiments/1/creatives", creativesRequest.getPath());

        RecordedRequest statusCheckRequest = takeFacebookRequest("facebook request");
        assertEquals("GET", statusCheckRequest.getMethod());
        assertTrue(statusCheckRequest.getPath().contains("987654321"));

        RecordedRequest publishRequest = takeFacebookRequest("facebook request");
        assertEquals("POST", publishRequest.getMethod());
        assertTrue(publishRequest.getPath().endsWith("/987654321"));

        RecordedRequest fetchRequest = takeFacebookRequest("facebook request");
        assertEquals("GET", fetchRequest.getMethod());
        assertTrue(fetchRequest.getPath().contains("987654321"));

        RecordedRequest campaignRequest = takeFacebookRequest("facebook request");
        JsonNode campaignPayload = objectMapper.readTree(campaignRequest.getBody().inputStream());
        assertEquals("OUTCOME_LEADS", campaignPayload.get("objective").asText());
        RecordedRequest adSetRequest = takeFacebookRequest("facebook request");
        JsonNode adSetPayload = objectMapper.readTree(adSetRequest.getBody().inputStream());
        assertEquals("ON_AD", adSetPayload.get("destination_type").asText());
        assertEquals("LEAD_GENERATION", adSetPayload.get("optimization_goal").asText());
        RecordedRequest creativeRequest = takeFacebookRequest("facebook request");
        JsonNode creativePayload = objectMapper.readTree(creativeRequest.getBody().inputStream());
        JsonNode linkData = creativePayload.get("object_story_spec").get("link_data");
        assertEquals("https://www.facebook.com/ads/leadgen/?id=987654321", linkData.get("link").asText());
        JsonNode cta = linkData.get("call_to_action");
        assertNotNull(cta);
        JsonNode ctaValue = cta.get("value");
        assertNotNull(ctaValue);
        assertEquals("987654321", ctaValue.get("lead_gen_form_id").asText());
        takeFacebookRequest("facebook request"); // ad

        RecordedRequest backendReport = takeBackendRequest("backend request");
        assertEquals("/api/facebook-campaigns", backendReport.getPath());

        RecordedRequest publicationPatch = takeBackendRequest("backend request");
        assertEquals("PATCH", publicationPatch.getMethod());
        assertEquals("/api/instant-forms/33/publication", publicationPatch.getPath());
        JsonNode patchPayload = objectMapper.readTree(publicationPatch.getBody().inputStream());
        assertTrue(patchPayload.get("published").asBoolean());
        assertEquals("https://www.facebook.com/ads/leadgen/?id=987654321", patchPayload.get("shareLink").asText());
        assertEquals("987654321", patchPayload.get("facebookFormId").asText());

        RecordedRequest runningPatch = takeBackendRequest("backend request");
        assertEquals("PATCH", runningPatch.getMethod());
        assertEquals("/api/experiments/1/status?status=RUNNING", runningPatch.getPath());
    }

    @Test
    void normalizesAiInstantFormIdentifierBeforeCallingFacebook() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("[{"
            + "\"id\":1,\"name\":\"Exp\",\"pageId\":\"84\","
            + "\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},"
            + "\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"},"
            + "\"facebookInstantForm\":{\"id\":33,\"facebookFormId\":\"ai_form_3_1_token\",\"name\":\"Lead\",\"status\":\"DRAFT\",\"approved\":true,\"published\":false},"
            + "\"nextStepInstantForm\":true}]" )
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"status\":\"DRAFT\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"success\":true}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"form_3_1_token\",\"status\":\"ACTIVE\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        RecordedRequest statusCheckRequest = takeFacebookRequest("facebook request");
        assertEquals("GET", statusCheckRequest.getMethod());
        assertTrue(statusCheckRequest.getPath().contains("form_3_1_token"));

        RecordedRequest publishRequest = takeFacebookRequest("facebook request");
        assertTrue(publishRequest.getPath().endsWith("/form_3_1_token"));

        RecordedRequest fetchRequest = takeFacebookRequest("facebook request");
        assertTrue(fetchRequest.getPath().contains("form_3_1_token"));

        RecordedRequest campaignRequest = takeFacebookRequest("facebook request");
        JsonNode campaignPayload = objectMapper.readTree(campaignRequest.getBody().inputStream());
        assertEquals("OUTCOME_LEADS", campaignPayload.get("objective").asText());
        RecordedRequest adSetRequest = takeFacebookRequest("facebook request");
        JsonNode adSetPayload = objectMapper.readTree(adSetRequest.getBody().inputStream());
        assertEquals("ON_AD", adSetPayload.get("destination_type").asText());
        assertEquals("LEAD_GENERATION", adSetPayload.get("optimization_goal").asText());

        RecordedRequest creativeRequest = takeFacebookRequest("facebook request");
        JsonNode creativePayload = objectMapper.readTree(creativeRequest.getBody().inputStream());
        JsonNode linkData = creativePayload.get("object_story_spec").get("link_data");
        assertEquals("https://www.facebook.com/ads/leadgen/?id=form_3_1_token", linkData.get("link").asText());
        assertEquals("form_3_1_token", linkData.get("call_to_action").get("value").get("lead_gen_form_id").asText());

        takeBackendRequest("backend request"); // experiments-ready
        takeBackendRequest("backend request"); // creatives fetch
        takeBackendRequest("backend request"); // campaign report
        RecordedRequest publicationPatch = takeBackendRequest("backend request");
        JsonNode patchPayload = objectMapper.readTree(publicationPatch.getBody().inputStream());
        assertEquals("https://www.facebook.com/ads/leadgen/?id=form_3_1_token", patchPayload.get("shareLink").asText());
        assertEquals("form_3_1_token", patchPayload.get("facebookFormId").asText());

        RecordedRequest runningPatch = takeBackendRequest("backend request");
        assertEquals("PATCH", runningPatch.getMethod());
        assertEquals("/api/experiments/1/status?status=RUNNING", runningPatch.getPath());
    }


    @Test
    void publishesInstantFormUsingShareLinkWhenFormIdMissing() throws Exception {
        backend.enqueueResponse(new MockResponse()
            .setBody(
                """
                [{"id":1,"name":"Exp","pageId":"84",
                  "facebookPage":{"id":9,"pageId":"84","name":"Estúdio"},
                  "instagramAccount":{"id":55,"handle":"@estudio","code":"IG-EST","name":"Estúdio"},
                  "facebookInstantForm":{"id":33,"facebookFormId":null,"name":"Lead","status":"DRAFT","approved":true,"published":false,"shareLink":"https://www.facebook.com/ads/leadgen/?id=2468"},
                  "nextStepInstantForm":true}]
                """
            )
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"status\":\"DRAFT\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"success\":true}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"2468\",\"status\":\"ACTIVE\",\"share_link\":\"https://www.facebook.com/ads/leadgen/?id=2468\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        RecordedRequest statusCheckRequest = takeFacebookRequest("facebook request");
        assertEquals("GET", statusCheckRequest.getMethod());
        assertTrue(statusCheckRequest.getPath().contains("2468"));

        RecordedRequest publishRequest = takeFacebookRequest("facebook request");
        assertTrue(publishRequest.getPath().endsWith("/2468"));

        RecordedRequest fetchRequest = takeFacebookRequest("facebook request");
        assertTrue(fetchRequest.getPath().contains("2468"));

        RecordedRequest campaignRequest = takeFacebookRequest("facebook request");
        JsonNode campaignPayload = objectMapper.readTree(campaignRequest.getBody().inputStream());
        assertEquals("OUTCOME_LEADS", campaignPayload.get("objective").asText());

        RecordedRequest adSetRequest = takeFacebookRequest("facebook request");
        JsonNode adSetPayload = objectMapper.readTree(adSetRequest.getBody().inputStream());
        assertEquals("ON_AD", adSetPayload.get("destination_type").asText());
        assertEquals("LEAD_GENERATION", adSetPayload.get("optimization_goal").asText());

        RecordedRequest creativeRequest = takeFacebookRequest("facebook request");
        JsonNode creativePayload = objectMapper.readTree(creativeRequest.getBody().inputStream());
        JsonNode linkData = creativePayload.get("object_story_spec").get("link_data");
        assertEquals("https://www.facebook.com/ads/leadgen/?id=2468", linkData.get("link").asText());
        assertEquals("2468", linkData.get("call_to_action").get("value").get("lead_gen_form_id").asText());

        takeBackendRequest("backend request"); // experiments-ready
        takeBackendRequest("backend request"); // creatives fetch
        takeBackendRequest("backend request"); // campaign report
        RecordedRequest publicationPatch = takeBackendRequest("backend request");
        JsonNode patchPayload = objectMapper.readTree(publicationPatch.getBody().inputStream());
        assertTrue(patchPayload.get("published").asBoolean());
        assertEquals("https://www.facebook.com/ads/leadgen/?id=2468", patchPayload.get("shareLink").asText());
        assertEquals("2468", patchPayload.get("facebookFormId").asText());

        RecordedRequest runningPatch = takeBackendRequest("backend request");
        assertEquals("PATCH", runningPatch.getMethod());
        assertEquals("/api/experiments/1/status?status=RUNNING", runningPatch.getPath());
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
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SIGN_UP\",\"leadGenFormId\":\"321123321123321\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        RecordedRequest campaignRequest = takeFacebookRequest("facebook request");
        JsonNode campaignPayload = objectMapper.readTree(campaignRequest.getBody().inputStream());
        assertEquals("OUTCOME_LEADS", campaignPayload.get("objective").asText());
        RecordedRequest adSetRequest = takeFacebookRequest("facebook request");
        JsonNode adSetPayload = objectMapper.readTree(adSetRequest.getBody().inputStream());
        assertEquals("ON_AD", adSetPayload.get("destination_type").asText());
        assertEquals("LEAD_GENERATION", adSetPayload.get("optimization_goal").asText());

        RecordedRequest creativeRequest = takeFacebookRequest("facebook request");
        JsonNode creativePayload = objectMapper.readTree(creativeRequest.getBody().inputStream());
        JsonNode linkData = creativePayload.get("object_story_spec").get("link_data");
        JsonNode cta = linkData.get("call_to_action");
        assertEquals("SIGN_UP", cta.get("type").asText());
        assertEquals("321123321123321", cta.get("value").get("lead_gen_form_id").asText());
        assertEquals("https://cdn.example/img.jpg", linkData.get("picture").asText());
        assertFalse(linkData.has("image_hash"));

        takeBackendRequest("backend request"); // experiments-ready
        takeBackendRequest("backend request"); // creatives fetch
        RecordedRequest backendReport = takeBackendRequest("backend request");
        JsonNode reportedCreative = objectMapper.readTree(backendReport.getBody().inputStream())
            .get("adCreative");
        assertEquals("321123321123321", reportedCreative.get("leadGenFormId").asText());

        RecordedRequest runningPatch = takeBackendRequest("backend request");
        assertEquals("PATCH", runningPatch.getMethod());
        assertEquals("/api/experiments/1/status?status=RUNNING", runningPatch.getPath());
    }

    @Test
    void ignoresConnectionIssuesFetchingExperiments() {
        backend.enqueueResponse(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

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

            backend.enqueueResponse(new MockResponse().setResponseCode(404));
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
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":{\"message\":\"Permissions error\",\"type\":\"OAuthException\",\"code\":200,\"error_subcode\":1815066,\"error_user_msg\":\"O usuário não tem permissão para criar anúncios com esta conta de anúncios\"}}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        RecordedRequest get = takeBackendRequest("backend request");
        assertEquals("GET", get.getMethod());
        assertEquals("/api/facebook-campaigns/experiments-ready", get.getPath());

        RecordedRequest creativesGet = takeBackendRequest("backend request");
        assertEquals("GET", creativesGet.getMethod());
        assertEquals("/api/experiments/1/creatives", creativesGet.getPath());

        RecordedRequest postCampaign = takeFacebookRequest("facebook request");
        assertEquals("POST", postCampaign.getMethod());
        assertEquals("/v23.0/act_1/campaigns", postCampaign.getPath());
        assertEquals(1, facebook.getRequestCount());

        RecordedRequest patch = takeBackendRequest("backend request");
        assertEquals("PATCH", patch.getMethod());
        assertEquals("/api/experiments/1/status?status=FAILED", patch.getPath());
        assertEquals(3, backend.getRequestCount());
    }

    @Test
    void skipsExperimentAfterPermissionErrorEvenIfBackendKeepsReturningIt() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":{\"message\":\"Permissions error\",\"type\":\"OAuthException\",\"code\":200,\"error_subcode\":1815066}}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setResponseCode(500));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\"}]")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();
        service.createCampaignsFromExperiments();

        RecordedRequest firstGet = takeBackendRequest("backend request");
        assertEquals("GET", firstGet.getMethod());
        assertEquals("/api/facebook-campaigns/experiments-ready", firstGet.getPath());

        RecordedRequest firstCreativesGet = takeBackendRequest("backend request");
        assertEquals("GET", firstCreativesGet.getMethod());
        assertEquals("/api/experiments/1/creatives", firstCreativesGet.getPath());

        RecordedRequest campaignPost = takeFacebookRequest("facebook request");
        assertEquals("POST", campaignPost.getMethod());
        assertEquals("/v23.0/act_1/campaigns", campaignPost.getPath());

        RecordedRequest failedPatch = takeBackendRequest("backend request");
        assertEquals("PATCH", failedPatch.getMethod());
        assertEquals("/api/experiments/1/status?status=FAILED", failedPatch.getPath());

        RecordedRequest secondGet = takeBackendRequest("backend request");
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
            webClientBuilder,
            configurationClient,
            backend.url("/").toString(),
            "/api",
            objectMapper
        );

        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":{\"message\":\"Error validating access token: Session has expired\",\"type\":\"OAuthException\",\"code\":190,\"error_subcode\":463}}")
            .addHeader("Content-Type", "application/json"));

        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();
        configurationClient.setConfiguration(configurationWithAccessToken("fresh-token"));
        service.createCampaignsFromExperiments();

        RecordedRequest firstExperimentRequest = takeBackendRequest("backend request");
        assertEquals("/api/facebook-campaigns/experiments-ready", firstExperimentRequest.getPath());
        RecordedRequest firstCreativeRequest = takeBackendRequest("backend request");
        assertEquals("/api/experiments/1/creatives", firstCreativeRequest.getPath());
        RecordedRequest expiredCampaignRequest = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/campaigns", expiredCampaignRequest.getPath());

        RecordedRequest secondExperimentRequest = takeBackendRequest("backend request");
        assertEquals("/api/facebook-campaigns/experiments-ready", secondExperimentRequest.getPath());
        RecordedRequest secondCreativeRequest = takeBackendRequest("backend request");
        assertEquals("/api/experiments/1/creatives", secondCreativeRequest.getPath());
        RecordedRequest renewedCampaignRequest = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/campaigns", renewedCampaignRequest.getPath());

        JsonNode renewedPayload = objectMapper.readTree(renewedCampaignRequest.getBody().inputStream());
        assertEquals("fresh-token", renewedPayload.get("access_token").asText());

        RecordedRequest adSetRequest = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/adsets", adSetRequest.getPath());
        RecordedRequest creativeRequest = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/adcreatives", creativeRequest.getPath());
        RecordedRequest adRequest = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/ads", adRequest.getPath());

        RecordedRequest finalBackendPost = takeBackendRequest("backend request");
        assertEquals("/api/facebook-campaigns", finalBackendPost.getPath());

        RecordedRequest runningPatch = takeBackendRequest("backend request");
        assertEquals("PATCH", runningPatch.getMethod());
        assertEquals("/api/experiments/1/status?status=RUNNING", runningPatch.getPath());
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
            webClientBuilder,
            configurationClient,
            backend.url("/").toString(),
            "/api",
            objectMapper
        );

        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":{\"message\":\"Error validating access token: Session has expired\",\"type\":\"OAuthException\",\"code\":190,\"error_subcode\":463}}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        adsService.updateAccessToken("renewed-by-backend");
        configurationClient.setConfiguration(configurationWithAccessToken("renewed-by-backend"));

        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        RecordedRequest firstExperiment = takeBackendRequest("backend request");
        assertEquals("/api/facebook-campaigns/experiments-ready", firstExperiment.getPath());
        RecordedRequest firstCreative = takeBackendRequest("backend request");
        assertEquals("/api/experiments/1/creatives", firstCreative.getPath());
        RecordedRequest expiredCampaign = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/campaigns", expiredCampaign.getPath());

        RecordedRequest secondExperiment = takeBackendRequest("backend request");
        assertEquals("/api/facebook-campaigns/experiments-ready", secondExperiment.getPath());
        RecordedRequest secondCreative = takeBackendRequest("backend request");
        assertEquals("/api/experiments/1/creatives", secondCreative.getPath());
        RecordedRequest renewedCampaign = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/campaigns", renewedCampaign.getPath());

        JsonNode renewedPayload = objectMapper.readTree(renewedCampaign.getBody().inputStream());
        assertEquals("renewed-by-backend", renewedPayload.get("access_token").asText());

        RecordedRequest adSetRequest = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/adsets", adSetRequest.getPath());
        takeFacebookRequest("facebook request"); // creative
        takeFacebookRequest("facebook request"); // ad

        RecordedRequest backendPost = takeBackendRequest("backend request");
        assertEquals("/api/facebook-campaigns", backendPost.getPath());

        RecordedRequest runningPatchSecond = takeBackendRequest("backend request");
        assertEquals("PATCH", runningPatchSecond.getMethod());
        assertEquals("/api/experiments/1/status?status=RUNNING", runningPatchSecond.getPath());
    }

    @Test
    void fallsBackToExperimentFacebookPageWhenConfigurationDoesNotProvideOne() throws Exception {
        configurationClient.setConfiguration(configurationWithoutDefaultPageId("token"));

        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        RecordedRequest campaignRequest = takeFacebookRequest("facebook request");
        JsonNode campaignPayload = objectMapper.readTree(campaignRequest.getBody().inputStream());
        assertEquals("OUTCOME_TRAFFIC", campaignPayload.get("objective").asText());
        RecordedRequest adSetRequest = takeFacebookRequest("facebook request");
        JsonNode adSetPayload = objectMapper.readTree(adSetRequest.getBody().inputStream());
        assertEquals("84", adSetPayload.get("promoted_object").get("page_id").asText());

        RecordedRequest creativeRequest = takeFacebookRequest("facebook request");
        JsonNode creativePayload = objectMapper.readTree(creativeRequest.getBody().inputStream());
        assertEquals("84", creativePayload.get("object_story_spec").get("page_id").asText());
        assertEquals("https://cdn.example/img.jpg", creativePayload.get("object_story_spec").get("link_data").get("picture").asText());
        assertFalse(
            creativePayload.get("object_story_spec").get("link_data").has("image_hash")
        );

        takeBackendRequest("backend request"); // experiments-ready
        takeBackendRequest("backend request"); // creatives fetch
        takeBackendRequest("backend request"); // campaign report
        RecordedRequest runningPatch = takeBackendRequest("backend request");
        assertEquals("PATCH", runningPatch.getMethod());
        assertEquals("/api/experiments/1/status?status=RUNNING", runningPatch.getPath());
    }

    @Test
    void resolvesPageIdFromAssociatedFacebookPageAlias() throws Exception {
        configurationClient.setConfiguration(configurationWithoutDefaultPageId("token"));

        backend.enqueueResponse(
            new MockResponse()
                .setBody("[{\"id\":1,\"name\":\"Exp\",\"associatedFacebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"}}]")
                .addHeader("Content-Type", "application/json")
        );
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}").addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}").addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}").addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}").addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(
            new MockResponse()
                .setBody(
                    "[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]"
                )
                .addHeader("Content-Type", "application/json")
        );
        backend.enqueueResponse(new MockResponse().setBody("[]").addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}").addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}").addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        takeFacebookRequest("facebook request");
        RecordedRequest adSetRequest = takeFacebookRequest("facebook request");
        JsonNode adSetPayload = objectMapper.readTree(adSetRequest.getBody().inputStream());
        assertEquals("84", adSetPayload.get("promoted_object").get("page_id").asText());

        RecordedRequest creativeRequest = takeFacebookRequest("facebook request");
        JsonNode creativePayload = objectMapper.readTree(creativeRequest.getBody().inputStream());
        assertEquals("84", creativePayload.get("object_story_spec").get("page_id").asText());
        assertEquals(
            "https://cdn.example/img.jpg",
            creativePayload.get("object_story_spec").get("link_data").get("picture").asText()
        );
        assertFalse(creativePayload.get("object_story_spec").get("link_data").has("image_hash"));

        takeBackendRequest("backend request"); // experiments-ready
        takeBackendRequest("backend request"); // creatives fetch
        takeBackendRequest("backend request"); // campaign report
        RecordedRequest aliasPatch = takeBackendRequest("backend request");
        assertEquals("PATCH", aliasPatch.getMethod());
        assertEquals("/api/experiments/1/status?status=RUNNING", aliasPatch.getPath());
    }

    @Test
    void skipsExperimentWhenNoPageCanBeResolved() throws Exception {
        configurationClient.setConfiguration(configurationWithoutDefaultPageId("token"));

        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"pageId\":\"84\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"https://cdn.example/img.jpg\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        RecordedRequest experimentsRequest = takeBackendRequest("backend request");
        assertEquals("/api/facebook-campaigns/experiments-ready", experimentsRequest.getPath());
        RecordedRequest creativesRequest = takeBackendRequest("backend request");
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
