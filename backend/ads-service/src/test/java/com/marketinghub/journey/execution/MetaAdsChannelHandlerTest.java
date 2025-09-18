package com.marketinghub.journey.execution;

import com.marketinghub.journey.execution.channel.ChannelDispatchResult;
import com.marketinghub.journey.execution.channel.ChannelDispatchStatus;
import com.marketinghub.journey.execution.channel.MetaAdsChannelHandler;
import com.marketinghub.journey.execution.config.MetaMarketingProperties;
import com.marketinghub.journey.model.JourneyAssignment;
import com.marketinghub.journey.model.JourneyAssignmentType;
import com.marketinghub.journey.model.JourneyPhase;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.model.Lead;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class MetaAdsChannelHandlerTest {
    private MetaAdsChannelHandler handler;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        MetaMarketingProperties properties = new MetaMarketingProperties();
        properties.setEnabled(true);
        properties.setAccessToken("token");
        properties.setAdAccountId("123");
        handler = new MetaAdsChannelHandler(restTemplate, properties);
    }

    @Test
    void returnsTransientFailureOnRateLimit() {
        JourneyStep step = JourneyStep.builder()
                .id(1L)
                .template(JourneyTemplate.builder().id(2L).name("template").build())
                .position(1)
                .phase(JourneyPhase.ATTENTION)
                .stimulusType(JourneyStimulusType.AD)
                .metadata(Map.of("adsetId", "456", "creativeId", "789"))
                .build();
        JourneyAssignment assignment = JourneyAssignment.builder()
                .id(10L)
                .type(JourneyAssignmentType.LEAD)
                .lead(Lead.builder().id(UUID.randomUUID()).build())
                .build();

        server.expect(once(), requestTo("https://graph.facebook.com/v18.0/act_123/ads"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", "60"));

        ChannelDispatchResult result = handler.dispatch(assignment, step, Map.of());

        assertThat(result.status()).isEqualTo(ChannelDispatchStatus.TRANSIENT_ERROR);
        assertThat(result.nextAttemptAt()).isNotNull();
    }

    @Test
    void returnsPermanentFailureWhenDisabled() {
        MetaMarketingProperties properties = new MetaMarketingProperties();
        properties.setEnabled(false);
        MetaAdsChannelHandler disabledHandler = new MetaAdsChannelHandler(new RestTemplate(), properties);
        JourneyStep step = JourneyStep.builder()
                .id(1L)
                .template(JourneyTemplate.builder().id(2L).name("template").build())
                .position(1)
                .phase(JourneyPhase.ATTENTION)
                .stimulusType(JourneyStimulusType.AD)
                .build();
        JourneyAssignment assignment = JourneyAssignment.builder()
                .id(10L)
                .type(JourneyAssignmentType.LEAD)
                .lead(Lead.builder().id(UUID.randomUUID()).build())
                .build();

        ChannelDispatchResult result = disabledHandler.dispatch(assignment, step, Map.of());

        assertThat(result.status()).isEqualTo(ChannelDispatchStatus.PERMANENT_ERROR);
    }
}
