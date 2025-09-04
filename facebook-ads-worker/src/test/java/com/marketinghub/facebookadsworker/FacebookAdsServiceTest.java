package com.marketinghub.facebookadsworker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.facebookads.FacebookAdsClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@SpringBootTest
class FacebookAdsServiceTest {

    @Autowired
    private FacebookAdsService service;

    @MockBean
    private FacebookAdsClient client;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void createCampaignDelegatesToClient() {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", "123");
        given(client.createCampaign(anyString(), anyString(), anyString())).willReturn(node);
        String id = service.createInstagramCampaign("1", "Camp");
        assertEquals("123", id);
    }

    @Test
    void metricsDelegatesToClient() {
        ObjectNode node = mapper.createObjectNode();
        given(client.getCampaignInsights(anyString())).willReturn(node);
        ObjectNode result = service.getCampaignMetrics("77");
        assertEquals(node, result);
    }
}
