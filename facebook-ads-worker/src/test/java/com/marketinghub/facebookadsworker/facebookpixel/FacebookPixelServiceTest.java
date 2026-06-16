package com.marketinghub.facebookadsworker.facebookpixel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingResolverProperties;
import com.marketinghub.facebookadsworker.testsupport.FailFastMockWebServer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valida o fluxo de criação de pixels solicitados pelo backend.
 */
class FacebookPixelServiceTest {
    private FailFastMockWebServer backend;
    private FailFastMockWebServer facebook;
    private ObjectMapper objectMapper;
    private FacebookPixelService service;

    @BeforeEach
    // Prepara servidores simulados e o serviço testado.
    void setUp() throws IOException {
        backend = new FailFastMockWebServer();
        facebook = new FailFastMockWebServer();
        backend.start();
        facebook.start();
        objectMapper = new ObjectMapper();

        TargetingResolverProperties resolverProperties = new TargetingResolverProperties();
        FacebookAdsService facebookAdsService = new FacebookAdsService(
            WebClient.builder(),
            facebook.url("/").toString(),
            "v23.0",
            objectMapper,
            resolverProperties
        );
        FacebookWorkerConfigurationClient configurationClient = new FacebookWorkerConfigurationClient(
            WebClient.builder(),
            backend.url("/").toString(),
            "/api"
        );
        service = new FacebookPixelService(
            facebookAdsService,
            WebClient.builder(),
            configurationClient,
            backend.url("/").toString(),
            "/api",
            objectMapper,
            true
        );
    }

    @AfterEach
    // Encerra os servidores simulados e verifica requisições inesperadas.
    void tearDown() throws IOException {
        backend.assertNoUnmatchedRequests();
        facebook.assertNoUnmatchedRequests();
        backend.shutdown();
        facebook.shutdown();
    }

    @Test
    // Garante que a pendência de pixel é executada mesmo sem token de system user e sem business owner.
    void syncPixelsCreatesPendingPixelWithoutSystemUserTokenOrOwnerBusiness() throws Exception {
        backend.enqueueResponse(new MockResponse()
            .setBody("""
                {
                  "accountId": 1,
                  "adAccountId": "1234567890",
                  "accessToken": "main-token"
                }
                """)
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse()
            .setBody("[{\"nicheId\":21,\"nicheName\":\"GerProj 5\"}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setBody("{\"id\":\"pixel-21\"}")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueueResponse(new MockResponse()
            .setBody("{\"code\":\"<script>pixel</script>\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueueResponse(new MockResponse().setResponseCode(200));
        backend.enqueueResponse(new MockResponse()
            .setBody("[]")
            .addHeader("Content-Type", "application/json"));

        service.syncPixelsAndConversions();

        RecordedRequest configRequest = backend.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(configRequest);
        assertEquals("/api/accounts/facebook/worker-config", configRequest.getPath());
        RecordedRequest pendingRequest = backend.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(pendingRequest);
        assertEquals("/api/facebook-pixels/pending", pendingRequest.getPath());
        RecordedRequest createRequest = facebook.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(createRequest);
        assertEquals("/v23.0/act_1234567890/adspixels", createRequest.getPath());
        String createBody = createRequest.getBody().readUtf8();
        assertTrue(createBody.contains("\"access_token\":\"main-token\""));
        assertFalse(createBody.contains("owner_business"));
        RecordedRequest codeRequest = facebook.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(codeRequest);
        assertTrue(codeRequest.getPath().contains("/v23.0/pixel-21"));
        RecordedRequest registerRequest = backend.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(registerRequest);
        assertEquals("/api/facebook-pixels", registerRequest.getPath());
        String registerBody = registerRequest.getBody().readUtf8();
        assertTrue(registerBody.contains("\"nicheId\":21"));
        assertTrue(registerBody.contains("\"pixelId\":\"pixel-21\""));
        RecordedRequest conversionsRequest = backend.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(conversionsRequest);
        assertEquals("/api/facebook-pixels/conversions-ready", conversionsRequest.getPath());
    }
}
