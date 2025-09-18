package com.marketinghub.journey.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.journey.execution.config.Ga4Properties;
import com.marketinghub.journey.execution.config.MetaMarketingProperties;
import com.marketinghub.journey.model.Journey;
import com.marketinghub.journey.model.JourneyAssignment;
import com.marketinghub.journey.model.JourneyAssignmentType;
import com.marketinghub.journey.model.JourneyPhase;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.model.Lead;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class TelemetryServiceTest {

    @Test
    void sendsPixelEventWhenEnabled() {
        RestTemplate metaTemplate = new RestTemplate();
        RestTemplate gaTemplate = new RestTemplate();
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(metaTemplate, gaTemplate);

        MetaMarketingProperties metaProperties = new MetaMarketingProperties();
        metaProperties.setEnabled(true);
        metaProperties.setPixelEnabled(true);
        metaProperties.setPixelId("pixel123");
        metaProperties.setAccessToken("pixel-token");

        Ga4Properties ga4Properties = new Ga4Properties();

        TelemetryService telemetryService = new TelemetryService(builder, metaProperties, ga4Properties, new ObjectMapper());

        MockRestServiceServer metaServer = MockRestServiceServer.createServer(metaTemplate);
        MockRestServiceServer gaServer = MockRestServiceServer.createServer(gaTemplate);

        metaServer.expect(once(), requestTo("https://graph.facebook.com/v18.0/pixel123/events"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer pixel-token"))
                .andExpect(content().string(containsString("\"event_name\":\"Lead\"")))
                .andExpect(content().string(containsString("\"journey_id\":1")))
                .andRespond(withStatus(HttpStatus.OK));

        JourneyTemplate template = JourneyTemplate.builder()
                .id(9L)
                .name("Template")
                .build();
        Journey journey = Journey.builder()
                .id(1L)
                .template(template)
                .name("Journey")
                .build();
        JourneyStep step = JourneyStep.builder()
                .id(2L)
                .template(template)
                .position(1)
                .phase(JourneyPhase.ATTENTION)
                .stimulusType(JourneyStimulusType.EMAIL)
                .metadata(new HashMap<>())
                .build();
        JourneyAssignment assignment = JourneyAssignment.builder()
                .id(3L)
                .journey(journey)
                .type(JourneyAssignmentType.LEAD)
                .lead(Lead.builder().id(UUID.randomUUID()).build())
                .build();

        telemetryService.emitStepDispatched(assignment, step, Map.of("source", "landing"), Map.of("providerMessageId", "abc123"));

        metaServer.verify();
        gaServer.verify();
    }

    @Test
    void sendsGa4EventWhenEnabled() {
        RestTemplate metaTemplate = new RestTemplate();
        RestTemplate gaTemplate = new RestTemplate();
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(metaTemplate, gaTemplate);

        MetaMarketingProperties metaProperties = new MetaMarketingProperties();
        metaProperties.setPixelEnabled(false);

        Ga4Properties ga4Properties = new Ga4Properties();
        ga4Properties.setEnabled(true);
        ga4Properties.setMeasurementId("G-XYZ");
        ga4Properties.setApiSecret("secret");

        TelemetryService telemetryService = new TelemetryService(builder, metaProperties, ga4Properties, new ObjectMapper());

        MockRestServiceServer metaServer = MockRestServiceServer.createServer(metaTemplate);
        MockRestServiceServer gaServer = MockRestServiceServer.createServer(gaTemplate);

        UUID leadId = UUID.randomUUID();

        gaServer.expect(once(), requestTo("https://www.google-analytics.com/mp/collect?measurement_id=G-XYZ&api_secret=secret"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"name\":\"email_sent\"")))
                .andExpect(content().string(containsString("\"journey_id\":1")))
                .andExpect(content().string(containsString("\"client_id\":\"" + leadId + "\"")))
                .andRespond(withStatus(HttpStatus.OK));

        JourneyTemplate template = JourneyTemplate.builder()
                .id(9L)
                .name("Template")
                .build();
        Journey journey = Journey.builder()
                .id(1L)
                .template(template)
                .name("Journey")
                .build();
        JourneyStep step = JourneyStep.builder()
                .id(2L)
                .template(template)
                .position(1)
                .phase(JourneyPhase.INTEREST)
                .stimulusType(JourneyStimulusType.EMAIL)
                .metadata(new HashMap<>())
                .build();
        JourneyAssignment assignment = JourneyAssignment.builder()
                .id(3L)
                .journey(journey)
                .type(JourneyAssignmentType.LEAD)
                .lead(Lead.builder().id(leadId).build())
                .build();

        Map<String, Object> dispatchMetadata = Map.of(
                "providerMessageId", "abc123",
                "value", BigDecimal.ONE
        );

        telemetryService.emitStepDispatched(assignment, step, Map.of("source", "landing"), dispatchMetadata);

        gaServer.verify();
        metaServer.verify();
    }
}

