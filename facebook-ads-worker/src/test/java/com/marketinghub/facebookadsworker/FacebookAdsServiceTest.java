package com.marketinghub.facebookadsworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookPermissionException;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingResolverProperties;
import com.marketinghub.facebookadsworker.testsupport.FailFastMockWebServer;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacebookAdsServiceTest {
    private FailFastMockWebServer server;
    private FacebookAdsService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        server = new FailFastMockWebServer();
        server.start();
        String baseUrl = server.url("/").toString();
        objectMapper = new ObjectMapper();
        TargetingResolverProperties resolverProperties = new TargetingResolverProperties();
        resolverProperties.setDefaultAdAccountId("act_1234567890");
        service = new FacebookAdsService(WebClient.builder(), baseUrl, "v23.0", objectMapper, resolverProperties);
        service.updateAccessToken("token");
    }

    @AfterEach
    void tearDown() throws IOException {
        server.assertNoUnmatchedRequests();
        server.shutdown();
    }

    private RecordedRequest takeRequest(String description) throws InterruptedException {
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request, "Expected request (" + description + ") within timeout.");
        return request;
    }

    @Test
    void createCampaignPostsCorrectRequest() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"id\":\"123\"}")
            .addHeader("Content-Type", "application/json"));
        String id = service.createInstagramCampaign("1", "Camp");
        RecordedRequest request = takeRequest("request");
        assertEquals("/v23.0/act_1/campaigns", request.getPath());
        JsonNode body = objectMapper.readTree(request.getBody().inputStream());
        assertEquals("Camp", body.get("name").asText());
        assertEquals("OUTCOME_TRAFFIC", body.get("objective").asText());
        assertEquals("PAUSED", body.get("status").asText());
        assertEquals(0, body.get("special_ad_categories").size());
        assertEquals("123", id);
    }

    @Test
    void createCampaignUsesProvidedObjectiveWhenPresent() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"id\":\"456\"}")
            .addHeader("Content-Type", "application/json"));
        String id = service.createCampaign("1", "Camp", "OUTCOME_LEADS");
        RecordedRequest request = takeRequest("request");
        assertEquals("/v23.0/act_1/campaigns", request.getPath());
        JsonNode body = objectMapper.readTree(request.getBody().inputStream());
        assertEquals("Camp", body.get("name").asText());
        assertEquals("OUTCOME_LEADS", body.get("objective").asText());
        assertEquals("456", id);
    }

    @Test
    void createAdSetPostsCorrectRequest() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"id\":\"222\"}")
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
            "BR",
            null,
            Collections.emptyList()
        );
        String id = service.createAdSet("1", request);
        RecordedRequest recorded = takeRequest("request");
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
        assertFalse(body.has("bid_amount"));
        assertEquals("BR", body.get("targeting").get("geo_locations").get("countries").get(0).asText());
        assertEquals("42", body.get("promoted_object").get("page_id").asText());
        assertEquals("222", id);
    }

    @Test
    void createAdSetIncludesBidAmountForManualStrategies() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"id\":\"333\"}")
            .addHeader("Content-Type", "application/json"));
        FacebookAdsService.AdSetRequest request = new FacebookAdsService.AdSetRequest(
            "Camp - Ad Set",
            "123",
            "1500",
            "IMPRESSIONS",
            "LINK_CLICKS",
            "WEBSITE",
            "COST_CAP",
            "200",
            "42",
            "BR",
            null,
            Collections.emptyList()
        );
        service.createAdSet("1", request);
        RecordedRequest recorded = takeRequest("request");
        JsonNode body = objectMapper.readTree(recorded.getBody().inputStream());
        assertEquals("COST_CAP", body.get("bid_strategy").asText());
        assertEquals("200", body.get("bid_amount").asText());
    }


    @Test
    void createAdSetRemovesLanguagesFromTargeting() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"id\":\"555\"}")
            .addHeader("Content-Type", "application/json"));
        String targetingJson = "{\"languages\":[55,66],\"geo_locations\":{\"countries\":[\"BR\"]}}";
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
            "BR",
            targetingJson,
            Collections.emptyList()
        );

        service.createAdSet("1", request);

        RecordedRequest recorded = takeRequest("request");
        JsonNode targeting = objectMapper.readTree(recorded.getBody().inputStream()).get("targeting");
        assertNull(targeting.get("languages"));
        JsonNode locales = targeting.get("locales");
        assertNotNull(locales);
        assertEquals(2, locales.size());
        assertEquals(55, locales.get(0).asInt());
        assertEquals(66, locales.get(1).asInt());
    }

    @Test
    void createAdSetConvertsLocaleCodesToNumericIds() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"id\":\"555\"}")
            .addHeader("Content-Type", "application/json"));
        String targetingJson = "{\"languages\":[\"pt_BR\",\"6\",\"999\"],\"geo_locations\":{\"countries\":[\"BR\"]}}";
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
            "BR",
            targetingJson,
            Collections.emptyList()
        );

        service.createAdSet("1", request);

        RecordedRequest recorded = takeRequest("request");
        JsonNode targeting = objectMapper.readTree(recorded.getBody().inputStream()).get("targeting");
        JsonNode locales = targeting.get("locales");
        assertNotNull(locales);
        assertEquals(2, locales.size());
        assertEquals(16, locales.get(0).asInt());
        assertEquals(6, locales.get(1).asInt());
    }

    @Test
    void createAdSetCoercesExistingLocalesToNumericIds() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"id\":\"555\"}")
            .addHeader("Content-Type", "application/json"));
        String targetingJson = "{\"locales\":[\"pt_br\",\"foo\",55],\"geo_locations\":{\"countries\":[\"BR\"]}}";
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
            "BR",
            targetingJson,
            Collections.emptyList()
        );

        service.createAdSet("1", request);

        RecordedRequest recorded = takeRequest("request");
        JsonNode targeting = objectMapper.readTree(recorded.getBody().inputStream()).get("targeting");
        JsonNode locales = targeting.get("locales");
        assertNotNull(locales);
        assertEquals(2, locales.size());
        assertEquals(16, locales.get(0).asInt());
        assertEquals(55, locales.get(1).asInt());
    }

    @Test
    void createAdSetRemovesNonNumericRegionsFromTargeting() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"id\":\"555\"}")
            .addHeader("Content-Type", "application/json"));
        String targetingJson = "{\"geo_locations\":{\"regions\":[{\"key\":\"SP\"},{\"key\":123},{\"key\":\"456\"}],\"countries\":[\"BR\"]}}";
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
            "BR",
            targetingJson,
            Collections.emptyList()
        );

        service.createAdSet("1", request);

        RecordedRequest recorded = takeRequest("request");
        JsonNode targeting = objectMapper.readTree(recorded.getBody().inputStream()).get("targeting");
        JsonNode regions = targeting.get("geo_locations").get("regions");
        assertEquals(2, regions.size());
        assertEquals(123, regions.get(0).get("key").asInt());
        assertEquals(456, regions.get(1).get("key").asInt());
    }

    @Test
    void createAdSetResolvesInterestNamesToIds() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"data\":[{\"id\":\"6003139266461\",\"name\":\"Pilates\"}]}" )
            .addHeader("Content-Type", "application/json"));
        server.enqueueResponse(new MockResponse().setBody("{\"id\":\"555\"}")
            .addHeader("Content-Type", "application/json"));

        String targetingJson = "{\"interests\":[\"Pilates\"],\"geo_locations\":{\"countries\":[\"BR\"]}}";
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
            "BR",
            targetingJson,
            Collections.emptyList()
        );

        service.createAdSet("1", request);

        RecordedRequest searchRequest = takeRequest("request");
        HttpUrl searchUrl = searchRequest.getRequestUrl();
        assertNotNull(searchUrl);
        assertEquals("/v23.0/act_1234567890/targetingsearch", searchUrl.encodedPath());
        assertEquals("adinterest", searchUrl.queryParameter("type"));
        assertEquals("Pilates", searchUrl.queryParameter("q"));
        assertEquals("1", searchUrl.queryParameter("limit"));
        assertEquals("id,name", searchUrl.queryParameter("fields"));
        assertEquals("pt_BR", searchUrl.queryParameter("locale"));
        assertEquals("token", searchUrl.queryParameter("access_token"));

        RecordedRequest adSetRequest = takeRequest("request");
        JsonNode targeting = objectMapper.readTree(adSetRequest.getBody().inputStream()).get("targeting");
        JsonNode interests = targeting.get("interests");
        assertNotNull(interests);
        assertEquals(1, interests.size());
        assertEquals("6003139266461", interests.get(0).get("id").asText());
        assertEquals("Pilates", interests.get(0).get("name").asText());
    }

    @Test
    void createAdCreativePostsCorrectRequest() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"id\":\"333\"}")
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
        RecordedRequest recorded = takeRequest("request");
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
        server.enqueueResponse(new MockResponse().setBody("{\"id\":\"999\"}")
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
        RecordedRequest recorded = takeRequest("request");
        JsonNode body = objectMapper.readTree(recorded.getBody().inputStream());
        JsonNode linkData = body.get("object_story_spec").get("link_data");
        assertEquals("https://www.facebook.com/ads/leadgen/?id=123456789012345", linkData.get("link").asText());
        JsonNode callToAction = linkData.get("call_to_action");
        assertEquals("SIGN_UP", callToAction.get("type").asText());
        assertEquals("https://www.facebook.com/ads/leadgen/?id=123456789012345", callToAction.get("value").get("link").asText());
        assertEquals("123456789012345", callToAction.get("value").get("lead_gen_form_id").asText());
        assertEquals("999", id);
    }

    @Test
    void suggestTargetingOptionsUsesInterestSuggestionsSearch() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"data\":[{\"id\":\"600313\",\"name\":\"Pilates\",\"audience_size\":1200000,\"path\":[\"Interesses\",\"Fitness\"]}]}" )
            .addHeader("Content-Type", "application/json"));

        FacebookAdsService.TargetingSuggestionsRequest request = new FacebookAdsService.TargetingSuggestionsRequest(
            "1",
            List.of(new FacebookAdsService.TargetingSuggestionSeed("Pilates", "adinterest")),
            "pt_BR",
            "BR",
            50
        );

        List<FacebookAdsService.FacebookTargetingSuggestionResult> results = service.suggestTargetingOptions(request);

        RecordedRequest searchRequest = takeRequest("request");
        HttpUrl searchUrl = searchRequest.getRequestUrl();
        assertNotNull(searchUrl);
        assertEquals("/v23.0/search", searchUrl.encodedPath());
        assertEquals("adinterestsuggestion", searchUrl.queryParameter("type"));
        assertEquals("[\"Pilates\"]", searchUrl.queryParameter("interest_list"));
        assertEquals("50", searchUrl.queryParameter("limit"));
        assertEquals("pt_BR", searchUrl.queryParameter("locale"));
        assertEquals("token", searchUrl.queryParameter("access_token"));

        assertEquals(1, results.size());
        assertEquals("600313", results.get(0).id());
        assertEquals("Pilates", results.get(0).name());
    }

    @Test
    void createAdCreativeKeepsWebsiteLinkWhenLeadFormIsPresent() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"id\":\"998\"}")
            .addHeader("Content-Type", "application/json"));
        FacebookAdsService.AdCreativeRequest request = new FacebookAdsService.AdCreativeRequest(
            "Camp - Lead",
            "42",
            null,
            "https://example.com/landing",
            "123456789012345",
            "Mensagem",
            null,
            null,
            "SIGN_UP",
            null,
            null
        );

        service.createAdCreative("1", request);

        RecordedRequest recorded = takeRequest("request");
        JsonNode linkData = objectMapper
            .readTree(recorded.getBody().inputStream())
            .get("object_story_spec")
            .get("link_data");
        assertEquals("https://example.com/landing", linkData.get("link").asText());
        assertEquals("https://example.com/landing", linkData.get("call_to_action").get("value").get("link").asText());
        assertEquals("123456789012345", linkData.get("call_to_action").get("value").get("lead_gen_form_id").asText());
    }

    @Test
    void createAdCreativeFallsBackToPictureWhenHashMissing() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"id\":\"777\"}")
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

        RecordedRequest recorded = takeRequest("request");
        JsonNode linkData = objectMapper
            .readTree(recorded.getBody().inputStream())
            .get("object_story_spec")
            .get("link_data");
        assertEquals("https://cdn.example/img.jpg", linkData.get("picture").asText());
    }

    @Test
    void uploadAdImageReturnsFirstHash() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"images\":{\"image1\":{\"hash\":\"abc\"}}}")
            .addHeader("Content-Type", "application/json"));

        String hash = service.uploadAdImage("1", "https://cdn.example/img.jpg");

        RecordedRequest request = takeRequest("request");
        assertEquals("/v23.0/act_1/adimages", request.getPath());
        JsonNode body = objectMapper.readTree(request.getBody().inputStream());
        assertEquals("https://cdn.example/img.jpg", body.get("url").asText());
        assertEquals("abc", hash);
    }

    @Test
    void createAdPostsCorrectRequest() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"id\":\"444\"}")
            .addHeader("Content-Type", "application/json"));
        FacebookAdsService.AdRequest request = new FacebookAdsService.AdRequest(
            "Camp - Ad",
            "adset",
            "creative"
        );
        String id = service.createAd("1", request);
        RecordedRequest recorded = takeRequest("request");
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
        server.enqueueResponse(new MockResponse()
            .setBody("{\"access_token\":\"page-token\"}")
            .addHeader("Content-Type", "application/json"));
        server.enqueueResponse(new MockResponse()
            .setBody("{\"data\":[{\"id\":\"form-1\",\"name\":\"Form Teste\",\"status\":\"DRAFT\"}]}")
            .addHeader("Content-Type", "application/json"));

        String identifier = service.findInstantFormIdentifier("999", "Form Teste");

        assertEquals("form-1", identifier);

        RecordedRequest pageTokenRequest = takeRequest("request");
        assertEquals("/v23.0/999?fields=access_token&access_token=token", pageTokenRequest.getPath());

        RecordedRequest formsRequest = takeRequest("request");
        assertEquals(
            "/v23.0/999/leadgen_forms?fields=id,name,status,draft_id&limit=200&access_token=page-token",
            formsRequest.getPath()
        );
    }

    @Test
    void findInstantFormIdentifierCachesPageAccessToken() throws Exception {
        server.enqueueResponse(new MockResponse()
            .setBody("{\"access_token\":\"cached-page-token\"}")
            .addHeader("Content-Type", "application/json"));
        server.enqueueResponse(new MockResponse()
            .setBody("{\"data\":[]}")
            .addHeader("Content-Type", "application/json"));
        server.enqueueResponse(new MockResponse()
            .setBody("{\"data\":[]}")
            .addHeader("Content-Type", "application/json"));

        service.findInstantFormIdentifier("321", "Nome");
        service.findInstantFormIdentifier("321", "Nome");

        RecordedRequest first = takeRequest("request");
        assertEquals("/v23.0/321?fields=access_token&access_token=token", first.getPath());

        RecordedRequest second = takeRequest("request");
        assertEquals(
            "/v23.0/321/leadgen_forms?fields=id,name,status,draft_id&limit=200&access_token=cached-page-token",
            second.getPath()
        );

        RecordedRequest third = takeRequest("request");
        assertEquals(
            "/v23.0/321/leadgen_forms?fields=id,name,status,draft_id&limit=200&access_token=cached-page-token",
            third.getPath()
        );

        assertNull(server.takeRequest(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void findInstantFormIdentifierReturnsNullWhenPageTokenMissing() throws Exception {
        server.enqueueResponse(new MockResponse()
            .setBody("{\"data\":{}}")
            .addHeader("Content-Type", "application/json"));

        String identifier = service.findInstantFormIdentifier("432", "Teste");

        assertNull(identifier);

        RecordedRequest request = takeRequest("request");
        assertEquals("/v23.0/432?fields=access_token&access_token=token", request.getPath());
        assertNull(server.takeRequest(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void findInstantFormIdentifierPropagatesPermissionErrors() {
        server.enqueueResponse(new MockResponse()
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
        server.enqueueResponse(new MockResponse().setBody("{\"status\":\"ACTIVE\"}")
            .addHeader("Content-Type", "application/json"));

        service.publishInstantForm("123");

        RecordedRequest statusCheck = takeRequest("request");
        assertEquals("GET", statusCheck.getMethod());
        assertEquals("/v23.0/123?access_token=token", statusCheck.getPath());
        assertNull(server.takeRequest(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void publishInstantFormPostsWhenStatusIsNotActive() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"status\":\"DRAFT\"}")
            .addHeader("Content-Type", "application/json"));
        server.enqueueResponse(new MockResponse().setBody("{\"success\":true}")
            .addHeader("Content-Type", "application/json"));

        service.publishInstantForm(" 123 ");

        RecordedRequest statusCheck = takeRequest("request");
        assertEquals("GET", statusCheck.getMethod());
        assertEquals("/v23.0/123?access_token=token", statusCheck.getPath());

        RecordedRequest publishRequest = takeRequest("request");
        assertEquals("POST", publishRequest.getMethod());
        assertEquals("/v23.0/123", publishRequest.getPath());
        JsonNode body = objectMapper.readTree(publishRequest.getBody().inputStream());
        assertEquals("ACTIVE", body.get("status").asText());
        assertEquals("token", body.get("access_token").asText());
    }


    @Test
    void validateTargetingSpecEncodesTargetingSpecQueryParam() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"data\":[]}")
            .addHeader("Content-Type", "application/json"));

        JsonNode targetingSpec = objectMapper.readTree("""
            {
              "geo_locations": {
                "countries": ["BR"]
              },
              "age_min": 18
            }
            """);

        JsonNode response = service.validateTargetingSpec(
            new FacebookAdsService.TargetingValidationRequest("act_123", targetingSpec)
        );

        assertNotNull(response);
        RecordedRequest recorded = takeRequest("targeting validation request");
        assertTrue(recorded.getPath().startsWith("/v23.0/act_123/targetingvalidation?"));
        assertFalse(recorded.getPath().contains("targeting_spec={"));
        HttpUrl requestUrl = recorded.getRequestUrl();
        assertNotNull(requestUrl);
        String targetingSpecParam = requestUrl.queryParameter("targeting_spec");
        assertNotNull(targetingSpecParam);
        assertTrue(targetingSpecParam.contains("geo_locations"));
    }

    @Test
    void estimateReachEncodesTargetingSpecQueryParam() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"data\":[{\"users\":1234}]}")
            .addHeader("Content-Type", "application/json"));

        JsonNode targetingSpec = objectMapper.readTree("""
            {
              "geo_locations": {
                "countries": ["BR"]
              },
              "age_min": 18
            }
            """);

        JsonNode response = service.estimateReach(
            new FacebookAdsService.ReachEstimateRequest("act_123", targetingSpec)
        );

        assertNotNull(response);
        RecordedRequest recorded = takeRequest("reach estimate request");
        assertTrue(recorded.getPath().startsWith("/v23.0/act_123/reachestimate?"));
        assertFalse(recorded.getPath().contains("targeting_spec={"));
        HttpUrl requestUrl = recorded.getRequestUrl();
        assertNotNull(requestUrl);
        String targetingSpecParam = requestUrl.queryParameter("targeting_spec");
        assertNotNull(targetingSpecParam);
        assertTrue(targetingSpecParam.contains("geo_locations"));
    }


    @Test
    void estimateReachRetriesWithoutInvalidInterestFromFlexibleSpec() throws Exception {
        server.enqueueResponse(new MockResponse()
            .setResponseCode(400)
            .setBody("""
                {"error":{"message":"Invalid parameter","type":"OAuthException","code":100,"error_subcode":1487079,
                "error_user_msg":"Invalid data for field interests. O interesse com ID 6004382299972 é inválido."}}
                """)
            .addHeader("Content-Type", "application/json"));
        server.enqueueResponse(new MockResponse().setBody("{\"data\":[{\"users\":1234}]}" )
            .addHeader("Content-Type", "application/json"));

        JsonNode targetingSpec = objectMapper.readTree("""
            {
              "geo_locations": {
                "countries": ["BR"]
              },
              "age_min": 18,
              "flexible_spec": [
                {
                  "interests": [
                    {"id": "6004382299972", "name": "Invalid Interest"},
                    {"id": "6004382299973", "name": "Valid Interest"}
                  ]
                }
              ]
            }
            """);

        JsonNode response = service.estimateReach(
            new FacebookAdsService.ReachEstimateRequest("act_123", targetingSpec)
        );

        assertNotNull(response);
        RecordedRequest firstAttempt = takeRequest("reach estimate first attempt");
        RecordedRequest secondAttempt = takeRequest("reach estimate second attempt");
        HttpUrl firstUrl = firstAttempt.getRequestUrl();
        HttpUrl secondUrl = secondAttempt.getRequestUrl();
        assertNotNull(firstUrl);
        assertNotNull(secondUrl);
        String firstTargetingSpec = firstUrl.queryParameter("targeting_spec");
        String secondTargetingSpec = secondUrl.queryParameter("targeting_spec");
        assertNotNull(firstTargetingSpec);
        assertNotNull(secondTargetingSpec);
        assertTrue(firstTargetingSpec.contains("6004382299972"));
        assertFalse(secondTargetingSpec.contains("6004382299972"));
        assertTrue(secondTargetingSpec.contains("6004382299973"));
    }

    @Test
    void validateTargetingSpecDoesNotRetryWhenInvalidInterestIdIsMissing() throws Exception {
        server.enqueueResponse(new MockResponse()
            .setResponseCode(400)
            .setBody("""
                {"error":{"message":"Invalid parameter","type":"OAuthException","code":100,"error_subcode":1487079,
                "error_user_msg":"Invalid data for field interests."}}
                """)
            .addHeader("Content-Type", "application/json"));

        JsonNode targetingSpec = objectMapper.readTree("""
            {
              "geo_locations": {
                "countries": ["BR"]
              },
              "interests": [
                {"id": "6004382299972", "name": "Interest"}
              ]
            }
            """);

        RuntimeException thrown = assertThrows(
            RuntimeException.class,
            () -> service.validateTargetingSpec(
                new FacebookAdsService.TargetingValidationRequest("act_123", targetingSpec)
            )
        );
        assertTrue(thrown.getCause() instanceof WebClientResponseException);

        takeRequest("targeting validation attempt");
        assertNull(server.takeRequest(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void metricsRequestsCampaignInsights() throws Exception {
        server.enqueueResponse(new MockResponse().setBody("{\"data\":[{\"impressions\":\"10\"}]}")
            .addHeader("Content-Type", "application/json"));
        JsonNode node = service.getCampaignInsights("77", Map.of("fields", "impressions"));
        RecordedRequest request = takeRequest("request");
        assertEquals("/v23.0/77/insights?access_token=token&fields=impressions", request.getPath());
        assertEquals("10", node.get("data").get(0).path("impressions").asText());
    }
}
