package com.marketinghub.leadportal.integration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.leadportal.LeadPortalFlow;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

class LeadPortalFlowPublisherTest {

    private RestTemplate restTemplate;
    private ExperimentHeroImageResolver heroImageResolver;
    private LeadPortalFlowPublisher publisher;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        heroImageResolver = mock(ExperimentHeroImageResolver.class);
        when(heroImageResolver.resolve(any())).thenReturn(Optional.empty());

        LeadPortalIntegrationProperties properties = new LeadPortalIntegrationProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://lead-portal.example.com");

        publisher = new LeadPortalFlowPublisher(restTemplate, properties, heroImageResolver);
    }

    @Test
    void shouldIgnoreManagedSimpleFlowBadRequest() {
        LeadPortalFlow flow = new LeadPortalFlow();
        flow.setSlug("formulario-simples-personal-trainer");

        HttpClientErrorException badRequest =
                HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        HttpHeaders.EMPTY,
                        """
                        {"error":"Fluxos simples são gerenciados automaticamente e não podem ser editados."}
                        """.getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8);
        org.mockito.Mockito.doThrow(badRequest).when(restTemplate).put(any(), any());

        assertThatCode(() -> publisher.publish(flow)).doesNotThrowAnyException();
    }

    @Test
    void shouldPropagateOtherBadRequestAsPublicationException() {
        LeadPortalFlow flow = new LeadPortalFlow();
        flow.setSlug("flow-abc");

        HttpClientErrorException badRequest =
                HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        HttpHeaders.EMPTY,
                        """
                        {"error":"validation failed"}
                        """.getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8);
        org.mockito.Mockito.doThrow(badRequest).when(restTemplate).put(any(), any());

        assertThatThrownBy(() -> publisher.publish(flow))
                .isInstanceOf(LeadPortalPublicationException.class)
                .hasMessageContaining("flow-abc");
    }
}
