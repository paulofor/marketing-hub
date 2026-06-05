package com.marketinghub.experiment.funnel;

import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.core.LeadRepository;
import com.marketinghub.leadportal.dto.RegisterLandingPageAnalyticsEventRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testa o registro de eventos de renderização e analytics da landing no funil do experimento.
 */
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

    /**
     * Valida que o render-complete grava visualização do formulário com visitante e campanha.
     */
    @Test
    void registerFormRenderCompletedSavesStageThreeEvent() {
        Experiment experiment = Experiment.builder().id(42L).build();
        when(experimentRepository.findFirstByLeadPortalFlowSlug("flow-slug"))
                .thenReturn(Optional.of(experiment));

        service.registerFormRenderCompleted("flow-slug", "visitor-123", "ad-123");

        ArgumentCaptor<ExperimentFunnelEvent> eventCaptor = ArgumentCaptor.forClass(ExperimentFunnelEvent.class);
        verify(eventRepository).save(eventCaptor.capture());

        ExperimentFunnelEvent saved = eventCaptor.getValue();
        assertEquals(experiment, saved.getExperiment());
        assertEquals(ExperimentFunnelStage.VISUALIZACAO_FORM, saved.getStage());
        assertEquals(ExperimentFunnelEventRepository.RENDER_COMPLETE_SOURCE, saved.getSource());
        assertEquals("visitorId=visitor-123", saved.getPayload());
        assertEquals("ad-123", saved.getCampaignCode());
    }

    /**
     * Valida que o render-complete aceita visitante ausente sem payload.
     */
    @Test
    void registerFormRenderCompletedAllowsMissingVisitorId() {
        Experiment experiment = Experiment.builder().id(42L).build();
        when(experimentRepository.findFirstByLeadPortalFlowSlug("flow-slug"))
                .thenReturn(Optional.of(experiment));

        service.registerFormRenderCompleted("flow-slug", "   ", "   ");

        ArgumentCaptor<ExperimentFunnelEvent> eventCaptor = ArgumentCaptor.forClass(ExperimentFunnelEvent.class);
        verify(eventRepository).save(eventCaptor.capture());
        assertNull(eventCaptor.getValue().getPayload());
    }

    /**
     * Valida que analytics da landing publicada resolve o experimento pelo slug da URL final.
     */
    @Test
    void registerLandingPageAnalyticsUsesPublishedStandaloneSlugFallback() {
        Experiment experiment = Experiment.builder().id(36L).build();
        when(experimentRepository.findFirstByLeadPortalFlowSlug("exp-36-landing-geralanding"))
                .thenReturn(Optional.empty());
        when(experimentRepository.findFirstByFollowUpActionUrlFlowSlug("exp-36-landing-geralanding"))
                .thenReturn(Optional.of(experiment));

        service.registerLandingPageAnalyticsEvent("exp-36-landing-geralanding",
                new RegisterLandingPageAnalyticsEventRequest(
                        "event-1",
                        "page_view",
                        "session-1",
                        null,
                        null,
                        null,
                        "https://oportunidadebrasil.shop/api/flows/exp-36-landing-geralanding/page",
                        Instant.parse("2026-06-04T21:00:00Z"),
                        "JUnit"));

        ArgumentCaptor<ExperimentFunnelEvent> eventCaptor = ArgumentCaptor.forClass(ExperimentFunnelEvent.class);
        verify(eventRepository).save(eventCaptor.capture());

        ExperimentFunnelEvent saved = eventCaptor.getValue();
        assertEquals(experiment, saved.getExperiment());
        assertEquals(ExperimentFunnelStage.VISUALIZACAO_FORM, saved.getStage());
        assertEquals(ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE, saved.getSource());
        assertEquals(Instant.parse("2026-06-04T21:00:00Z"), saved.getOccurredAt());
    }

    /**
     * Valida que o resumo consolida page_view da landing junto com render-complete na visualização do formulário.
     */
    @Test
    void summarizeCountsLandingAnalyticsAsFormVisualization() {
        Experiment experiment = Experiment.builder().id(37L).build();
        when(experimentRepository.findById(37L)).thenReturn(Optional.of(experiment));
        when(eventRepository.aggregateManualByExperiment(37L, null)).thenReturn(List.of());

        service.summarize(37L);

        verify(jdbcTemplate).query(
                eq("""
                        SELECT COUNT(*) AS total,
                               NULL AS unique_count,
                               MAX(occurred_at) AS last_event
                        FROM experiment_funnel_event
                        WHERE experiment_id = ?
                          AND stage = 'VISUALIZACAO_FORM'
                          AND source IN (?, ?)
                          AND (? IS NULL OR occurred_at > ?)
                        """),
                any(ResultSetExtractor.class),
                eq(37L),
                eq(ExperimentFunnelEventRepository.RENDER_COMPLETE_SOURCE),
                eq(ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE),
                eq(null),
                eq(null));
    }

    /**
     * Valida que render-complete rejeita slug vazio.
     */
    @Test
    void registerFormRenderCompletedFailsWhenSlugIsMissing() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registerFormRenderCompleted("   ", "visitor-123", null));

        assertEquals("Slug do fluxo é obrigatório", ex.getMessage());
    }
}
