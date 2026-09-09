package com.marketinghub.experiment.monitoring;

import com.marketinghub.experiment.Experiment;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: projetar métricas do canal pelo contexto de experimento sem vazar sua
 * implementação para outros módulos.
 */
@Component
public class ExperimentAcquisitionMetricsReader {

  /** Lê o snapshot persistido do canal e preserva explicitamente a ausência de campanha. */
  public ExperimentAcquisitionMetricsSnapshot read(Experiment experiment) {
    var metric = experiment.getCampaignMetric();
    var campaign = metric == null ? null : metric.getCampaign();
    return new ExperimentAcquisitionMetricsSnapshot(
        experiment.getPlatform(),
        metric == null ? null : metric.getSpend(),
        campaign == null ? null : campaign.getMetricsLastSyncedAt(),
        campaign == null ? null : campaign.getMetricsFinalSyncedAt(),
        campaign == null ? null : campaign.getMetricsLastError(),
        metric == null ? null : metric.getImpressions(),
        metric == null ? null : metric.getClicks(),
        campaign != null);
  }
}
