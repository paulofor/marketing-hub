package com.marketinghub.facebookads;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

class FacebookAdsClientTest {

    @Test
    void getAdAccountsReturnsJson() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("{\"data\":[{\"id\":\"1\",\"name\":\"Test\"}]}"));
        server.start();
        String baseUrl = server.url("/").toString();
        DefaultFacebookAdsClient client = new DefaultFacebookAdsClient("TOKEN", "v19.0", baseUrl);
        JsonNode json = client.getAdAccounts();
        assertThat(json.path("data").get(0).path("id").asText()).isEqualTo("1");
        server.shutdown();
    }

    @Test
    void createCampaignReturnsId() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("{\"id\":\"321\"}"));
        server.start();
        DefaultFacebookAdsClient client = new DefaultFacebookAdsClient("TOKEN", "v19.0", server.url("/").toString());
        JsonNode json = client.createCampaign("1", "Camp", "OUTCOME_TRAFFIC");
        assertThat(json.path("id").asText()).isEqualTo("321");
        server.shutdown();
    }

    @Test
    void getCampaignInsightsReturnsJson() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("{\"data\":[]}"));
        server.start();
        DefaultFacebookAdsClient client = new DefaultFacebookAdsClient("TOKEN", "v19.0", server.url("/").toString());
        JsonNode json = client.getCampaignInsights("99");
        assertThat(json.path("data").isArray()).isTrue();
        server.shutdown();
    }
}
