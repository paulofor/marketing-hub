package com.marketinghub.leadportal.integration;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeadPortalFlowRepublisherTest {

    private LeadPortalFlowRepository repository;
    private LeadPortalFlowPublisher publisher;
    private LeadPortalIntegrationProperties properties;
    private LeadPortalFlowRepublisher republisher;

    @BeforeEach
    void setUp() {
        repository = mock(LeadPortalFlowRepository.class);
        publisher = mock(LeadPortalFlowPublisher.class);
        properties = new LeadPortalIntegrationProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://example.com");
        republisher = new LeadPortalFlowRepublisher(repository, publisher, properties);
    }

    @Test
    void republishApprovedFlowsWhenIntegrationEnabled() {
        LeadPortalFlow flow = new LeadPortalFlow();
        flow.setSlug("flow-1");
        when(repository.findAllByApprovedTrue()).thenReturn(List.of(flow));

        republisher.republishApprovedFlows();

        verify(publisher).publish(flow);
    }

    @Test
    void skipRepublishWhenIntegrationDisabled() {
        properties.setEnabled(false);

        republisher.republishApprovedFlows();

        verifyNoInteractions(repository, publisher);
    }

    @Test
    void continueProcessingWhenPublishFails() {
        LeadPortalFlow flow = new LeadPortalFlow();
        flow.setSlug("broken-flow");
        when(repository.findAllByApprovedTrue()).thenReturn(List.of(flow));
        doThrow(new LeadPortalPublicationException("fail"))
                .when(publisher)
                .publish(flow);

        republisher.republishApprovedFlows();

        verify(publisher).publish(flow);
    }
}
