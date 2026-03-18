package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.repository.LeadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperimentFunnelServiceRenderCompleteTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private ExperimentFunnelEventRepository eventRepository;

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ExperimentFunnelService service;

    @Test
    void registerFormRenderCompletedSavesStageThreeEvent() {
        Experiment experiment = Experiment.builder().id(42L).build();
        when(experimentRepository.findFirstByLeadPortalFlowSlug("flow-slug"))
                .thenReturn(Optional.of(experiment));

        service.registerFormRenderCompleted("flow-slug", "visitor-123");

        ArgumentCaptor<ExperimentFunnelEvent> eventCaptor = ArgumentCaptor.forClass(ExperimentFunnelEvent.class);
        verify(eventRepository).save(eventCaptor.capture());

        ExperimentFunnelEvent saved = eventCaptor.getValue();
        assertEquals(experiment, saved.getExperiment());
        assertEquals(ExperimentFunnelStage.VISUALIZACAO_FORM, saved.getStage());
        assertEquals(ExperimentFunnelEventRepository.RENDER_COMPLETE_SOURCE, saved.getSource());
        assertEquals("visitorId=visitor-123", saved.getPayload());
    }

    @Test
    void registerFormRenderCompletedAllowsMissingVisitorId() {
        Experiment experiment = Experiment.builder().id(42L).build();
        when(experimentRepository.findFirstByLeadPortalFlowSlug("flow-slug"))
                .thenReturn(Optional.of(experiment));

        service.registerFormRenderCompleted("flow-slug", "   ");

        ArgumentCaptor<ExperimentFunnelEvent> eventCaptor = ArgumentCaptor.forClass(ExperimentFunnelEvent.class);
        verify(eventRepository).save(eventCaptor.capture());
        assertNull(eventCaptor.getValue().getPayload());
    }

    @Test
    void registerFormRenderCompletedFailsWhenSlugIsMissing() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registerFormRenderCompleted("   ", "visitor-123"));

        assertEquals("Slug do fluxo é obrigatório", ex.getMessage());
    }
}
