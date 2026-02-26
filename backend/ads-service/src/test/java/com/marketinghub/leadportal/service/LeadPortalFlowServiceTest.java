package com.marketinghub.leadportal.service;

import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublisher;
import com.marketinghub.leadportal.integration.LeadPortalPublicationException;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadPortalFlowServiceTest {

    @Mock
    private LeadPortalFlowRepository repository;

    @Mock
    private LeadPortalFlowPublisher flowPublisher;

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private MarketNicheRepository marketNicheRepository;

    @InjectMocks
    private LeadPortalFlowService service;

    private LeadPortalFlow flow;

    @BeforeEach
    void setUp() {
        flow = LeadPortalFlow.builder()
                .id(1L)
                .name("Fluxo")
                .slug("fluxo")
                .questions(List.of())
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(flow));
        when(repository.save(any(LeadPortalFlow.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void updateApprovalPublishesFlowWhenApproved() {
        service.updateApproval(1L, true);

        verify(flowPublisher).publish(flow);
        verify(flowPublisher, never()).remove(anyString());
    }

    @Test
    void updateApprovalRemovesFlowWhenRevoked() {
        flow.setApproved(true);
        service.updateApproval(1L, false);

        verify(flowPublisher).remove("fluxo");
        verify(flowPublisher, never()).publish(any());
    }

    @Test
    void updateApprovalPropagatesPublisherErrors() {
        doThrow(new LeadPortalPublicationException("fail", new RuntimeException()))
                .when(flowPublisher).publish(flow);

        assertThatThrownBy(() -> service.updateApproval(1L, true))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(Throwable::getCause)
                .isInstanceOf(LeadPortalPublicationException.class);
    }
}
