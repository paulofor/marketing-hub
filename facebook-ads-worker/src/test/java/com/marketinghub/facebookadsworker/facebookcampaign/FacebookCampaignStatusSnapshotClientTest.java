package com.marketinghub.facebookadsworker.facebookcampaign;

import com.marketinghub.facebookadsworker.testsupport.FailFastMockWebServer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Testa a consulta de status efetivo de campanhas na Graph API. */
class FacebookCampaignStatusSnapshotClientTest {
    private FailFastMockWebServer facebook;

    /** Sobe o servidor HTTP simulado da Graph API. */
    @BeforeEach
    void setUp() throws Exception {
        facebook = new FailFastMockWebServer();
        facebook.start();
    }

    /** Encerra o servidor HTTP simulado da Graph API. */
    @AfterEach
    void tearDown() throws Exception {
        facebook.shutdown();
    }

    /** Garante que o campo expandido de ad sets/anúncios é codificado na URL. */
    @Test
    void fetchEncodesExpandedStatusFields() throws Exception {
        facebook.enqueueResponse(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"cmp-1\",\"status\":\"ACTIVE\",\"effective_status\":\"ACTIVE\"}"));
        FacebookCampaignStatusSnapshotClient client = new FacebookCampaignStatusSnapshotClient(
                WebClient.builder(),
                facebook.url("/").toString(),
                "v23.0");

        client.fetch("cmp-1", "token");

        RecordedRequest request = facebook.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath())
                .contains("/v23.0/cmp-1")
                .contains("fields=status,effective_status,adsets%7Bstatus,effective_status,ads%7Bstatus,effective_status%7D%7D")
                .contains("access_token=token");
    }
}
