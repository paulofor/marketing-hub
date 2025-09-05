package com.marketinghub.facebookadsworker.instagramcampaign;

import com.marketinghub.facebookadsworker.FacebookAdsService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstagramCampaignServiceTest {
    private MockWebServer backend;
    private MockWebServer facebook;
    private InstagramCampaignService service;

    @BeforeEach
    void setUp() throws IOException {
        backend = new MockWebServer();
        backend.start();
        facebook = new MockWebServer();
        facebook.start();

        String backendUrl = backend.url("/").toString();
        String facebookUrl = facebook.url("/").toString();

        FacebookAdsService adsService = new FacebookAdsService(WebClient.builder(), facebookUrl, "token");
        service = new InstagramCampaignService(adsService, WebClient.builder(), backendUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        backend.shutdown();
        facebook.shutdown();
    }

    @Test
    void createsCampaignForEachAuthorizedCreative() throws Exception {
        backend.enqueue(new MockResponse().setBody("[{\"id\":1,\"adAccountId\":\"1\",\"name\":\"Camp\"}]")
            .addHeader("Content-Type", "application/json"));
        facebook.enqueue(new MockResponse().setBody("{\"id\":\"10\"}")
            .addHeader("Content-Type", "application/json"));
        backend.enqueue(new MockResponse().setBody("{}")
            .addHeader("Content-Type", "application/json"));

        service.createCampaignsFromAuthorizedCreatives();

        RecordedRequest get = backend.takeRequest();
        assertEquals("/instagram-creatives/approved", get.getPath());
        RecordedRequest postFb = facebook.takeRequest();
        assertEquals("/v20.0/act_1/campaigns", postFb.getPath());
        RecordedRequest postBackend = backend.takeRequest();
        assertEquals("/instagram-creatives/1/campaign", postBackend.getPath());
    }
}

