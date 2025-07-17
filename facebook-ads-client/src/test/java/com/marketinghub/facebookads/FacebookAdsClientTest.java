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
}
