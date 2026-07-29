package com.marketinghub.facebookadsworker.facebookcampaign;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookAccessTokenManager;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.facebookapi.ExperimentFacebookApiLogClient;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient.FacebookWorkerConfiguration;
import com.marketinghub.facebookadsworker.facebooktokenrenewal.FacebookTokenRenewalClient;
import com.marketinghub.facebookadsworker.facebooktokenrenewal.FacebookTokenRenewalService;
import com.marketinghub.facebookadsworker.testsupport.FailFastMockWebServer;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingResolverProperties;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valida a orquestração de publicação de campanhas Facebook contra stubs de backend e Meta API.
 */
@Timeout(value = 15, unit = TimeUnit.SECONDS)
class FacebookCampaignServiceTest {
    private FailFastMockWebServer backend;
    private FailFastMockWebServer facebook;
    private FacebookCampaignService service;
    private ObjectMapper objectMapper;
    private FacebookAdsService adsService;
    private StubFacebookWorkerConfigurationClient configurationClient;
    private WebClient.Builder webClientBuilder;
    private ExperimentFacebookApiLogClient apiLogClient;
    private String imageUrl;
    private String videoUrl;
    private String manualTargetingPackageOverride;
    private String reachEstimateResponseBody;

    /**
     * Creates isolated backend and Facebook API stubs for each campaign publication scenario.
     */
    @BeforeEach
    void setUp() throws IOException {
        backend = new FailFastMockWebServer();
        backend.start();
        facebook = new FailFastMockWebServer();
        facebook.start();

        String facebookUrl = facebook.url("/").toString();
        imageUrl = facebook.url("/creative-image.jpg").toString();
        videoUrl = facebook.url("/creative-video.mp4").toString();

        objectMapper = new ObjectMapper();
        reachEstimateResponseBody = defaultReachEstimateResponse();
        configurationClient = new StubFacebookWorkerConfigurationClient();
        configurationClient.setConfiguration(configurationWithAccessToken("token"));
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(5));
        webClientBuilder = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient));
        TargetingResolverProperties resolverProperties = new TargetingResolverProperties();
        resolverProperties.setDefaultAdAccountId("act_1234567890");
        adsService = new FacebookAdsService(
            webClientBuilder,
            facebookUrl,
            "v23.0",
            objectMapper,
            resolverProperties
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
        apiLogClient = new ExperimentFacebookApiLogClient(
            webClientBuilder,
            objectMapper,
            backend.url("/").toString(),
            "/api"
        );
        service = new FacebookCampaignService(
            adsService,
            accessTokenManager,
            webClientBuilder,
            configurationClient,
            backend.url("/").toString(),
            "/api",
            objectMapper,
            apiLogClient,
            new MetaVideoNormalizer(false, "ffmpeg", Duration.ofSeconds(1))
        );
        backend.enqueuePriorityConditionalResponse(
            request -> "/api/experiments/1/facebook-api-logs".equals(request.getPath()) && "POST".equals(request.getMethod()),
            () -> new MockResponse().setBody("{}").addHeader("Content-Type", "application/json")
        );
        backend.enqueuePriorityConditionalResponse(
            request -> "/api/facebook-campaigns".equals(request.getPath())
                && "POST".equals(request.getMethod()),
            () -> new MockResponse().setBody("{}").addHeader("Content-Type", "application/json")
        );
        backend.enqueuePriorityConditionalResponse(
            request -> "/api/facebook-campaigns/publication-job-steps".equals(request.getPath())
                && "POST".equals(request.getMethod()),
            () -> new MockResponse().setBody("{}").addHeader("Content-Type", "application/json")
        );
        backend.enqueuePriorityConditionalResponse(
            request -> request.getPath() != null
                && request.getPath().contains("/api/instant-forms/")
                && request.getPath().endsWith("/publication")
                && "PATCH".equals(request.getMethod()),
            () -> new MockResponse().setBody("{}").addHeader("Content-Type", "application/json")
        );

        backend.enqueueConditionalResponse(
            request -> "/api/facebook-campaigns/experiments-ready".equals(request.getPath())
                && "GET".equals(request.getMethod()),
            () -> new MockResponse().setBody("[]").addHeader("Content-Type", "application/json")
        );
        backend.enqueuePriorityConditionalResponse(
            request -> request.getPath() != null
                && request.getPath().matches("/api/facebook-adsets/experiments/\\d+/targeting-package")
                && "GET".equals(request.getMethod()),
            () -> new MockResponse().setBody(defaultManualTargetingPackage()).addHeader("Content-Type", "application/json")
        );
        backend.enqueueConditionalResponse(
            request -> request.getPath() != null
                && request.getPath().matches("/api/experiments/\\d+/adset-playbook")
                && "GET".equals(request.getMethod()),
            () -> new MockResponse().setBody("{\"specs\":[]}").addHeader("Content-Type", "application/json")
        );
        backend.enqueuePriorityConditionalResponse(
            request -> request.getPath() != null
                && request.getPath().startsWith("/api/internal/facebook-campaigns/image-hash-mappings/resolve")
                && "GET".equals(request.getMethod()),
            () -> new MockResponse().setResponseCode(404)
        );
        backend.enqueuePriorityConditionalResponse(
            request -> "/api/internal/facebook-campaigns/image-hash-mappings".equals(request.getPath())
                && "POST".equals(request.getMethod()),
            () -> new MockResponse()
                .setBody("{\"metaImageHash\":\"hash-preloaded\"}")
                .addHeader("Content-Type", "application/json")
        );
        backend.enqueuePriorityConditionalResponse(
            request -> request.getPath() != null
                && request.getPath().contains("/api/experiments/")
                && request.getPath().contains("/status?status=")
                && "PATCH".equals(request.getMethod()),
            () -> new MockResponse().setBody("{}").addHeader("Content-Type", "application/json")
        );
        facebook.enqueuePriorityConditionalResponse(
            request -> "/creative-image.jpg".equals(request.getPath()) && "GET".equals(request.getMethod()),
            () -> new MockResponse()
                .setBody("fake-image-bytes")
                .addHeader("Content-Type", "image/jpeg")
        );
        facebook.enqueueConditionalResponse(
            request -> request.getPath() != null
                && request.getPath().matches("/v23\\.0/act_[^/]+/adimages")
                && "POST".equals(request.getMethod()),
            () -> new MockResponse()
                .setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
                .addHeader("Content-Type", "application/json")
        );
        facebook.enqueuePriorityConditionalResponse(
            request -> request.getPath() != null
                && request.getPath().startsWith("/v23.0/act_")
                && request.getPath().contains("/reachestimate?")
                && "GET".equals(request.getMethod()),
            () -> new MockResponse()
                .setBody(reachEstimateResponseBody)
                .addHeader("Content-Type", "application/json")
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        backend.assertNoUnmatchedRequests();
        facebook.assertNoUnmatchedRequests();
        backend.shutdown();
        facebook.shutdown();
    }


    /**
     * Provides a backend-approved manual targeting package used when no ready playbook exists.
     */
    private String defaultManualTargetingPackage() {
        if (manualTargetingPackageOverride != null) {
            return manualTargetingPackageOverride;
        }
        return """
            {
              "experimentId":1,
              "targeting":{
                "interests":[],
                "jobTitles":[
                  {"id":10,"term":"Personal Trainer","metaId":"1419795191647433","metaKey":"Certified Personal Trainer"}
                ],
                "behaviors":[]
              }
            }
            """;
    }

    /**
     * Monta um pacote manual aprovado contendo apenas interesse para testes de publicação.
     */
    private String interestOnlyManualTargetingPackage() {
        return """
            {
              "experimentId":1,
              "targeting":{
                "interests":[
                  {"id":20,"term":"Manicure","metaId":"6003139266461","metaKey":"Manicure (cosmetics)"}
                ],
                "jobTitles":[],
                "behaviors":[]
              }
            }
            """;
    }

    /**
     * Builds the default approved manual targeting response for campaign publication tests.
     */
    private MockResponse defaultManualTargetingPackageResponse() {
        return new MockResponse()
            .setBody(defaultManualTargetingPackage())
            .addHeader("Content-Type", "application/json");
    }

    /**
     * Monta uma resposta de alcance dentro da faixa operacional aceita para publicação.
     */
    private String defaultReachEstimateResponse() {
        return "{\"data\":[{\"users_lower_bound\":250000,\"users_upper_bound\":5000000}]}";
    }

    private RecordedRequest takeBackendRequest(String description) throws InterruptedException {
        RecordedRequest request = backend.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request, "Expected backend request (" + description + ") within timeout.");
        return request;
    }

    /**
     * Takes the next Meta API request while ignoring local image download calls unless explicitly requested.
     */
    private RecordedRequest takeFacebookRequest(String description) throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            RecordedRequest request = facebook.takeRequest(5, TimeUnit.SECONDS);
            assertNotNull(request, "Expected Facebook request (" + description + ") within timeout.");
            String path = request.getPath();
            boolean isAdImageRequest = path != null && path.contains("/adimages");
            boolean isImageAssetRequest = "/creative-image.jpg".equals(path) && "GET".equals(request.getMethod());
            boolean isVideoAssetRequest = "/creative-video.mp4".equals(path) && "GET".equals(request.getMethod());
            boolean isReachEstimateRequest = path != null && path.contains("/reachestimate?");
            String normalizedDescription = description.toLowerCase();
            boolean expectsAdImage = normalizedDescription.contains("ad image");
            boolean expectsImageAsset = normalizedDescription.contains("image asset");
            boolean expectsVideoAsset = normalizedDescription.contains("video asset");
            boolean expectsReachEstimate = normalizedDescription.contains("reach");
            if ((isAdImageRequest && !expectsAdImage)
                || (isImageAssetRequest && !expectsImageAsset)
                || (isVideoAssetRequest && !expectsVideoAsset)
                || (isReachEstimateRequest && !expectsReachEstimate)) {
                continue;
            }
            return request;
        }
        throw new AssertionError("Expected Facebook request (" + description + ") within timeout.");
    }

    private RecordedRequest takeBackendRequestMatching(String description, Predicate<RecordedRequest> predicate)
        throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            RecordedRequest request = takeBackendRequest(description);
            if (predicate.test(request)) {
                return request;
            }
        }
        throw new AssertionError("Expected backend request (" + description + ") matching predicate within 20 attempts.");
    }

    @Test
    // Verifies that a released experiment publishes the full Facebook campaign hierarchy.
    void createsCampaignHierarchyForEachExperiment() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"dailyBudget\":25.0,\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(defaultManualTargetingPackageResponse());
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
        assertEquals("/api/facebook-campaigns/experiments/1/creatives-ready", creativesGet.getPath());

        RecordedRequest playbookGet = takeBackendRequestMatching(
            "playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        assertEquals("/api/experiments/1/adset-playbook", playbookGet.getPath());

        RecordedRequest targetingGet = takeBackendRequestMatching(
            "filtered manual targeting request",
            request -> "/api/facebook-adsets/experiments/1/targeting-package".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        assertEquals("/api/facebook-adsets/experiments/1/targeting-package", targetingGet.getPath());

        RecordedRequest postAdImage = takeFacebookRequest("facebook ad image upload");
        assertEquals("/v23.0/act_1/adimages", postAdImage.getPath());
        String adImagePayload = postAdImage.getBody().readUtf8();
        assertTrue(adImagePayload.contains("name=\"source\""));
        assertFalse(adImagePayload.contains("name=\"url\""));

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
        assertFalse(adSetPayload.has("bid_amount"));
        assertEquals("WEBSITE", adSetPayload.get("destination_type").asText());
        assertEquals("42", adSetPayload.get("promoted_object").get("page_id").asText());
        JsonNode targeting = adSetPayload.get("targeting");
        assertEquals("BR", targeting.get("geo_locations").get("countries").get(0).asText());
        assertFalse(targeting.has("work_positions"));
        assertEquals(1, targeting.get("flexible_spec").size());
        assertEquals("1419795191647433", targeting.get("flexible_spec").get(0).get("work_positions").get(0).get("id").asText());
        assertEquals(0, targeting.get("targeting_automation").get("advantage_audience").asInt());

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
        assertEquals("hash-preloaded", linkData.get("image_hash").asText());
        assertFalse(linkData.has("picture"));

        RecordedRequest postAd = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/ads", postAd.getPath());
        JsonNode adPayload = objectMapper.readTree(postAd.getBody().inputStream());
        assertEquals("Exp - Ad", adPayload.get("name").asText());
        assertEquals("20", adPayload.get("adset_id").asText());
        assertEquals("30", adPayload.get("creative").get("creative_id").asText());

        RecordedRequest postBackend = takeBackendRequestMatching(
            "campaign report",
            request -> "/api/facebook-campaigns".equals(request.getPath()) && "POST".equals(request.getMethod())
        );
        JsonNode backendPayload = objectMapper.readTree(postBackend.getBody().inputStream());
        assertEquals("ACTIVE", backendPayload.get("status").asText());
        assertEquals("ADSET", backendPayload.get("budgetMode").asText());
        assertEquals("ACTIVE", backendPayload.get("adSet").get("status").asText());
        assertEquals("ACTIVE", backendPayload.get("ad").get("status").asText());
        assertEquals("2500", backendPayload.get("adSet").get("dailyBudget").asText());
        assertTrue(backend.getRequestCount() >= 4);
    }

    @Test
    // Garante que label comercial de botao nao seja enviado como enum tecnico da Meta.
    void normalizesCommercialCallToActionLabelBeforeCreatingAdCreative() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"dailyBudget\":25.0,\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"Abrir a planilha de evidências\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(defaultManualTargetingPackageResponse());
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        takeBackendRequest("experiments ready");
        takeBackendRequest("creatives ready");
        takeBackendRequestMatching(
            "playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        takeBackendRequestMatching(
            "manual targeting package request",
            request -> "/api/facebook-adsets/experiments/1/targeting-package".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        takeFacebookRequest("facebook campaign");
        takeFacebookRequest("facebook ad set");
        RecordedRequest creativeRequest = takeFacebookRequest("facebook creative");
        JsonNode creativePayload = objectMapper.readTree(creativeRequest.getBody().inputStream());
        JsonNode callToAction = creativePayload.get("object_story_spec")
            .get("link_data")
            .get("call_to_action");
        assertEquals("LEARN_MORE", callToAction.get("type").asText());
    }

    @Test
    // Garante fallback minimo quando a Meta rejeita o payload completo de adcreative.
    void retriesWithSimpleLinkCreativeWhenFacebookRejectsPrimaryAdCreativePayload() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"dailyBudget\":25.0,\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"},\"publicationJobId\":\"job-adcreative-fallback\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(defaultManualTargetingPackageResponse());
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":{\"message\":\"Service temporarily unavailable\",\"type\":\"OAuthException\",\"code\":2,\"error_user_msg\":\"Não foi possível criar o criativo do anúncio\"}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        takeBackendRequest("experiments ready");
        takeBackendRequest("creatives ready");
        takeBackendRequestMatching(
            "playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        takeBackendRequestMatching(
            "manual targeting package request",
            request -> "/api/facebook-adsets/experiments/1/targeting-package".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        takeFacebookRequest("facebook ad image upload");
        takeFacebookRequest("facebook campaign");
        takeFacebookRequest("facebook ad set");

        RecordedRequest primaryCreative = takeFacebookRequest("facebook creative primary");
        JsonNode primaryPayload = objectMapper.readTree(primaryCreative.getBody().inputStream());
        JsonNode primaryLinkData = primaryPayload.get("object_story_spec").get("link_data");
        assertEquals("HL", primaryLinkData.get("name").asText());
        assertEquals("Desc", primaryLinkData.get("description").asText());

        RecordedRequest fallbackCreative = takeFacebookRequest("facebook creative fallback");
        JsonNode fallbackPayload = objectMapper.readTree(fallbackCreative.getBody().inputStream());
        assertEquals("Exp - Creative - Link Fallback", fallbackPayload.get("name").asText());
        JsonNode storySpec = fallbackPayload.get("object_story_spec");
        assertEquals("42", storySpec.get("page_id").asText());
        assertEquals("21", storySpec.get("instagram_user_id").asText());
        JsonNode fallbackLinkData = storySpec.get("link_data");
        assertEquals("Texto Criativo", fallbackLinkData.get("message").asText());
        assertEquals("https://exp.example/landing", fallbackLinkData.get("link").asText());
        assertEquals("hash-preloaded", fallbackLinkData.get("image_hash").asText());
        assertEquals("SHOP_NOW", fallbackLinkData.get("call_to_action").get("type").asText());
        assertFalse(fallbackLinkData.has("name"));
        assertFalse(fallbackLinkData.has("description"));

        RecordedRequest adRequest = takeFacebookRequest("facebook ad");
        JsonNode adPayload = objectMapper.readTree(adRequest.getBody().inputStream());
        assertEquals("30", adPayload.get("creative").get("creative_id").asText());

        RecordedRequest diagnosticStep = takeBackendRequestMatching(
            "simple link fallback diagnostic",
            request -> "/api/facebook-campaigns/publication-job-steps".equals(request.getPath())
                && "POST".equals(request.getMethod())
                && request.getBody().clone().readUtf8().contains("CAMPAIGN_AD_CREATIVE_SIMPLE_LINK_FALLBACK")
        );
        JsonNode diagnosticPayload = objectMapper.readTree(diagnosticStep.getBody().inputStream());
        assertEquals("job-adcreative-fallback", diagnosticPayload.get("jobId").asText());
        assertTrue(diagnosticPayload.get("errorMessage").asText().contains("fallback minimo"));

        RecordedRequest backendReport = takeBackendRequestMatching(
            "campaign report",
            request -> "/api/facebook-campaigns".equals(request.getPath()) && "POST".equals(request.getMethod())
        );
        JsonNode reportPayload = objectMapper.readTree(backendReport.getBody().inputStream());
        assertEquals("30", reportPayload.get("adCreative").get("id").asText());
        assertFalse(reportPayload.get("adCreative").hasNonNull("headline"));
        assertFalse(reportPayload.get("adCreative").hasNonNull("description"));
    }

    @Test
    // Garante que indisponibilidade temporária da Meta não invalida o experimento.
    void keepsExperimentEligibleWhenFacebookAdCreativeCreationIsTemporarilyUnavailable() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"dailyBudget\":25.0,\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"},\"publicationJobId\":\"job-meta-temporary\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(defaultManualTargetingPackageResponse());
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":{\"message\":\"Service temporarily unavailable\",\"type\":\"OAuthException\",\"code\":2,\"error_user_msg\":\"Não foi possível criar o criativo do anúncio\"}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":{\"message\":\"Service temporarily unavailable\",\"type\":\"OAuthException\",\"code\":2,\"error_user_msg\":\"Não foi possível criar o criativo do anúncio\"}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"success\":true}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"success\":true}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        takeBackendRequest("experiments ready");
        takeBackendRequest("creatives ready");
        takeBackendRequestMatching(
            "playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        takeBackendRequestMatching(
            "manual targeting package request",
            request -> "/api/facebook-adsets/experiments/1/targeting-package".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        takeFacebookRequest("facebook ad image upload");
        takeFacebookRequest("facebook campaign");
        takeFacebookRequest("facebook ad set");
        takeFacebookRequest("facebook creative primary");
        takeFacebookRequest("facebook creative fallback");
        RecordedRequest cleanupAdSet = takeFacebookRequest("facebook cleanup ad set");
        assertEquals("DELETE", cleanupAdSet.getMethod());
        RecordedRequest cleanupCampaign = takeFacebookRequest("facebook cleanup campaign");
        assertEquals("DELETE", cleanupCampaign.getMethod());
    }

    @Test
    // Garante que a ausência de limites da Meta gera aviso, mas não bloqueia teste controlado.
    void continuesCampaignCreationWhenReachBoundsAreUnavailable() throws Exception {
        reachEstimateResponseBody = "{\"data\":[{\"users\":1234}]}";
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"dailyBudget\":25.0,\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"},\"publicationJobId\":\"job-reach-warning\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(defaultManualTargetingPackageResponse());
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        takeBackendRequest("experiments ready");
        takeBackendRequest("creatives ready");
        takeBackendRequestMatching(
            "playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        takeBackendRequestMatching(
            "manual targeting package request",
            request -> "/api/facebook-adsets/experiments/1/targeting-package".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        RecordedRequest reachRequest = takeFacebookRequest("reach validation");
        assertTrue(reachRequest.getPath().contains("/reachestimate?"));
        takeBackendRequestMatching(
            "publication job reach api step",
            request -> "/api/facebook-campaigns/publication-job-steps".equals(request.getPath())
                && "POST".equals(request.getMethod())
        );
        RecordedRequest warningStepRequest = takeBackendRequestMatching(
            "publication job warning step",
            request -> "/api/facebook-campaigns/publication-job-steps".equals(request.getPath())
                && "POST".equals(request.getMethod())
        );
        JsonNode warningStepPayload = objectMapper.readTree(warningStepRequest.getBody().inputStream());
        assertEquals("CAMPAIGN_REACH_VALIDATION_WARNING", warningStepPayload.get("stepName").asText());
        assertTrue(warningStepPayload.get("errorMessage").asText().contains("teste controlado"));
        RecordedRequest campaignRequest = takeFacebookRequest("campaign creation");
        assertEquals("/v23.0/act_1/campaigns", campaignRequest.getPath());
        RecordedRequest campaignReport = takeBackendRequestMatching(
            "campaign report",
            request -> "/api/facebook-campaigns".equals(request.getPath()) && "POST".equals(request.getMethod())
        );
        JsonNode backendPayload = objectMapper.readTree(campaignReport.getBody().inputStream());
        assertEquals("ACTIVE", backendPayload.get("status").asText());
    }

    @Test
    // Garante que a campanha não é criada quando o público fica abaixo do alcance mínimo.
    void skipsCampaignCreationWhenReachValidationIsBelowMinimum() throws Exception {
        reachEstimateResponseBody = "{\"data\":[{\"users_lower_bound\":1000,\"users_upper_bound\":50000}]}";
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"dailyBudget\":25.0,\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"},\"publicationJobId\":\"job-reach-low\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(defaultManualTargetingPackageResponse());

        service.createCampaignsFromExperiments();

        takeBackendRequest("experiments ready");
        takeBackendRequest("creatives ready");
        takeBackendRequestMatching(
            "playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        takeBackendRequestMatching(
            "manual targeting package request",
            request -> "/api/facebook-adsets/experiments/1/targeting-package".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        RecordedRequest reachRequest = takeFacebookRequest("reach validation");
        assertTrue(reachRequest.getPath().contains("/reachestimate?"));
        JsonNode reachTargetingSpec = targetingSpecFromReachEstimateRequest(reachRequest);
        assertEquals("BR", reachTargetingSpec.get("geo_locations").get("countries").get(0).asText());
        assertFalse(reachTargetingSpec.has("work_positions"));
        assertEquals(1, reachTargetingSpec.get("flexible_spec").size());
        takeBackendRequestMatching(
            "publication job reach step",
            request -> "/api/facebook-campaigns/publication-job-steps".equals(request.getPath())
                && "POST".equals(request.getMethod())
        );
        RecordedRequest failureStepRequest = takeBackendRequestMatching(
            "publication job failure step",
            request -> "/api/facebook-campaigns/publication-job-steps".equals(request.getPath())
                && "POST".equals(request.getMethod())
        );
        JsonNode failureStepPayload = objectMapper.readTree(failureStepRequest.getBody().inputStream());
        assertEquals("CAMPAIGN_REACH_VALIDATION_BLOCKED", failureStepPayload.get("stepName").asText());
        assertTrue(failureStepPayload.get("errorMessage").asText().contains("Público pequeno demais"));
        RecordedRequest failedStatusRequest = takeBackendRequestMatching(
            "failed status update",
            request -> request.getPath() != null
                && request.getPath().contains("/api/experiments/1/status?status=FAILED")
                && "PATCH".equals(request.getMethod())
        );
        assertNotNull(failedStatusRequest);
        assertEquals(1, facebook.getRequestCount());
    }

    @Test
    // Garante que público acima do alerta operacional gera aviso, mas não bloqueia teste controlado.
    void continuesCampaignCreationWhenReachValidationIsAboveWarningThresholdWithObjectData() throws Exception {
        reachEstimateResponseBody = "{\"data\":{\"users_lower_bound\":84000000,\"users_upper_bound\":98900000,\"estimate_ready\":true}}";
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"dailyBudget\":25.0,\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"},\"publicationJobId\":\"job-reach-high\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(defaultManualTargetingPackageResponse());
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        takeBackendRequest("experiments ready");
        takeBackendRequest("creatives ready");
        takeBackendRequestMatching(
            "playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        takeBackendRequestMatching(
            "manual targeting package request",
            request -> "/api/facebook-adsets/experiments/1/targeting-package".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        RecordedRequest reachRequest = takeFacebookRequest("reach validation");
        assertTrue(reachRequest.getPath().contains("/reachestimate?"));
        takeBackendRequestMatching(
            "publication job reach step",
            request -> "/api/facebook-campaigns/publication-job-steps".equals(request.getPath())
                && "POST".equals(request.getMethod())
        );
        RecordedRequest warningStepRequest = takeBackendRequestMatching(
            "publication job warning step",
            request -> "/api/facebook-campaigns/publication-job-steps".equals(request.getPath())
                && "POST".equals(request.getMethod())
        );
        JsonNode warningStepPayload = objectMapper.readTree(warningStepRequest.getBody().inputStream());
        assertEquals("CAMPAIGN_REACH_VALIDATION_WARNING", warningStepPayload.get("stepName").asText());
        assertTrue(warningStepPayload.get("errorMessage").asText().contains("Público amplo para teste controlado"));
        RecordedRequest campaignRequest = takeFacebookRequest("campaign creation");
        assertEquals("/v23.0/act_1/campaigns", campaignRequest.getPath());
        RecordedRequest campaignReport = takeBackendRequestMatching(
            "campaign report",
            request -> "/api/facebook-campaigns".equals(request.getPath()) && "POST".equals(request.getMethod())
        );
        JsonNode backendPayload = objectMapper.readTree(campaignReport.getBody().inputStream());
        assertEquals("ACTIVE", backendPayload.get("status").asText());
    }

    @Test
    // Garante que nenhum objeto de anúncio seja criado quando o criativo pronto não tem imagem.
    void blocksCampaignPublicationWhenReadyCreativeHasNoImage() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"dailyBudget\":25.0,\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"},\"publicationJobId\":\"job-no-image\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        RecordedRequest experimentsReady = takeBackendRequest("experiments ready");
        assertEquals("/api/facebook-campaigns/experiments-ready", experimentsReady.getPath());
        RecordedRequest creativesReady = takeBackendRequest("creatives ready");
        assertEquals("/api/facebook-campaigns/experiments/1/creatives-ready", creativesReady.getPath());
        RecordedRequest failureStep = takeBackendRequestMatching(
            "publication job image required step",
            request -> "/api/facebook-campaigns/publication-job-steps".equals(request.getPath())
                && "POST".equals(request.getMethod())
        );
        JsonNode failurePayload = objectMapper.readTree(failureStep.getBody().inputStream());
        assertEquals("job-no-image", failurePayload.get("jobId").asText());
        assertEquals("CAMPAIGN_CREATIVE_IMAGE_REQUIRED", failurePayload.get("stepName").asText());
        assertTrue(failurePayload.get("errorMessage").asText().contains("todos os anúncios precisam de imagem"));

        RecordedRequest failedStatusRequest = takeBackendRequestMatching(
            "failed status update",
            request -> request.getPath() != null
                && request.getPath().contains("/api/experiments/1/status?status=FAILED")
                && "PATCH".equals(request.getMethod())
        );
        assertNotNull(failedStatusRequest);
        assertEquals(0, facebook.getRequestCount());
    }

    @Test
    void retriesAdCreativeCreationWhenFacebookCannotDownloadImageTemporarily() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"dailyBudget\":25.0,\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":{\"message\":\"Invalid parameter\",\"code\":100,\"error_subcode\":3858258,\"error_user_msg\":\"Não foi possível baixar sua imagem\"}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setBody("{\"images\":{\"" + imageUrl + "\":{\"hash\":\"hash123\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        RecordedRequest postCampaign = takeFacebookRequest("facebook campaign creation");
        assertEquals("/v23.0/act_1/campaigns", postCampaign.getPath());

        RecordedRequest postAdSet = takeFacebookRequest("facebook adset creation");
        assertEquals("/v23.0/act_1/adsets", postAdSet.getPath());

        RecordedRequest creativeAttempt1 = takeFacebookRequest("facebook creative attempt 1");
        assertEquals("/v23.0/act_1/adcreatives", creativeAttempt1.getPath());
        RecordedRequest creativeAttempt2 = takeFacebookRequest("facebook creative attempt 2");
        assertEquals("/v23.0/act_1/adcreatives", creativeAttempt2.getPath());

        RecordedRequest postAd = takeFacebookRequest("facebook ad creation");
        assertEquals("/v23.0/act_1/ads", postAd.getPath());
    }

    
    @Test
    // Verifies cleanup of a Facebook campaign when ad set creation fails.
    void deletesFacebookCampaignWhenAdSetCreationFails() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"dailyBudget\":25.0,\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"99\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":{\"message\":\"Invalid\",\"code\":100}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"success\":true}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{\"specs\":[]}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(defaultManualTargetingPackageResponse());
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

        RecordedRequest experimentsReady = takeBackendRequest("backend request");
        assertEquals("/api/facebook-campaigns/experiments-ready", experimentsReady.getPath());
        takeBackendRequest("backend request"); // creatives
        takeBackendRequest("backend request"); // adset playbook

        RecordedRequest postCampaign = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/campaigns", postCampaign.getPath());

        RecordedRequest failedAdSet = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/adsets", failedAdSet.getPath());

        RecordedRequest cleanupDelete = takeFacebookRequest("facebook cleanup");
        assertEquals("DELETE", cleanupDelete.getMethod());
        assertTrue(cleanupDelete.getPath().startsWith("/v23.0/99"));
    }


    @Test
    // Garante que contrato de recompensa gratuita publica campanha como Leads, sem otimizar para cliques.
    void createsLeadCampaignForSinglePromiseRewardEvenWithWebsiteDestination() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"dailyBudget\":25.0,\"freeReward\":\"3 mensagens prontas\",\"campaignObjective\":\"LEADS\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"3 mensagens prontas\",\"primaryText\":\"Receba 3 mensagens prontas para conversar com leads\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SIGN_UP\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(defaultManualTargetingPackageResponse());
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        takeBackendRequest("backend request"); // experiments-ready
        takeBackendRequest("backend request"); // creatives
        takeBackendRequestMatching(
            "playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        takeBackendRequestMatching(
            "manual targeting request",
            request -> "/api/facebook-adsets/experiments/1/targeting-package".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        takeFacebookRequest("facebook ad image upload");

        RecordedRequest postCampaign = takeFacebookRequest("facebook campaign");
        JsonNode campaignPayload = objectMapper.readTree(postCampaign.getBody().inputStream());
        assertEquals("OUTCOME_LEADS", campaignPayload.get("objective").asText());

        RecordedRequest postAdSet = takeFacebookRequest("facebook ad set");
        JsonNode adSetPayload = objectMapper.readTree(postAdSet.getBody().inputStream());
        assertEquals("WEBSITE", adSetPayload.get("destination_type").asText());
        assertEquals("LEAD_GENERATION", adSetPayload.get("optimization_goal").asText());
    }

    @Test
    // Garante que low-ticket com recompensa secundária continua como venda otimizada para Purchase.
    void createsSalesCampaignForLowTicketProductEvenWithSecondaryReward() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"dailyBudget\":25.0,\"experimentType\":\"LOW_TICKET_PRODUCT\",\"freeReward\":\"ver amostra gratuita\",\"campaignObjective\":\"SALES\",\"facebookPixelId\":\"pixel-123\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"Ver amostra gratuita\",\"primaryText\":\"Veja a amostra gratuita e compre o produto completo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"BUY_NOW\",\"destinationUrl\":\"https://exp.example/checkout\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(defaultManualTargetingPackageResponse());
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        takeBackendRequest("backend request"); // experiments-ready
        takeBackendRequest("backend request"); // creatives
        takeBackendRequestMatching(
            "playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        takeBackendRequestMatching(
            "manual targeting request",
            request -> "/api/facebook-adsets/experiments/1/targeting-package".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        takeFacebookRequest("facebook ad image upload");

        RecordedRequest postCampaign = takeFacebookRequest("facebook campaign");
        JsonNode campaignPayload = objectMapper.readTree(postCampaign.getBody().inputStream());
        assertEquals("OUTCOME_SALES", campaignPayload.get("objective").asText());

        RecordedRequest postAdSet = takeFacebookRequest("facebook ad set");
        JsonNode adSetPayload = objectMapper.readTree(postAdSet.getBody().inputStream());
        assertEquals("WEBSITE", adSetPayload.get("destination_type").asText());
        assertEquals("OFFSITE_CONVERSIONS", adSetPayload.get("optimization_goal").asText());
        assertEquals("pixel-123", adSetPayload.get("promoted_object").get("pixel_id").asText());
        assertEquals("PURCHASE", adSetPayload.get("promoted_object").get("custom_event_type").asText());
    }

    @Test
    void usesExperimentStandaloneUrlAsDestinationBeforeCreativeFallback() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("""
                [{"id":1,"name":"Exp","dailyBudget":25.0,"followUpActionUrl":"https://oportunidadebrasil.shop/api/flows/exp-1-landing-geralanding/page","facebookPage":{"id":9,"pageId":"84","name":"Estúdio"},"instagramAccount":{"id":55,"handle":"@estudio","code":"IG-EST","name":"Estúdio"}}]
                """)
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("""
                {"images":{"uploaded":{"hash":"hash-preloaded"}}}
                """)
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("""
                [{"id":101,"experimentId":1,"headline":"HL","primaryText":"Texto Criativo","imageUrl":"%s","description":"Desc","cta":"SHOP_NOW","destinationUrl":"","instagramUserId":"21","status":"READY"}]
                """.formatted(imageUrl))
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(defaultManualTargetingPackageResponse());
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        takeBackendRequest("experiments ready");
        takeBackendRequest("creatives ready");
        takeBackendRequestMatching("playbook request", request -> "/api/experiments/1/adset-playbook".equals(request.getPath()));
        takeBackendRequestMatching("targeting package", request -> "/api/facebook-adsets/experiments/1/targeting-package".equals(request.getPath()));
        takeFacebookRequest("facebook ad image upload");
        takeFacebookRequest("facebook campaign");
        takeFacebookRequest("facebook ad set");

        RecordedRequest postCreative = takeFacebookRequest("facebook creative");
        JsonNode creativePayload = objectMapper.readTree(postCreative.getBody().inputStream());
        String destination = creativePayload.get("object_story_spec").get("link_data").get("link").asText();
        assertTrue(destination.startsWith("https://oportunidadebrasil.shop/api/flows/exp-1-landing-geralanding/page"));
        assertTrue(destination.contains("campaign=exp-1"));
    }

    @Test
    // Verifies instant form share links are used when the journey requires a form destination.
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
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(defaultManualTargetingPackageResponse());
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
        assertEquals("/api/facebook-campaigns/experiments/1/creatives-ready", creativesRequest.getPath());

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

        RecordedRequest apiLogPost = takeBackendRequestMatching(
            "facebook api log",
            request -> "/api/experiments/1/facebook-api-logs".equals(request.getPath())
                && "POST".equals(request.getMethod())
        );
        assertEquals("/api/experiments/1/facebook-api-logs", apiLogPost.getPath());

        RecordedRequest backendReport = takeBackendRequestMatching(
            "campaign report",
            request -> "/api/facebook-campaigns".equals(request.getPath()) && "POST".equals(request.getMethod())
        );

        RecordedRequest publicationPatch = takeBackendRequest("backend request");
        assertEquals("PATCH", publicationPatch.getMethod());
        assertEquals("/api/instant-forms/33/publication", publicationPatch.getPath());
        JsonNode patchPayload = objectMapper.readTree(publicationPatch.getBody().inputStream());
        assertTrue(patchPayload.get("published").asBoolean());
        assertEquals("https://www.facebook.com/ads/leadgen/?id=987654321", patchPayload.get("shareLink").asText());
        assertEquals("987654321", patchPayload.get("facebookFormId").asText());
        assertTrue(backend.getRequestCount() >= 4);
    }

    @Test
    // Verifies AI-provided instant form identifiers are normalized before Graph API calls.
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
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(defaultManualTargetingPackageResponse());
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
        takeBackendRequestMatching(
            "campaign report",
            request -> "/api/facebook-campaigns".equals(request.getPath()) && "POST".equals(request.getMethod())
        );
        RecordedRequest publicationPatch = takeBackendRequestMatching(
            "instant form publication",
            request -> request.getPath().contains("/instant-forms/") && "PATCH".equals(request.getMethod())
        );
        JsonNode patchPayload = objectMapper.readTree(publicationPatch.getBody().inputStream());
        assertEquals("https://www.facebook.com/ads/leadgen/?id=form_3_1_token", patchPayload.get("shareLink").asText());
        assertEquals("form_3_1_token", patchPayload.get("facebookFormId").asText());
        assertTrue(backend.getRequestCount() >= 4);
    }


    @Test
    // Verifies draft instant forms can be published from share links when form IDs are missing.
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
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
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
        takeBackendRequestMatching(
            "campaign report",
            request -> "/api/facebook-campaigns".equals(request.getPath()) && "POST".equals(request.getMethod())
        );
        RecordedRequest publicationPatch = takeBackendRequestMatching(
            "instant form publication",
            request -> request.getPath().contains("/instant-forms/") && "PATCH".equals(request.getMethod())
        );
        JsonNode patchPayload = objectMapper.readTree(publicationPatch.getBody().inputStream());
        assertTrue(patchPayload.get("published").asBoolean());
        assertEquals("https://www.facebook.com/ads/leadgen/?id=2468", patchPayload.get("shareLink").asText());
        assertEquals("2468", patchPayload.get("facebookFormId").asText());
        assertTrue(backend.getRequestCount() >= 4);
    }

    @Test
    // Verifies lead generation campaigns use the form ID supplied by the approved creative.
    void createsLeadGenCampaignWhenCreativeProvidesFormId() throws Exception {
        configurationClient.setConfiguration(new FacebookWorkerConfiguration(
            1L,
            "1",
            "token",
            "system-token",
            "123456789012345",
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
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SIGN_UP\",\"leadGenFormId\":\"321123321123321\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
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
        assertEquals("hash-preloaded", linkData.get("image_hash").asText());
        assertFalse(linkData.has("picture"));

        takeBackendRequest("backend request"); // experiments-ready
        takeBackendRequest("backend request"); // creatives fetch
        RecordedRequest playbookRequest = takeBackendRequestMatching(
            "playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        assertEquals("/api/experiments/1/adset-playbook", playbookRequest.getPath());
        RecordedRequest backendReport = takeBackendRequestMatching(
            "campaign report",
            request -> "/api/facebook-campaigns".equals(request.getPath()) && "POST".equals(request.getMethod())
        );
        JsonNode reportedCreative = objectMapper.readTree(backendReport.getBody().inputStream())
            .get("adCreative");
        assertEquals("321123321123321", reportedCreative.get("leadGenFormId").asText());
        assertTrue(backend.getRequestCount() >= 4);
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
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":{\"message\":\"Permissions error\",\"type\":\"OAuthException\",\"code\":200,\"error_subcode\":1815066,\"error_user_msg\":\"O usuário não tem permissão para criar anúncios com esta conta de anúncios\"}}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        RecordedRequest get = takeBackendRequest("backend request");
        assertEquals("GET", get.getMethod());
        assertEquals("/api/facebook-campaigns/experiments-ready", get.getPath());

        RecordedRequest creativesGet = takeBackendRequest("backend request");
        assertEquals("GET", creativesGet.getMethod());
        assertEquals("/api/facebook-campaigns/experiments/1/creatives-ready", creativesGet.getPath());

        RecordedRequest postCampaign = takeFacebookRequest("facebook request");
        assertEquals("POST", postCampaign.getMethod());
        assertEquals("/v23.0/act_1/campaigns", postCampaign.getPath());
        assertEquals(4, facebook.getRequestCount());

        RecordedRequest patch = takeBackendRequestMatching(
            "experiment failed status update",
            request -> request.getPath().contains("/status?status=FAILED") && "PATCH".equals(request.getMethod())
        );
        assertEquals("/api/experiments/1/status?status=FAILED", patch.getPath());
        assertTrue(backend.getRequestCount() >= 3);
    }

    /**
     * Verifies that a permission-blocked experiment is not retried in the same worker lifecycle.
     */
    @Test
    // Verifies experiments blocked by permission errors are skipped on later worker cycles.
    void skipsExperimentAfterPermissionErrorEvenIfBackendKeepsReturningIt() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":{\"message\":\"Permissions error\",\"type\":\"OAuthException\",\"code\":200,\"error_subcode\":1815066}}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
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
        assertEquals("/api/facebook-campaigns/experiments/1/creatives-ready", firstCreativesGet.getPath());

        RecordedRequest campaignPost = takeFacebookRequest("facebook request");
        assertEquals("POST", campaignPost.getMethod());
        assertEquals("/v23.0/act_1/campaigns", campaignPost.getPath());

        RecordedRequest failedPatch = takeBackendRequestMatching(
            "experiment failed status update",
            request -> request.getPath().contains("/status?status=FAILED") && "PATCH".equals(request.getMethod())
        );
        assertEquals("/api/experiments/1/status?status=FAILED", failedPatch.getPath());

        RecordedRequest secondGet = takeBackendRequest("backend request");
        assertEquals("GET", secondGet.getMethod());
        assertEquals("/api/facebook-campaigns/experiments-ready", secondGet.getPath());

        assertEquals(4, facebook.getRequestCount());
        assertTrue(backend.getRequestCount() >= 8);
    }

    @Test
    // Verifies processing resumes after automatic token renewal clears an expired token.
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
            objectMapper,
            apiLogClient,
            new MetaVideoNormalizer(false, "ffmpeg", Duration.ofSeconds(1))
        );

        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":{\"message\":\"Error validating access token: Session has expired\",\"type\":\"OAuthException\",\"code\":190,\"error_subcode\":463}}")
            .addHeader("Content-Type", "application/json"));

        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
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
        assertEquals("/api/facebook-campaigns/experiments/1/creatives-ready", firstCreativeRequest.getPath());
        RecordedRequest firstPlaybookRequest = takeBackendRequestMatching(
            "first playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        assertEquals("/api/experiments/1/adset-playbook", firstPlaybookRequest.getPath());
        RecordedRequest expiredCampaignRequest = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/campaigns", expiredCampaignRequest.getPath());

        RecordedRequest firstManualTargetingRequest = takeBackendRequestMatching(
            "first manual targeting package request",
            request -> "/api/facebook-adsets/experiments/1/targeting-package".equals(request.getPath()) && "GET".equals(request.getMethod())
        );
        assertEquals("/api/facebook-adsets/experiments/1/targeting-package", firstManualTargetingRequest.getPath());

        RecordedRequest secondExperimentRequest = takeBackendRequestMatching(
            "second experiments-ready request",
            request -> "/api/facebook-campaigns/experiments-ready".equals(request.getPath()) && "GET".equals(request.getMethod())
        );
        assertEquals("/api/facebook-campaigns/experiments-ready", secondExperimentRequest.getPath());
        assertTrue(facebook.getRequestCount() >= 1);
        assertTrue(backend.getRequestCount() >= 6);
    }

    @Test
    // Verifies processing resumes when the backend supplies a renewed token without app credentials.
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
            objectMapper,
            apiLogClient,
            new MetaVideoNormalizer(false, "ffmpeg", Duration.ofSeconds(1))
        );

        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
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
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
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
        assertEquals("/api/facebook-campaigns/experiments/1/creatives-ready", firstCreative.getPath());
        RecordedRequest firstPlaybookRequest = takeBackendRequestMatching(
            "first playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        assertEquals("/api/experiments/1/adset-playbook", firstPlaybookRequest.getPath());
        RecordedRequest expiredCampaign = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/campaigns", expiredCampaign.getPath());

        RecordedRequest firstManualTargetingRequest = takeBackendRequestMatching(
            "first manual targeting package request",
            request -> "/api/facebook-adsets/experiments/1/targeting-package".equals(request.getPath()) && "GET".equals(request.getMethod())
        );
        assertEquals("/api/facebook-adsets/experiments/1/targeting-package", firstManualTargetingRequest.getPath());

        RecordedRequest secondExperiment = takeBackendRequestMatching(
            "second experiments-ready request",
            request -> "/api/facebook-campaigns/experiments-ready".equals(request.getPath()) && "GET".equals(request.getMethod())
        );
        assertEquals("/api/facebook-campaigns/experiments-ready", secondExperiment.getPath());
        RecordedRequest secondCreative = takeBackendRequest("backend request");
        assertEquals("/api/facebook-campaigns/experiments/1/creatives-ready", secondCreative.getPath());
        RecordedRequest secondPlaybookRequest = takeBackendRequestMatching(
            "second playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        assertEquals("/api/experiments/1/adset-playbook", secondPlaybookRequest.getPath());
        RecordedRequest renewedCampaign = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/campaigns", renewedCampaign.getPath());

        JsonNode renewedPayload = objectMapper.readTree(renewedCampaign.getBody().inputStream());
        assertEquals("renewed-by-backend", renewedPayload.get("access_token").asText());

        RecordedRequest adSetRequest = takeFacebookRequest("facebook request");
        assertEquals("/v23.0/act_1/adsets", adSetRequest.getPath());
        takeFacebookRequest("facebook request"); // creative
        takeFacebookRequest("facebook request"); // ad

        RecordedRequest backendPost = takeBackendRequestMatching(
            "campaign report",
            request -> "/api/facebook-campaigns".equals(request.getPath()) && "POST".equals(request.getMethod())
        );
        assertTrue(backend.getRequestCount() >= 4);
    }

    @Test
    // Verifies the experiment page is used when worker configuration has no default page.
    void fallsBackToExperimentFacebookPageWhenConfigurationDoesNotProvideOne() throws Exception {
        configurationClient.setConfiguration(configurationWithoutDefaultPageId("token"));

        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\",\"facebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"},\"instagramAccount\":{\"id\":55,\"handle\":\"@estudio\",\"code\":\"IG-EST\",\"name\":\"Estúdio\"}}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
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
        assertEquals("hash-preloaded", creativePayload.get("object_story_spec").get("link_data").get("image_hash").asText());
        assertFalse(creativePayload.get("object_story_spec").get("link_data").has("picture"));

        takeBackendRequest("backend request"); // experiments-ready
        takeBackendRequest("backend request"); // creatives fetch
        RecordedRequest playbookRequest = takeBackendRequestMatching(
            "playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        assertEquals("/api/experiments/1/adset-playbook", playbookRequest.getPath());
        takeBackendRequestMatching(
            "campaign report",
            request -> "/api/facebook-campaigns".equals(request.getPath()) && "POST".equals(request.getMethod())
        );
        assertTrue(backend.getRequestCount() >= 4);
    }

    @Test
    // Verifies page aliases from the experiment payload resolve the Facebook page ID.
    void resolvesPageIdFromAssociatedFacebookPageAlias() throws Exception {
        configurationClient.setConfiguration(configurationWithoutDefaultPageId("token"));

        backend.enqueueResponse(
            new MockResponse()
                .setBody("[{\"id\":1,\"name\":\"Exp\",\"associatedFacebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"}}]")
                .addHeader("Content-Type", "application/json")
        );
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}").addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}").addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}").addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}").addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(
            new MockResponse()
                .setBody(
                    "[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]"
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
        assertEquals("hash-preloaded", creativePayload.get("object_story_spec").get("link_data").get("image_hash").asText());
        assertFalse(creativePayload.get("object_story_spec").get("link_data").has("picture"));

        takeBackendRequest("backend request"); // experiments-ready
        takeBackendRequest("backend request"); // creatives fetch
        RecordedRequest playbookRequest = takeBackendRequestMatching(
            "playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        assertEquals("/api/experiments/1/adset-playbook", playbookRequest.getPath());
        takeBackendRequestMatching(
            "campaign report",
            request -> "/api/facebook-campaigns".equals(request.getPath()) && "POST".equals(request.getMethod())
        );
        assertTrue(backend.getRequestCount() >= 4);
    }

    /**
     * Ensures the Facebook Ads worker uses approved manual job-title targeting when no ad set was pre-generated.
     */
    @Test
    // Verifies approved manual targeting is used when no playbook ad set exists.
    void createsCampaignUsingApprovedManualTargetingPackageWhenNoAdSetExists() throws Exception {
        backend.enqueueResponse(
            new MockResponse()
                .setBody("[{\"id\":1,\"name\":\"Exp\",\"associatedFacebookPage\":{\"id\":9,\"pageId\":\"84\",\"name\":\"Estúdio\"}}]")
                .addHeader("Content-Type", "application/json")
        );
        facebook.enqueueResponse(new MockResponse().setBody("{\"images\":{\"uploaded\":{\"hash\":\"hash-preloaded\"}}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}").addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}").addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}").addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}").addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(
            new MockResponse()
                .setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
                .addHeader("Content-Type", "application/json")
        );
        backend.enqueueResponse(new MockResponse().setBody("{}").addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]").addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(
            new MockResponse()
                .setBody(defaultManualTargetingPackage())
                .addHeader("Content-Type", "application/json")
        );
        backend.enqueueResponse(new MockResponse().setBody("{}").addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}").addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}").addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        takeBackendRequest("backend request"); // experiments-ready
        takeBackendRequest("backend request"); // creatives fetch
        takeBackendRequestMatching(
            "playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        takeBackendRequestMatching(
            "manual targeting package request",
            request -> "/api/facebook-adsets/experiments/1/targeting-package".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );

        takeFacebookRequest("facebook request"); // campaign
        RecordedRequest adSetRequest = takeFacebookRequest("facebook request");
        JsonNode adSetPayload = objectMapper.readTree(adSetRequest.getBody().inputStream());
        JsonNode targeting = adSetPayload.get("targeting");
        assertFalse(targeting.has("work_positions"));
        JsonNode workPositions = targeting.get("flexible_spec").get(0).get("work_positions");
        assertEquals(1, workPositions.size());
        assertEquals("1419795191647433", workPositions.get(0).get("id").asText());
        assertEquals("Certified Personal Trainer", workPositions.get(0).get("name").asText());
    }



    /**
     * Garante que um interesse aprovado no pacote manual já libera a publicação sem exigir cargo.
     */
    @Test
    void createsCampaignUsingApprovedInterestOnlyManualTargetingPackage() throws Exception {
        manualTargetingPackageOverride = interestOnlyManualTargetingPackage();
        backend.enqueueResponse(
            new MockResponse()
                .setBody("""
                    [{"id":1,"name":"Exp","associatedFacebookPage":{"id":9,"pageId":"84","name":"Estúdio"}}]
                    """)
                .addHeader("Content-Type", "application/json")
        );
        facebook.enqueueResponse(new MockResponse().setBody("""
            {"images":{"uploaded":{"hash":"hash-preloaded"}}}
            """)
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}").addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}").addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}").addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}").addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(
            new MockResponse()
                .setBody("""
                    [{"id":101,"experimentId":1,"headline":"HL","primaryText":"Texto Criativo","imageUrl":"%s","description":"Desc","cta":"SHOP_NOW","destinationUrl":"https://exp.example/landing","instagramUserId":"21","status":"READY"}]
                    """.formatted(imageUrl))
                .addHeader("Content-Type", "application/json")
        );
        backend.enqueueResponse(new MockResponse().setBody("{}").addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[]").addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}").addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}").addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}").addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        takeBackendRequest("backend request"); // experiments-ready
        takeBackendRequest("backend request"); // creatives fetch
        takeBackendRequestMatching(
            "playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        takeBackendRequestMatching(
            "manual targeting package request",
            request -> "/api/facebook-adsets/experiments/1/targeting-package".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );

        takeFacebookRequest("facebook request"); // campaign
        RecordedRequest adSetRequest = takeFacebookRequest("facebook request");
        JsonNode adSetPayload = objectMapper.readTree(adSetRequest.getBody().inputStream());
        JsonNode targeting = adSetPayload.get("targeting");
        assertFalse(targeting.has("interests"));
        assertFalse(targeting.has("work_positions"));
        JsonNode interests = targeting.get("flexible_spec").get(0).get("interests");
        assertEquals(1, interests.size());
        assertEquals("6003139266461", interests.get(0).get("id").asText());
        assertEquals("Manicure (cosmetics)", interests.get(0).get("name").asText());
    }

    /**
     * Garante que vídeos de criativo são normalizados antes do upload para a Meta.
     */
    @Test
    void normalizesVideoCreativeBeforeUploadingToMeta() throws Exception {
        byte[] normalizedBytes = new byte[] {9, 8, 7, 6};
        MetaVideoNormalizer normalizer = new MetaVideoNormalizer(true, "ffmpeg", Duration.ofSeconds(1)) {
            @Override
            public NormalizedVideo normalize(byte[] sourceBytes, String sourceFileName) {
                assertArrayEquals(new byte[] {1, 2, 3, 4}, sourceBytes);
                return new NormalizedVideo(normalizedBytes, "creative-video-meta.mp4", "video/mp4", true);
            }

            @Override
            public NormalizedImage extractFallbackFrame(byte[] sourceBytes, String sourceFileName) {
                assertArrayEquals(normalizedBytes, sourceBytes);
                return new NormalizedImage(new byte[] {5, 6, 7}, "creative-video-meta-fallback.jpg", "image/jpeg");
            }
        };
        service = new FacebookCampaignService(
            adsService,
            new StubFacebookAccessTokenManager(adsService, configurationClient),
            webClientBuilder,
            configurationClient,
            backend.url("/").toString(),
            "/api",
            objectMapper,
            apiLogClient,
            normalizer
        );
        configurationClient.setConfiguration(configurationWithTokens("user-token", "system-token"));
        adsService.updateAccessToken("user-token");
        facebook.enqueuePriorityConditionalResponse(
            request -> "/creative-video.mp4".equals(request.getPath()) && "GET".equals(request.getMethod()),
            () -> new MockResponse()
                .setBody(new okio.Buffer().write(new byte[] {1, 2, 3, 4}))
                .addHeader("Content-Type", "video/mp4")
        );
        facebook.enqueueResponse(new MockResponse()
            .setBody("{\"images\":{\"uploaded\":{\"hash\":\"thumb-hash\"}}}")
            .addHeader("Content-Type", "application/json"));
        String uploadPath = "/video-ads-upload/v23.0/video-123";
        facebook.enqueueResponse(new MockResponse()
            .setBody("{\"video_id\":\"video-123\",\"upload_url\":\"" + facebook.url(uploadPath) + "\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setBody("{\"success\":true}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setBody("{\"success\":true}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"10\"}").addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"20\"}").addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"30\"}").addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"id\":\"40\"}").addHeader("Content-Type", "application/json"));

        backend.enqueueResponse(new MockResponse().setBody("""
            [{"id":1,"name":"Exp","facebookPage":{"id":9,"pageId":"84","name":"Estúdio"}}]
            """).addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("""
            [{"id":101,"experimentId":1,"headline":"HL","primaryText":"Texto Criativo","format":"VIDEO","videoUrl":"%s","description":"Desc","cta":"SHOP_NOW","destinationUrl":"https://exp.example/landing","instagramUserId":"21","status":"READY","videoCreative":true,"audibleApprovedVideo":true}]
            """.formatted(videoUrl)).addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        takeBackendRequest("backend request"); // experiments-ready
        takeBackendRequest("backend request"); // creatives fetch
        takeBackendRequestMatching(
            "playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        takeBackendRequestMatching(
            "manual targeting package request",
            request -> "/api/facebook-adsets/experiments/1/targeting-package".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );

        RecordedRequest reach = takeFacebookRequest("reach validation");
        assertTrue(reach.getPath().contains("/reachestimate?"));
        RecordedRequest thumbnailUpload = takeFacebookRequest("video thumbnail ad image upload");
        assertEquals("/v23.0/act_1/adimages", thumbnailUpload.getPath());
        RecordedRequest start = takeFacebookRequest("video start");
        assertEquals("/v23.0/act_1/video_ads", start.getPath());
        assertEquals("system-token", objectMapper.readTree(start.getBody().inputStream()).get("access_token").asText());
        RecordedRequest upload = takeFacebookRequest("video upload");
        assertEquals(uploadPath, upload.getPath());
        assertEquals("OAuth system-token", upload.getHeader("Authorization"));
        assertEquals(normalizedBytes.length, upload.getBodySize());
        assertArrayEquals(normalizedBytes, upload.getBody().readByteArray());
        RecordedRequest finish = takeFacebookRequest("video finish");
        assertEquals("/v23.0/act_1/video_ads", finish.getPath());
        takeFacebookRequest("facebook request"); // campaign
        takeFacebookRequest("facebook request"); // ad set
        RecordedRequest creative = takeFacebookRequest("facebook request");
        JsonNode creativePayload = objectMapper.readTree(creative.getBody().clone().inputStream());
        JsonNode storySpec = creativePayload.get("object_story_spec");
        assertEquals("user-token", creativePayload.get("access_token").asText());
        assertEquals("video-123", storySpec.get("video_data").get("video_id").asText());
        assertEquals("thumb-hash", storySpec.get("video_data").get("image_hash").asText());
        assertFalse(storySpec.get("video_data").has("link"));
    }

    /**
     * Bloqueia publicação quando a Meta rejeita o upload de vídeo, sem converter o criativo para imagem.
     */
    @Test
    void blocksVideoCreativePublicationWhenVideoUploadsFail() throws Exception {
        byte[] normalizedBytes = new byte[] {9, 8, 7, 6};
        MetaVideoNormalizer normalizer = new MetaVideoNormalizer(true, "ffmpeg", Duration.ofSeconds(1)) {
            @Override
            public NormalizedVideo normalize(byte[] sourceBytes, String sourceFileName) {
                assertArrayEquals(new byte[] {1, 2, 3, 4}, sourceBytes);
                return new NormalizedVideo(normalizedBytes, "creative-video-meta.mp4", "video/mp4", true);
            }

            @Override
            public NormalizedImage extractFallbackFrame(byte[] sourceBytes, String sourceFileName) {
                assertArrayEquals(normalizedBytes, sourceBytes);
                return new NormalizedImage(new byte[] {5, 6, 7}, "creative-video-meta-fallback.jpg", "image/jpeg");
            }
        };
        service = new FacebookCampaignService(
            adsService,
            new StubFacebookAccessTokenManager(adsService, configurationClient),
            webClientBuilder,
            configurationClient,
            backend.url("/").toString(),
            "/api",
            objectMapper,
            apiLogClient,
            normalizer
        );
        facebook.enqueuePriorityConditionalResponse(
            request -> "/creative-video.mp4".equals(request.getPath()) && "GET".equals(request.getMethod()),
            () -> new MockResponse()
                .setBody(new okio.Buffer().write(new byte[] {1, 2, 3, 4}))
                .addHeader("Content-Type", "video/mp4")
        );
        facebook.enqueueResponse(new MockResponse()
            .setBody("{\"images\":{\"uploaded\":{\"hash\":\"thumb-hash\"}}}")
            .addHeader("Content-Type", "application/json"));
        String uploadPath = "/video-ads-upload/v23.0/video-123";
        facebook.enqueueResponse(new MockResponse()
            .setBody("{\"video_id\":\"video-123\",\"upload_url\":\"" + facebook.url(uploadPath) + "\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setResponseCode(403)
            .setBody("{\"error\":{\"message\":\"Forbidden\",\"type\":\"OAuthException\",\"code\":200}}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setResponseCode(500)
            .setBody("{\"error\":{\"message\":\"An unknown error has occurred.\",\"type\":\"OAuthException\",\"code\":1}}")
            .addHeader("Content-Type", "application/json"));

        backend.enqueueResponse(new MockResponse().setBody("""
            [{"id":1,"name":"Exp","facebookPage":{"id":9,"pageId":"84","name":"Estúdio"},"publicationJobId":"job-video-required"}]
            """).addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("""
            [{"id":101,"experimentId":1,"headline":"HL","primaryText":"Texto Criativo","format":"VIDEO","videoUrl":"%s","description":"Desc","cta":"SHOP_NOW","destinationUrl":"https://exp.example/landing","instagramUserId":"21","status":"READY","videoCreative":true,"audibleApprovedVideo":true}]
            """.formatted(videoUrl)).addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        takeBackendRequest("backend request"); // experiments-ready
        takeBackendRequest("backend request"); // creatives fetch
        takeBackendRequestMatching(
            "playbook request",
            request -> "/api/experiments/1/adset-playbook".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );
        takeBackendRequestMatching(
            "manual targeting package request",
            request -> "/api/facebook-adsets/experiments/1/targeting-package".equals(request.getPath())
                && "GET".equals(request.getMethod())
        );

        RecordedRequest thumbnailUpload = takeFacebookRequest("video thumbnail ad image upload");
        assertEquals("/v23.0/act_1/adimages", thumbnailUpload.getPath());
        RecordedRequest start = takeFacebookRequest("video start");
        assertEquals("/v23.0/act_1/video_ads", start.getPath());
        RecordedRequest upload = takeFacebookRequest("video upload");
        assertEquals(uploadPath, upload.getPath());
        RecordedRequest legacyVideoUpload = takeFacebookRequest("legacy video upload");
        assertEquals("/v23.0/act_1/advideos", legacyVideoUpload.getPath());
        RecordedRequest reachStep = takeBackendRequestMatching(
            "publication job reach step",
            request -> "/api/facebook-campaigns/publication-job-steps".equals(request.getPath())
                && "POST".equals(request.getMethod())
        );
        JsonNode reachPayload = objectMapper.readTree(reachStep.getBody().inputStream());
        assertEquals("CAMPAIGN_REACH_VALIDATION", reachPayload.get("stepName").asText());
        RecordedRequest failureStep = takeBackendRequestMatching(
            "video upload required failure step",
            request -> "/api/facebook-campaigns/publication-job-steps".equals(request.getPath())
                && "POST".equals(request.getMethod())
                && request.getBody().clone().readUtf8().contains("CAMPAIGN_CREATIVE_VIDEO_UPLOAD_REQUIRED")
        );
        JsonNode failurePayload = objectMapper.readTree(failureStep.getBody().inputStream());
        assertEquals("job-video-required", failurePayload.get("jobId").asText());
        assertEquals("CAMPAIGN_CREATIVE_VIDEO_UPLOAD_REQUIRED", failurePayload.get("stepName").asText());
        assertTrue(failurePayload.get("errorMessage").asText().contains("não será convertida para imagem fallback"));

        RecordedRequest failedStatusRequest = takeBackendRequestMatching(
            "failed status update",
            request -> request.getPath() != null
                && request.getPath().contains("/api/experiments/1/status?status=FAILED")
                && "PATCH".equals(request.getMethod())
        );
        assertNotNull(failedStatusRequest);
        assertEquals(6, facebook.getRequestCount());
    }

    /**
     * Extrai o targeting_spec enviado para a estimativa de alcance da Meta.
     */
    private JsonNode targetingSpecFromReachEstimateRequest(RecordedRequest request) throws IOException {
        String path = request.getPath();
        assertNotNull(path);
        String prefix = "targeting_spec=";
        int start = path.indexOf(prefix);
        assertTrue(start >= 0);
        int valueStart = start + prefix.length();
        int valueEnd = path.indexOf('&', valueStart);
        String encoded = valueEnd >= 0 ? path.substring(valueStart, valueEnd) : path.substring(valueStart);
        return objectMapper.readTree(URLDecoder.decode(encoded, StandardCharsets.UTF_8));
    }

    @Test
    // Garante que vídeo sem áudio aprovado é bloqueado antes de qualquer chamada à Meta.
    void blocksVideoCreativeWithoutAudibleApprovalBeforeFacebookPublication() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("""
            [{"id":1,"name":"Exp","singlePain":"cliente some depois da manutencao","funnelPromise":"encaixes em 7 dias","primaryCta":"Comprar o Mapa 7D","facebookPage":{"id":9,"pageId":"84","name":"Estúdio"},"publicationJobId":"job-audio-gate"}]
            """).addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("""
            [{"id":101,"experimentId":1,"headline":"Cliente some depois da manutencao","primaryText":"Veja encaixes em 7 dias antes do cliente sumir","format":"VIDEO","videoUrl":"https://cdn.example/video-sem-som.mp4","description":"Desc","cta":"SHOP_NOW","destinationUrl":"https://exp.example/landing","instagramUserId":"21","status":"READY","videoCreative":true,"audibleApprovedVideo":false}]
            """).addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setResponseCode(204));

        service.createCampaignsFromExperiments();

        assertEquals("/api/facebook-campaigns/experiments-ready", takeBackendRequest("experiments-ready").getPath());
        assertEquals("/api/facebook-campaigns/experiments/1/creatives-ready", takeBackendRequest("creatives-ready").getPath());
        RecordedRequest failureStep = takeBackendRequestMatching(
            "publication gate failure",
            request -> "/api/facebook-campaigns/publication-job-steps".equals(request.getPath())
                && "POST".equals(request.getMethod())
        );
        JsonNode failurePayload = objectMapper.readTree(failureStep.getBody().inputStream());
        assertEquals("job-audio-gate", failurePayload.get("jobId").asText());
        assertEquals("CAMPAIGN_CREATIVE_PUBLICATION_GATE", failurePayload.get("stepName").asText());
        assertTrue(failurePayload.get("errorMessage").asText().contains("audible audio"));
        RecordedRequest failedStatusRequest = takeBackendRequestMatching(
            "failed status update",
            request -> request.getPath() != null
                && request.getPath().contains("/api/experiments/1/status?status=FAILED")
                && "PATCH".equals(request.getMethod())
        );
        assertNotNull(failedStatusRequest);
        assertEquals(0, facebook.getRequestCount());
    }

    @Test
    // Garante que copy desalinhada com o contrato comercial é bloqueada antes da Meta.
    void blocksMisalignedCreativeCopyBeforeFacebookPublication() throws Exception {
        backend.enqueueResponse(new MockResponse().setBody("""
            [{"id":1,"name":"Exp","singlePain":"cliente some depois da manutencao","funnelPromise":"encaixes em 7 dias","primaryCta":"Comprar o Mapa 7D","facebookPage":{"id":9,"pageId":"84","name":"Estúdio"},"publicationJobId":"job-copy-gate"}]
            """).addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("""
            [{"id":101,"experimentId":1,"headline":"Oferta imperdivel","primaryText":"Clique e veja novidades para mudar sua rotina","imageUrl":"%s","description":"Desc","cta":"LEARN_MORE","destinationUrl":"https://exp.example/landing","instagramUserId":"21","status":"READY"}]
            """.formatted(imageUrl)).addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setResponseCode(204));

        service.createCampaignsFromExperiments();

        takeBackendRequest("experiments-ready");
        takeBackendRequest("creatives-ready");
        RecordedRequest failureStep = takeBackendRequestMatching(
            "publication gate failure",
            request -> "/api/facebook-campaigns/publication-job-steps".equals(request.getPath())
                && "POST".equals(request.getMethod())
        );
        JsonNode failurePayload = objectMapper.readTree(failureStep.getBody().inputStream());
        assertEquals("CAMPAIGN_CREATIVE_PUBLICATION_GATE", failurePayload.get("stepName").asText());
        assertTrue(failurePayload.get("errorMessage").asText().contains("does not match"));
        takeBackendRequestMatching(
            "failed status update",
            request -> request.getPath() != null
                && request.getPath().contains("/api/experiments/1/status?status=FAILED")
                && "PATCH".equals(request.getMethod())
        );
        assertEquals(0, facebook.getRequestCount());
    }

    @Test
    // Verifies experiments without a resolvable page are skipped before Facebook publication.
    void skipsExperimentWhenNoPageCanBeResolved() throws Exception {
        configurationClient.setConfiguration(configurationWithoutDefaultPageId("token"));

        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":1,\"name\":\"Exp\"}]")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setBody("[{\"id\":101,\"experimentId\":1,\"headline\":\"HL\",\"primaryText\":\"Texto Criativo\",\"imageUrl\":\"" + imageUrl + "\",\"description\":\"Desc\",\"cta\":\"SHOP_NOW\",\"destinationUrl\":\"https://exp.example/landing\",\"instagramUserId\":\"21\",\"status\":\"READY\"}]")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromExperiments();

        RecordedRequest experimentsRequest = takeBackendRequest("backend request");
        assertEquals("/api/facebook-campaigns/experiments-ready", experimentsRequest.getPath());
        RecordedRequest creativesRequest = takeBackendRequest("backend request");
        assertEquals("/api/facebook-campaigns/experiments/1/creatives-ready", creativesRequest.getPath());
        assertEquals(0, facebook.getRequestCount());
    }

    @Test
    void pausesCampaignWhenStopRequestIsReturned() throws Exception {
        backend.enqueueResponse(new MockResponse()
            .setBody("[{\"id\":\"camp-1\",\"externalId\":\"98765\",\"experimentId\":55}]")
            .setHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse().setBody("{\"success\":true}")
            .setHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setResponseCode(204));

        service.pauseCampaignsRequestedForStop();

        RecordedRequest stopGet = backend.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(stopGet);
        assertEquals("/api/facebook-campaigns/stop-requests", stopGet.getPath());

        RecordedRequest facebookPost = facebook.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(facebookPost);
        assertEquals("/v23.0/98765", facebookPost.getPath());
        assertTrue(facebookPost.getBody().readUtf8().contains("\"status\":\"PAUSED\""));

        RecordedRequest stopResult = backend.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(stopResult);
        assertEquals("/api/facebook-campaigns/camp-1/stop-results", stopResult.getPath());
        assertTrue(stopResult.getBody().readUtf8().contains("\"success\":true"));
    }

    @Test
    void resolvesStopRequestWithoutExternalId() throws Exception {
        backend.enqueueResponse(new MockResponse()
            .setBody("[{\"id\":\"camp-2\",\"externalId\":null,\"experimentId\":77}]")
            .setHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setResponseCode(204));

        service.pauseCampaignsRequestedForStop();

        RecordedRequest stopGet = backend.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(stopGet);
        assertEquals("/api/facebook-campaigns/stop-requests", stopGet.getPath());

        RecordedRequest stopResult = backend.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(stopResult);
        assertEquals("/api/facebook-campaigns/camp-2/stop-results", stopResult.getPath());
        assertTrue(stopResult.getBody().readUtf8().contains("\"success\":true"));
        assertEquals(0, facebook.getRequestCount());
    }


    private FacebookWorkerConfiguration configurationWithAccessToken(String accessToken) {
        return configurationWithTokens(accessToken, accessToken);
    }

    private FacebookWorkerConfiguration configurationWithTokens(String accessToken, String systemUserAccessToken) {
        return new FacebookWorkerConfiguration(
            1L,
            "1",
            accessToken,
            systemUserAccessToken,
            "123456789012345",
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
            accessToken,
            "123456789012345",
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
