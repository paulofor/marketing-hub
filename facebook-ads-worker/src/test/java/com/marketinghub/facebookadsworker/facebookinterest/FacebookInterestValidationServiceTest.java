package com.marketinghub.facebookadsworker.facebookinterest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.testsupport.FailFastMockWebServer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FacebookInterestValidationServiceTest {
    private FailFastMockWebServer backend;
    private FailFastMockWebServer facebook;
    private FacebookInterestValidationService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        backend = new FailFastMockWebServer();
        facebook = new FailFastMockWebServer();
        backend.start();
        facebook.start();

        objectMapper = new ObjectMapper();

        FacebookAdsService facebookAdsService = new FacebookAdsService(
            WebClient.builder(),
            facebook.url("/").toString(),
            "v23.0",
            objectMapper
        );
        facebookAdsService.updateAccessToken("token");

        FacebookInterestValidationClient validationClient = new FacebookInterestValidationClient(
            WebClient.builder(),
            backend.url("/").toString(),
            "/api"
        );

        service = new FacebookInterestValidationService(
            WebClient.builder(),
            facebookAdsService,
            validationClient,
            objectMapper,
            backend.url("/").toString(),
            "/api"
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        backend.assertNoUnmatchedRequests();
        facebook.assertNoUnmatchedRequests();
        backend.shutdown();
        facebook.shutdown();
    }

    @Test
    void validatePendingInterestsResolvesMatch() throws Exception {
        backend.enqueueResponse(new MockResponse()
            .setBody("[{\"id\":1,\"name\":\"Pilates\"}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setBody("{\"data\":[{\"id\":\"6003139266461\",\"name\":\"Pilates Training\"}]}" )
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setResponseCode(204));

        service.validatePendingInterests();

        RecordedRequest pendingRequest = backend.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(pendingRequest);
        assertEquals("/api/facebook-interests/pending", pendingRequest.getPath());

        RecordedRequest searchRequest = facebook.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(searchRequest);
        assertEquals("/v23.0/search", searchRequest.getRequestUrl().encodedPath());
        assertEquals("adinterest", searchRequest.getRequestUrl().queryParameter("type"));
        assertEquals("Pilates", searchRequest.getRequestUrl().queryParameter("q"));
        assertEquals("pt_BR", searchRequest.getRequestUrl().queryParameter("locale"));

        RecordedRequest updateRequest = backend.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(updateRequest);
        assertEquals("/api/facebook-interests/1", updateRequest.getPath());
        JsonNode body = objectMapper.readTree(updateRequest.getBody().inputStream());
        assertEquals("VALID", body.get("status").asText());
        assertEquals("6003139266461", body.get("facebookInterestId").asText());
        assertEquals("Pilates Training", body.get("name").asText());
        assertEquals(1, facebook.getRequestCount());
    }

    @Test
    void validatePendingInterestsMarksInvalidWhenNotFound() throws Exception {
        backend.enqueueResponse(new MockResponse()
            .setBody("[{\"id\":2,\"name\":\"Unknown Interest\"}]")
            .addHeader("Content-Type", "application/json"));
        enqueueEmptyInterestResponse();
        enqueueEmptyInterestResponse();
        enqueueEmptyInterestResponse();
        backend.enqueueResponse(new MockResponse().setResponseCode(204));

        service.validatePendingInterests();

        RecordedRequest pendingRequest = backend.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(pendingRequest);
        assertEquals("/api/facebook-interests/pending", pendingRequest.getPath());

        List<String> locales = Arrays.asList("pt_BR", "en_US", null);
        for (String locale : locales) {
            RecordedRequest searchRequest = facebook.takeRequest(5, TimeUnit.SECONDS);
            assertNotNull(searchRequest);
            assertEquals("/v23.0/search", searchRequest.getRequestUrl().encodedPath());
            assertEquals("adinterest", searchRequest.getRequestUrl().queryParameter("type"));
            assertEquals("Unknown Interest", searchRequest.getRequestUrl().queryParameter("q"));
            assertEquals(locale, searchRequest.getRequestUrl().queryParameter("locale"));
        }

        RecordedRequest updateRequest = backend.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(updateRequest);
        assertEquals("/api/facebook-interests/2", updateRequest.getPath());
        JsonNode body = objectMapper.readTree(updateRequest.getBody().inputStream());
        assertEquals("INVALID", body.get("status").asText());
        assertNull(body.get("facebookInterestId").textValue());
        assertNull(body.get("name").textValue());
        assertEquals(3, facebook.getRequestCount());
    }

    private void enqueueEmptyInterestResponse() {
        facebook.enqueueResponse(new MockResponse()
            .setBody("{\"data\":[]}")
            .addHeader("Content-Type", "application/json"));
    }
}
