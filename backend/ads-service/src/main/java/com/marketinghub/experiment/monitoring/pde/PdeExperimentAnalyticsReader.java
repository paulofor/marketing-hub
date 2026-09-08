package com.marketinghub.experiment.monitoring.pde;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.repository.jdbc.experiment.ExperimentPdeAnalyticsRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.net.URI;
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
    var codes = analytics.attributionCodes(experiment.getId());
    return analytics.summarize(
        experiment.getId(), slot.getProductSlug(), slot.getExperienceVersion(), codes);
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
}
