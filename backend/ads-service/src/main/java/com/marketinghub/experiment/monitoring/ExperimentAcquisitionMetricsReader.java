package com.marketinghub.experiment.monitoring;

import com.marketinghub.experiment.Experiment;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: projetar métricas do canal pelo contexto de experimento sem vazar sua
 * implementação para outros módulos.
 */
@Component
public class ExperimentAcquisitionMetricsReader {

  /** Lê métricas e causa persistida da parada sem reconstruir diagnósticos históricos ausentes. */
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
        campaign != null,
        campaign == null || campaign.getStopReason() == null
            ? null
            : campaign.getStopReason().name(),
        campaign == null ? null : campaign.getStopRequestedAt(),
        campaign == null ? null : campaign.getStopCompletedAt(),
        campaign != null
                && campaign.getStopReason() != null
                && "AUTOMATIC_STOP".equals(experiment.getLastStatusChangeAction())
            ? experiment.getLastStatusChangeReason()
            : null);
  }
}
