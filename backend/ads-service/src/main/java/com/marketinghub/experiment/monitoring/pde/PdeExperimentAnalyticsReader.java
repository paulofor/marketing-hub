package com.marketinghub.experiment.monitoring.pde;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.repository.jdbc.experiment.ExperimentPdeAnalyticsRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.net.URI;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolve o recorte oficial do experimento antes de ler métricas PDE compartilhadas por tela e
 * Hermes.
 */
@Component
public class PdeExperimentAnalyticsReader {
  private final PdeProductionSlotRepository slots;
  private final ExperimentPdeAnalyticsRepository analytics;

  /** Conecta somente cadastros e métricas persistidos no backend. */
  public PdeExperimentAnalyticsReader(
      PdeProductionSlotRepository slots, ExperimentPdeAnalyticsRepository analytics) {
    this.slots = slots;
    this.analytics = analytics;
  }

  /** Rejeita escopo incompleto em vez de substituir por dados globais ou de outro produto. */
  @Transactional(readOnly = true)
  public PdeAnalyticsSummary read(Experiment experiment) {
    return read(experiment, null, null);
  }

  /** Lê o mesmo recorte oficial limitado à janela comercial informada pelo ciclo. */
  @Transactional(readOnly = true)
  public PdeAnalyticsSummary read(Experiment experiment, Instant periodStart, Instant periodEnd) {
    Scope scope = scope(experiment);
    var codes = analytics.attributionCodes(experiment.getId());
    return analytics.summarize(
        experiment.getId(),
        scope.productSlug(),
        scope.experienceVersion(),
        codes,
        periodStart,
        periodEnd);
  }

  /** Resolve e valida produto e versão antes de qualquer consulta analítica. */
  private Scope scope(Experiment experiment) {
    String url = experiment.getFollowUpActionUrl();
    String host = url == null ? null : URI.create(url).getHost();
    if (host == null) throw new IllegalStateException("PDE_ANALYTICS_SLOT_REQUIRED");
    PdeProductionSlot slot =
        slots
            .findFirstByDomain(host.toLowerCase(java.util.Locale.ROOT))
            .orElseThrow(() -> new IllegalStateException("PDE_ANALYTICS_SLOT_REQUIRED"));
    if (slot.getExperienceVersion() == null
        || slot.getExperienceVersion().isBlank()
        || slot.getProductSlug() == null
        || slot.getProductSlug().isBlank()) {
      throw new IllegalStateException("PDE_ANALYTICS_VERSION_REQUIRED");
    }
    if (experiment.getProduct() == null
        || !slot.getProductSlug().equals(experiment.getProduct().getSlug())) {
      throw new IllegalStateException("PDE_ANALYTICS_PRODUCT_MISMATCH");
    }
    return new Scope(slot.getProductSlug(), slot.getExperienceVersion());
  }

  /** Lê desfechos financeiros e satisfação pelo mesmo produto, versão e atribuição já validados. */
  public PdeCommercialOutcomeSummary commercialOutcomes(
      Experiment experiment, PdeAnalyticsSummary summary) {
    return commercialOutcomes(experiment, summary, null, null);
  }

  /** Lê os desfechos financeiros e de valor dentro da mesma janela usada pelo funil. */
  public PdeCommercialOutcomeSummary commercialOutcomes(
      Experiment experiment, PdeAnalyticsSummary summary, Instant periodStart, Instant periodEnd) {
    return analytics.commercialOutcomes(
        experiment.getId(),
        summary.productSlug(),
        summary.currentExperienceVersion(),
        analytics.attributionCodes(experiment.getId()),
        periodStart,
        periodEnd);
  }

  /** Lê detalhes limitados da mesma identidade já validada pelo resumo canônico. */
  public java.util.List<
          com.marketinghub.experiment.funnel.service.analytics
              .ExperimentLandingAnalyticsDetailedEventDto>
      details(Experiment experiment, PdeAnalyticsSummary summary, int limit) {
    return analytics.detailedEvents(
        experiment.getId(),
        summary.productSlug(),
        summary.currentExperienceVersion(),
        analytics.attributionCodes(experiment.getId()),
        limit);
  }

  /** Conserva a identidade já validada do slot sem expor a entidade persistente. */
  private record Scope(String productSlug, String experienceVersion) {}
}
