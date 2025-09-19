package com.marketinghub.journey.execution;

import com.marketinghub.journey.execution.channel.ChannelDispatchResult;
import com.marketinghub.journey.execution.channel.ChannelDispatchStatus;
import com.marketinghub.journey.execution.channel.WhatsAppChannelHandler;
import com.marketinghub.journey.execution.config.WhatsAppProperties;
import com.marketinghub.journey.model.JourneyAssignment;
import com.marketinghub.journey.model.JourneyAssignmentType;
import com.marketinghub.journey.model.JourneyPhase;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.model.Lead;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class WhatsAppChannelHandlerTest {
    private WhatsAppChannelHandler handler;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        WhatsAppProperties properties = new WhatsAppProperties();
        properties.setEnabled(true);
        properties.setAccessToken("token");
        properties.setPhoneNumberId("1234");
        handler = new WhatsAppChannelHandler(new RestTemplateBuilder(), properties);
        RestTemplate restTemplate = (RestTemplate) Objects.requireNonNull(
                ReflectionTestUtils.getField(handler, "restTemplate"));
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void dispatchesTextMessage() {
        JourneyStep step = JourneyStep.builder()
                .id(1L)
                .template(JourneyTemplate.builder().id(2L).name("template").build())
                .position(1)
                .phase(JourneyPhase.INTEREST)
                .stimulusType(JourneyStimulusType.WHATSAPP)
                .metadata(Map.of("body", "Hello from MarketingHub"))
                .build();
        JourneyAssignment assignment = JourneyAssignment.builder()
                .id(10L)
                .type(JourneyAssignmentType.LEAD)
                .lead(Lead.builder().id(UUID.randomUUID()).build())
                .build();

        server.expect(once(), requestTo("https://graph.facebook.com/v18.0/1234/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withStatus(org.springframework.http.HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messages\":[{\"id\":\"wamid.123\"}]}"));

        ChannelDispatchResult result = handler.dispatch(assignment, step, Map.of("phone", "+551100000000"));

        assertThat(result.status()).isEqualTo(ChannelDispatchStatus.OK);
        assertThat(result.providerMessageId()).isEqualTo("wamid.123");
        server.verify();
    }

    @Test
    void returnsTransientFailureOnRateLimit() {
        JourneyStep step = JourneyStep.builder()
                .id(1L)
                .template(JourneyTemplate.builder().id(2L).name("template").build())
                .position(1)
                .phase(JourneyPhase.INTEREST)
                .stimulusType(JourneyStimulusType.WHATSAPP)
                .metadata(Map.of("body", "Hello"))
                .build();
        JourneyAssignment assignment = JourneyAssignment.builder()
                .id(11L)
                .type(JourneyAssignmentType.LEAD)
                .lead(Lead.builder().id(UUID.randomUUID()).build())
                .build();

        server.expect(once(), requestTo("https://graph.facebook.com/v18.0/1234/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", "120"));

        ChannelDispatchResult result = handler.dispatch(assignment, step, Map.of("phone", "+551100000001"));

        assertThat(result.status()).isEqualTo(ChannelDispatchStatus.TRANSIENT_ERROR);
        assertThat(result.nextAttemptAt()).isNotNull();
    }
}
