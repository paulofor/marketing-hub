package com.marketinghub.experiment.funnel;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentLandingAnalyticsEventRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

/** Comprova a fonte temporal canônica das consultas de analytics do funil. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class ExperimentFunnelAnalyticsRepositoryTest {

  @Autowired private ExperimentRepository experimentRepository;
  @Autowired private ExperimentFunnelEventRepository funnelEventRepository;
  @Autowired private ExperimentLandingAnalyticsEventRepository analyticsEventRepository;
  @Autowired private MarketNicheRepository nicheRepository;
  @Autowired private HypothesisRepository hypothesisRepository;
  @Autowired private EntityManager entityManager;

  /**
   * Garante que a consulta use os instantes normalizados do navegador mesmo quando os eventos
   * brutos compartilham o mesmo horário.
   */
  @Test
  void findsCanonicalEventsWithNormalizedOccurredAt() {
    Experiment experiment = createExperiment();
    Instant firstBrowserInstant = Instant.parse("2026-08-21T12:01:32.582Z");
    Instant secondBrowserInstant = Instant.parse("2026-08-21T12:01:43.221Z");
    Instant sharedRawInstant = Instant.parse("2026-08-21T12:01:32Z");
    ExperimentFunnelEvent firstRaw = saveRawPageView(experiment, "evento-1", sharedRawInstant);
    ExperimentFunnelEvent secondRaw = saveRawPageView(experiment, "evento-2", sharedRawInstant);
    saveNormalizedPageView(experiment, firstRaw, "evento-1", firstBrowserInstant);
    saveNormalizedPageView(experiment, secondRaw, "evento-2", secondBrowserInstant);
    entityManager.flush();
    entityManager.clear();

    var events =
        funnelEventRepository.findLandingAnalyticsEvents(
            experiment.getId(),
            ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE,
            null,
            PageRequest.of(0, 20));
    long total =
        funnelEventRepository.countLandingAnalyticsEvents(
            experiment.getId(),
            ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE,
            null);
    var visitors = analyticsEventRepository.aggregateVisitorsByExperiment(experiment.getId(), null);

    assertThat(events)
        .extracting(event -> event.getOccurredAt())
        .containsExactly(secondBrowserInstant, firstBrowserInstant);
    assertThat(total).isEqualTo(2);
    assertThat(visitors)
        .singleElement()
        .satisfies(
            visitor -> {
              assertThat(visitor.getValidPageViews()).isEqualTo(2);
              assertThat(ExperimentFunnelService.fromUtcDatabaseValue(visitor.getFirstAccessAt()))
                  .isEqualTo(firstBrowserInstant);
              assertThat(ExperimentFunnelService.fromUtcDatabaseValue(visitor.getLastAccessAt()))
                  .isEqualTo(secondBrowserInstant);
            });
  }

  /** Cria o experimento mínimo exigido pelas chaves estrangeiras do funil. */
  private Experiment createExperiment() {
    MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Nicho analytics").build());
    Hypothesis hypothesis =
        hypothesisRepository.save(
            Hypothesis.builder().marketNiche(niche).title("Hipótese analytics").build());
    return experimentRepository.save(
        Experiment.builder()
            .niche(niche)
            .hypothesisRef(hypothesis)
            .name("Experimento analytics canônico")
            .build());
  }

  /** Persiste o evento bruto com o payload necessário para reconstrução analítica. */
  private ExperimentFunnelEvent saveRawPageView(
      Experiment experiment, String eventId, Instant occurredAt) {
    return funnelEventRepository.save(
        ExperimentFunnelEvent.builder()
            .experiment(experiment)
            .stage(ExperimentFunnelStage.VISUALIZACAO_FORM)
            .source(ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE)
            .payload(
                "eventId=%s;eventType=page_view;visitorId=visitante-1;sessionId=sessao-1;pageUrl=https://example.test/oferta"
                    .formatted(eventId))
            .occurredAt(occurredAt)
            .build());
  }

  /** Persiste o evento normalizado com o instante de origem informado pelo navegador. */
  private void saveNormalizedPageView(
      Experiment experiment, ExperimentFunnelEvent rawEvent, String eventId, Instant occurredAt) {
    analyticsEventRepository.save(
        ExperimentLandingAnalyticsEvent.builder()
            .experiment(experiment)
            .funnelEvent(rawEvent)
            .eventId(eventId)
            .visitorId("visitante-1")
            .sessionId("sessao-1")
            .eventType("page_view")
            .pageUrl("https://example.test/oferta")
            .userAgent("Mozilla/5.0 Mobile")
            .trafficQuality("HUMAN")
            .trafficQualityReason("NO_AUTOMATION_SIGNAL")
            .occurredAt(occurredAt)
            .build());
  }
}
