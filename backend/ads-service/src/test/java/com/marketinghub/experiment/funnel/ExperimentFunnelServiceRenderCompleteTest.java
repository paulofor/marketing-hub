package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsDeviceDto;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentLandingAnalyticsEventRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository.LandingAnalyticsEventProjection;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentLandingAnalyticsEventRepository.VisitorRecurrenceProjection;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.core.LeadRepository;
import com.marketinghub.leadportal.dto.RegisterLandingPageAnalyticsEventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
    private ExperimentLandingAnalyticsEventRepository landingAnalyticsEventRepository;

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ExperimentFunnelService service;

    /**
     * Configura stubs comuns para permitir criação de eventos normalizados novos.
     */
    @BeforeEach
    void setUp() {
        lenient().when(landingAnalyticsEventRepository.findFirstByExperimentIdAndEventId(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(landingAnalyticsEventRepository.aggregateVisitorsByExperiment(any(), any()))
                .thenReturn(List.of());
    }

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

        when(eventRepository.save(any(ExperimentFunnelEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.registerLandingPageAnalyticsEvent("exp-36-landing-geralanding",
                new RegisterLandingPageAnalyticsEventRequest(
                        "event-1",
                        "page_view",
                        "visitor-1",
                        "session-1",
                        null,
                        null,
                        null,
                        "https://oportunidadebrasil.shop/api/flows/exp-36-landing-geralanding/page",
                        Instant.parse("2026-06-04T21:00:00Z"),
                        "JUnit",
                        "desktop",
                        "other",
                        1366,
                        768));

        ArgumentCaptor<ExperimentFunnelEvent> eventCaptor = ArgumentCaptor.forClass(ExperimentFunnelEvent.class);
        verify(eventRepository).save(eventCaptor.capture());

        ExperimentFunnelEvent saved = eventCaptor.getValue();
        assertEquals(experiment, saved.getExperiment());
        assertEquals(ExperimentFunnelStage.VISUALIZACAO_FORM, saved.getStage());
        assertEquals(ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE, saved.getSource());
        assertEquals(Instant.parse("2026-06-04T21:00:00Z"), saved.getOccurredAt());
    }



    /**
     * Valida que analytics com visitorId persiste o evento legado e a estrutura normalizada consultável.
     */
    @Test
    void registerLandingPageAnalyticsPersistsNormalizedVisitorEvent() {
        Experiment experiment = Experiment.builder().id(40L).build();
        when(experimentRepository.findFirstByLeadPortalFlowSlug("flow-slug"))
                .thenReturn(Optional.of(experiment));
        when(eventRepository.save(any(ExperimentFunnelEvent.class))).thenAnswer(invocation -> {
            ExperimentFunnelEvent event = invocation.getArgument(0);
            event.setId(700L);
            return event;
        });

        service.registerLandingPageAnalyticsEvent("flow-slug",
                new RegisterLandingPageAnalyticsEventRequest(
                        "event-normalized",
                        "page_view",
                        "visitor-123",
                        "session-123",
                        null,
                        null,
                        null,
                        "https://oportunidadebrasil.shop/page",
                        Instant.parse("2026-06-07T10:00:00Z"),
                        "JUnit Browser",
                        "desktop",
                        "other",
                        null,
                        null));

        ArgumentCaptor<ExperimentLandingAnalyticsEvent> normalizedCaptor =
                ArgumentCaptor.forClass(ExperimentLandingAnalyticsEvent.class);
        verify(landingAnalyticsEventRepository).save(normalizedCaptor.capture());

        ExperimentLandingAnalyticsEvent normalized = normalizedCaptor.getValue();
        assertEquals(experiment, normalized.getExperiment());
        assertEquals("event-normalized", normalized.getEventId());
        assertEquals("visitor-123", normalized.getVisitorId());
        assertEquals("session-123", normalized.getSessionId());
        assertEquals("page_view", normalized.getEventType());
        assertEquals("https://oportunidadebrasil.shop/page", normalized.getPageUrl());
        assertEquals(Instant.parse("2026-06-07T10:00:00Z"), normalized.getOccurredAt());
        assertEquals(700L, normalized.getFunnelEvent().getId());
    }

    /**
     * Valida que eventos legados sem visitorId continuam aceitos e normalizados sem afirmar recorrência provável.
     */
    @Test
    void registerLandingPageAnalyticsAllowsLegacyEventWithoutVisitorId() {
        Experiment experiment = Experiment.builder().id(41L).build();
        when(experimentRepository.findFirstByLeadPortalFlowSlug("flow-slug"))
                .thenReturn(Optional.of(experiment));
        when(eventRepository.save(any(ExperimentFunnelEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.registerLandingPageAnalyticsEvent("flow-slug",
                new RegisterLandingPageAnalyticsEventRequest(
                        "legacy-event",
                        "page_view",
                        null,
                        "legacy-session",
                        null,
                        null,
                        null,
                        "https://oportunidadebrasil.shop/page",
                        Instant.parse("2026-06-07T10:01:00Z"),
                        "JUnit Browser",
                        "desktop",
                        "other",
                        null,
                        null));

        ArgumentCaptor<ExperimentLandingAnalyticsEvent> normalizedCaptor =
                ArgumentCaptor.forClass(ExperimentLandingAnalyticsEvent.class);
        verify(landingAnalyticsEventRepository).save(normalizedCaptor.capture());
        assertNull(normalizedCaptor.getValue().getVisitorId());
        assertEquals("legacy-session", normalizedCaptor.getValue().getSessionId());
    }

    /**
     * Valida que page_view duplicado na janela canônica preserva legado e não duplica evento normalizado.
     */
    @Test
    void registerLandingPageAnalyticsDeduplicatesPageViewInCanonicalWindow() {
        Experiment experiment = Experiment.builder().id(42L).build();
        when(experimentRepository.findFirstByLeadPortalFlowSlug("flow-slug"))
                .thenReturn(Optional.of(experiment));
        when(eventRepository.save(any(ExperimentFunnelEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(landingAnalyticsEventRepository.existsPageViewInDeduplicationWindow(
                eq(42L),
                eq("visitor-duplicate"),
                eq("session-duplicate"),
                eq("page_view"),
                eq("https://oportunidadebrasil.shop/page"),
                eq(Instant.parse("2026-06-07T10:01:57Z")),
                eq(Instant.parse("2026-06-07T10:02:03Z"))))
                .thenReturn(true);

        service.registerLandingPageAnalyticsEvent("flow-slug",
                new RegisterLandingPageAnalyticsEventRequest(
                        "duplicate-event",
                        "page_view",
                        "visitor-duplicate",
                        "session-duplicate",
                        null,
                        null,
                        null,
                        "https://oportunidadebrasil.shop/page",
                        Instant.parse("2026-06-07T10:02:00Z"),
                        "JUnit Browser",
                        "desktop",
                        "other",
                        null,
                        null));

        verify(eventRepository).save(any(ExperimentFunnelEvent.class));
        verify(landingAnalyticsEventRepository).existsPageViewInDeduplicationWindow(
                eq(42L),
                eq("visitor-duplicate"),
                eq("session-duplicate"),
                eq("page_view"),
                eq("https://oportunidadebrasil.shop/page"),
                eq(Instant.parse("2026-06-07T10:01:57Z")),
                eq(Instant.parse("2026-06-07T10:02:03Z")));
        verify(landingAnalyticsEventRepository, never()).save(any(ExperimentLandingAnalyticsEvent.class));
    }

    /**
     * Valida que o resumo de analytics retorna contadores zerados quando ainda não há sessão capturada.
     */
    @Test
    void summarizeLandingAnalyticsReturnsEmptySummaryWhenThereAreNoEvents() {
        Experiment experiment = Experiment.builder().id(38L).build();
        when(experimentRepository.findById(38L)).thenReturn(Optional.of(experiment));
        when(eventRepository.findLandingAnalyticsEvents(
                eq(38L),
                eq(ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE),
                eq(null),
                any(Pageable.class)))
                .thenReturn(List.of());

        var summary = service.summarizeLandingAnalytics(38L);

        assertNotNull(summary);
        assertEquals(0, summary.totalEvents());
        assertEquals(0, summary.totalSessions());
        assertEquals(0, summary.pageViews());
        assertEquals(0, summary.sectionViewEvents());
        assertEquals(3, summary.deviceBreakdown().size());
        assertTrue(summary.deviceBreakdown().stream().allMatch(device -> device.percentage() == 0));
        assertEquals(3, summary.mobileOperatingSystemBreakdown().size());
        assertTrue(summary.mobileOperatingSystemBreakdown().stream().allMatch(os -> os.percentage() == 0));
        assertTrue(summary.screenSizeBreakdown().isEmpty());
        assertEquals(0, summary.loadMetrics().events());
        assertEquals("INSUFFICIENT_DATA", summary.loadMetrics().diagnosisCode());
    }

    /**
     * Valida que o resumo de analytics calcula percentuais de sessões por dispositivo.
     */
    @Test
    void summarizeLandingAnalyticsCalculatesDevicePercentages() {
        Experiment experiment = Experiment.builder().id(39L).build();
        when(experimentRepository.findById(39L)).thenReturn(Optional.of(experiment));
        when(eventRepository.findLandingAnalyticsEvents(
                eq(39L),
                eq(ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE),
                eq(null),
                any(Pageable.class)))
                .thenReturn(List.of(
                        landingEvent(1L, "eventId=e1;eventType=page_view;sessionId=s1;deviceType=mobile;operatingSystem=ios;screenWidth=390;screenHeight=844;userAgent=iPhone",
                                Instant.parse("2026-06-04T21:00:00Z")),
                        landingEvent(2L, "eventId=e2;eventType=page_view;sessionId=s2;deviceType=desktop;operatingSystem=other;screenWidth=1366;screenHeight=768;userAgent=Desktop",
                                Instant.parse("2026-06-04T21:01:00Z")),
                        landingEvent(3L, "eventId=e3;eventType=page_view;sessionId=s3;deviceType=tablet;operatingSystem=ios;screenWidth=820;screenHeight=1180;userAgent=iPad",
                                Instant.parse("2026-06-04T21:02:00Z")),
                        landingEvent(4L, "eventId=e4;eventType=section_view_time;sessionId=s1;elapsedMs=1500;deviceType=mobile;operatingSystem=ios;screenWidth=390;screenHeight=844",
                                Instant.parse("2026-06-04T21:03:00Z")),
                        landingEvent(5L, "eventId=e5;eventType=page_load_metric;sessionId=s1;loadDurationMs=2400;domContentLoadedMs=900;firstContentfulPaintMs=700;resourceErrorCount=2;connectionType=4g",
                                Instant.parse("2026-06-04T21:04:00Z"))));

        var summary = service.summarizeLandingAnalytics(39L);

        assertEquals(3, summary.totalSessions());
        assertEquals(5, summary.totalEvents());
        assertEquals(33.33, findDevicePercentage(summary.deviceBreakdown(), "mobile"));
        assertEquals(33.33, findDevicePercentage(summary.deviceBreakdown(), "desktop"));
        assertEquals(33.33, findDevicePercentage(summary.deviceBreakdown(), "tablet"));
        var mobileSession = summary.sessions().stream()
                .filter(session -> "s1".equals(session.sessionId()))
                .findFirst()
                .orElseThrow();
        assertEquals("Mobile", mobileSession.deviceLabel());
        assertEquals("iOS", mobileSession.operatingSystemLabel());
        assertEquals("390x844 px", mobileSession.screenSizeLabel());
        assertEquals(100.0, findOperatingSystemPercentage(summary.mobileOperatingSystemBreakdown(), "ios"));
        assertEquals(33.33, findScreenSizePercentage(summary.screenSizeBreakdown(), "390x844"));
        assertEquals(1, summary.loadMetrics().events());
        assertEquals(2400, summary.loadMetrics().averageLoadDurationMs());
        assertEquals(2400, summary.loadMetrics().p95LoadDurationMs());
        assertEquals(900, summary.loadMetrics().averageDomContentLoadedMs());
        assertEquals(700, summary.loadMetrics().averageFirstContentfulPaintMs());
        assertEquals(2, summary.loadMetrics().totalResourceErrors());
        assertEquals(2, summary.loadMetrics().sessionsWithoutSectionEvents());
        assertEquals(33.33, summary.loadMetrics().initialEngagementRate());
        assertEquals("RESOURCE_ERRORS", summary.loadMetrics().diagnosisCode());
        assertEquals("danger", summary.loadMetrics().diagnosisSeverity());
    }


    /**
     * Valida que visitantes com sessões diferentes são marcados como recorrentes prováveis.
     */
    @Test
    void summarizeLandingAnalyticsVisitorsMarksVisitorWithMultipleSessionsAsRecurrent() {
        Experiment experiment = Experiment.builder().id(45L).build();
        when(experimentRepository.findById(45L)).thenReturn(Optional.of(experiment));
        when(landingAnalyticsEventRepository.aggregateVisitorsByExperiment(45L, null))
                .thenReturn(List.of(visitorProjection(
                        "visitor-recurrent-123",
                        2,
                        2,
                        Instant.parse("2026-06-07T10:00:00Z"),
                        Instant.parse("2026-06-07T11:00:00Z"),
                        2,
                        "Mozilla/5.0 (iPhone)")));

        var summary = service.summarizeLandingAnalyticsVisitors(45L);

        assertEquals(1, summary.probableVisitors());
        assertEquals(1, summary.recurrentVisitors());
        assertEquals(0, summary.singleVisitVisitors());
        var visitor = summary.visitors().get(0);
        assertEquals("visi…-123", visitor.visitorId());
        assertEquals(2, visitor.totalSessions());
        assertEquals(2, visitor.validPageViews());
        assertEquals(3600, visitor.intervalSeconds());
        assertEquals(2, visitor.distinctPages());
        assertEquals("mobile", visitor.deviceType());
        assertTrue(visitor.recurrent());
    }
    /**
     * Valida que o diagnóstico diferencia baixa qualidade de tráfego quando carregamento está saudável.
     */
    @Test
    void summarizeLandingAnalyticsDiagnosesPossibleTrafficQuality() {
        Experiment experiment = Experiment.builder().id(55L).build();
        when(experimentRepository.findById(55L)).thenReturn(Optional.of(experiment));
        when(eventRepository.findLandingAnalyticsEvents(
                eq(55L),
                eq(ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE),
                eq(null),
                any(Pageable.class)))
                .thenReturn(List.of(
                        landingEvent(1L, "eventId=e1;eventType=page_view;sessionId=s1;deviceType=mobile;userAgent=Instagram",
                                Instant.parse("2026-06-04T21:00:00Z")),
                        landingEvent(2L, "eventId=e2;eventType=page_view;sessionId=s2;deviceType=mobile;userAgent=Instagram",
                                Instant.parse("2026-06-04T21:01:00Z")),
                        landingEvent(3L, "eventId=e3;eventType=page_view;sessionId=s3;deviceType=mobile;userAgent=Chrome",
                                Instant.parse("2026-06-04T21:02:00Z")),
                        landingEvent(4L, "eventId=e4;eventType=page_view;sessionId=s4;deviceType=mobile;userAgent=Chrome",
                                Instant.parse("2026-06-04T21:03:00Z")),
                        landingEvent(5L, "eventId=e5;eventType=page_view;sessionId=s5;deviceType=mobile;userAgent=Chrome",
                                Instant.parse("2026-06-04T21:04:00Z")),
                        landingEvent(6L, "eventId=e6;eventType=page_load_metric;sessionId=s1;loadDurationMs=1200;domContentLoadedMs=600;firstContentfulPaintMs=500;resourceErrorCount=0",
                                Instant.parse("2026-06-04T21:05:00Z"))));

        var summary = service.summarizeLandingAnalytics(55L);

        assertEquals(5, summary.totalSessions());
        assertEquals(5, summary.loadMetrics().sessionsWithoutSectionEvents());
        assertEquals(0.0, summary.loadMetrics().initialEngagementRate());
        assertEquals(2, summary.loadMetrics().inAppBrowserSessions());
        assertEquals(40.0, summary.loadMetrics().inAppBrowserPercentage());
        assertEquals("POSSIBLE_TRAFFIC_QUALITY", summary.loadMetrics().diagnosisCode());
        assertEquals("warning", summary.loadMetrics().diagnosisSeverity());
    }

    /**
     * Valida que visitante com uma sessão e um page_view permanece classificado como único.
     */
    @Test
    void summarizeLandingAnalyticsVisitorsKeepsSingleVisitorAsNotRecurrent() {
        Experiment experiment = Experiment.builder().id(46L).build();
        when(experimentRepository.findById(46L)).thenReturn(Optional.of(experiment));
        when(landingAnalyticsEventRepository.aggregateVisitorsByExperiment(46L, null))
                .thenReturn(List.of(visitorProjection(
                        "visitor-single-456",
                        1,
                        1,
                        Instant.parse("2026-06-07T10:00:00Z"),
                        Instant.parse("2026-06-07T10:00:00Z"),
                        1,
                        "Mozilla/5.0 (X11; Linux x86_64)")));

        var summary = service.summarizeLandingAnalyticsVisitors(46L);

        assertEquals(1, summary.probableVisitors());
        assertEquals(0, summary.recurrentVisitors());
        assertEquals(1, summary.singleVisitVisitors());
        assertEquals("visi…-456", summary.visitors().get(0).visitorId());
        assertEquals(0, summary.visitors().get(0).intervalSeconds());
        assertTrue(!summary.visitors().get(0).recurrent());
    }

    /**
     * Valida que eventos legados sem visitorId não aparecem na lista de visitantes prováveis.
     */
    @Test
    void summarizeLandingAnalyticsVisitorsIgnoresLegacyEventsWithoutVisitorId() {
        Experiment experiment = Experiment.builder().id(47L).build();
        when(experimentRepository.findById(47L)).thenReturn(Optional.of(experiment));
        when(landingAnalyticsEventRepository.aggregateVisitorsByExperiment(47L, null)).thenReturn(List.of());

        var summary = service.summarizeLandingAnalyticsVisitors(47L);

        assertEquals(0, summary.probableVisitors());
        assertEquals(0, summary.recurrentVisitors());
        assertTrue(summary.visitors().isEmpty());
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
                          AND (
                              source = ?
                              OR (
                                  source = ?
                                  AND payload LIKE '%eventType=page_view%'
                              )
                          )
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
     * Valida que o resumo não transforma eventos técnicos de analytics em novas visualizações do formulário.
     */
    @Test
    void summarizeFiltersLandingAnalyticsFormVisualizationToPageViewOnly() {
        Experiment experiment = Experiment.builder().id(41L).build();
        when(experimentRepository.findById(41L)).thenReturn(Optional.of(experiment));
        when(eventRepository.aggregateManualByExperiment(41L, null)).thenReturn(List.of());

        service.summarize(41L);

        verify(jdbcTemplate).query(
                eq("""
                        SELECT COUNT(*) AS total,
                               NULL AS unique_count,
                               MAX(occurred_at) AS last_event
                        FROM experiment_funnel_event
                        WHERE experiment_id = ?
                          AND stage = 'VISUALIZACAO_FORM'
                          AND (
                              source = ?
                              OR (
                                  source = ?
                                  AND payload LIKE '%eventType=page_view%'
                              )
                          )
                          AND (? IS NULL OR occurred_at > ?)
                        """),
                any(ResultSetExtractor.class),
                eq(41L),
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

    /**
     * Cria uma projeção mínima de evento de analytics para testes de consolidação.
     */
    private LandingAnalyticsEventProjection landingEvent(Long id, String payload, Instant occurredAt) {
        return new LandingAnalyticsEventProjection() {
            /**
             * Retorna o identificador sintético do evento de teste.
             */
            @Override
            public Long getId() {
                return id;
            }

            /**
             * Retorna o payload textual sintético do evento de teste.
             */
            @Override
            public String getPayload() {
                return payload;
            }

            /**
             * Retorna o instante sintético do evento de teste.
             */
            @Override
            public Instant getOccurredAt() {
                return occurredAt;
            }
        };
    }


    /**
     * Cria uma projeção agregada de visitante provável para testes de recorrência.
     */
    private VisitorRecurrenceProjection visitorProjection(String visitorId,
                                                          long totalSessions,
                                                          long validPageViews,
                                                          Instant firstAccessAt,
                                                          Instant lastAccessAt,
                                                          long distinctPages,
                                                          String lastUserAgent) {
        return new VisitorRecurrenceProjection() {
            /**
             * Retorna o visitorId sintético do teste.
             */
            @Override
            public String getVisitorId() {
                return visitorId;
            }

            /**
             * Retorna o total sintético de sessões do teste.
             */
            @Override
            public long getTotalSessions() {
                return totalSessions;
            }

            /**
             * Retorna o total sintético de page_views válidos do teste.
             */
            @Override
            public long getValidPageViews() {
                return validPageViews;
            }

            /**
             * Retorna o primeiro acesso sintético do teste.
             */
            @Override
            public Instant getFirstAccessAt() {
                return firstAccessAt;
            }

            /**
             * Retorna o último acesso sintético do teste.
             */
            @Override
            public Instant getLastAccessAt() {
                return lastAccessAt;
            }

            /**
             * Retorna a quantidade sintética de páginas distintas do teste.
             */
            @Override
            public long getDistinctPages() {
                return distinctPages;
            }

            /**
             * Retorna o user-agent sintético do teste.
             */
            @Override
            public String getLastUserAgent() {
                return lastUserAgent;
            }
        };
    }

    /**
     * Localiza o percentual de um sistema operacional mobile no resumo de analytics.
     */
    private double findOperatingSystemPercentage(
            List<com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsOperatingSystemDto> systems,
            String operatingSystem) {
        return systems.stream()
                .filter(system -> operatingSystem.equals(system.operatingSystem()))
                .map(com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsOperatingSystemDto::percentage)
                .findFirst()
                .orElseThrow();
    }

    /**
     * Localiza o percentual de uma resolução de tela no resumo de analytics.
     */
    private double findScreenSizePercentage(
            List<com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsScreenSizeDto> screens,
            String screenSize) {
        return screens.stream()
                .filter(screen -> screenSize.equals(screen.screenSize()))
                .map(com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsScreenSizeDto::percentage)
                .findFirst()
                .orElseThrow();
    }

    /**
     * Localiza o percentual de um dispositivo no resumo retornado pelo serviço.
     */
    private double findDevicePercentage(
            List<ExperimentLandingAnalyticsDeviceDto> devices,
            String deviceType) {
        return devices.stream()
                .filter(device -> deviceType.equals(device.deviceType()))
                .findFirst()
                .orElseThrow()
                .percentage();
    }

}
