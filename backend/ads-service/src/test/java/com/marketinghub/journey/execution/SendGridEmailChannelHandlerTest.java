package com.marketinghub.journey.execution;

import com.marketinghub.journey.execution.channel.ChannelDispatchResult;
import com.marketinghub.journey.execution.channel.ChannelDispatchStatus;
import com.marketinghub.journey.execution.channel.SendGridEmailChannelHandler;
import com.marketinghub.journey.execution.config.SendGridProperties;
import com.marketinghub.journey.model.JourneyAssignment;
import com.marketinghub.journey.model.JourneyAssignmentType;
import com.marketinghub.journey.model.JourneyPhase;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.journey.model.JourneyStatus;
import com.marketinghub.model.Lead;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class SendGridEmailChannelHandlerTest {
    private SendGridEmailChannelHandler handler;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        SendGridProperties properties = new SendGridProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setFromEmail("hello@example.com");
        properties.setFromName("Marketing Hub");
        handler = new SendGridEmailChannelHandler(new RestTemplateBuilder(), properties);
        RestTemplate restTemplate = (RestTemplate) Objects.requireNonNull(
                ReflectionTestUtils.getField(handler, "restTemplate"));
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void dispatchesEmailWithExpectedPayload() {
        JourneyStep step = JourneyStep.builder()
                .id(1L)
                .template(JourneyTemplate.builder().id(2L).name("template").build())
                .position(1)
                .phase(JourneyPhase.ATTENTION)
                .stimulusType(JourneyStimulusType.EMAIL)
                .metadata(new HashMap<>(Map.of("templateId", "d-123", "subject", "Welcome")))
                .build();
        JourneyAssignment assignment = JourneyAssignment.builder()
                .id(10L)
                .type(JourneyAssignmentType.LEAD)
                .lead(Lead.builder().id(UUID.randomUUID()).build())
                .journey(com.marketinghub.journey.model.Journey.builder()
                        .id(5L)
                        .template(step.getTemplate())
                        .name("Journey")
                        .status(JourneyStatus.ACTIVE)
                        .startAt(Instant.now())
                        .build())
                .build();

        Map<String, Object> context = new HashMap<>();
        context.put("email", "lead@example.com");

        server.expect(once(), requestTo("https://api.sendgrid.com/v3/mail/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withStatus(org.springframework.http.HttpStatus.ACCEPTED));

        ChannelDispatchResult result = handler.dispatch(assignment, step, context);

        assertThat(result.status()).isEqualTo(ChannelDispatchStatus.OK);
        server.verify();
    }

    @Test
    void returnsPermanentErrorWhenEmailMissing() {
        JourneyStep step = JourneyStep.builder()
                .id(1L)
                .template(JourneyTemplate.builder().id(2L).name("template").build())
                .position(1)
                .phase(JourneyPhase.ATTENTION)
                .stimulusType(JourneyStimulusType.EMAIL)
                .build();
        JourneyAssignment assignment = JourneyAssignment.builder()
                .id(10L)
                .type(JourneyAssignmentType.LEAD)
                .lead(Lead.builder().id(UUID.randomUUID()).build())
                .build();

        ChannelDispatchResult result = handler.dispatch(assignment, step, Map.of());

        assertThat(result.status()).isEqualTo(ChannelDispatchStatus.PERMANENT_ERROR);
    }
}
