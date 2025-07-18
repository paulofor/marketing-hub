package com.marketinghub.facebookads.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.facebookads.dto.ExperimentDTO;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

class CampaignServiceTest {
    @Test
    void createExperimentCampaignSendsBatch() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("[]"));
        server.start();
        String baseUrl = server.url("/").toString();
        CampaignService service = new CampaignService("TOKEN", "v19.0", baseUrl, "123", "SANDBOX");
        ExperimentDTO dto = new ExperimentDTO();
        dto.setName("My Campaign");
        dto.setSplitTest(true);
        JsonNode resp = service.createExperimentCampaign(dto);
        String body = server.takeRequest().getBody().readUtf8();
        assertThat(body).contains("batch=");
        server.shutdown();
    }
}
