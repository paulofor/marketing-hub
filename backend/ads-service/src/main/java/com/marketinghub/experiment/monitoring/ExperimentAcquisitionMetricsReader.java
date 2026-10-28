package com.marketinghub.experiment.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.repository.jdbc.chatgptads.ChatgptAdsRepository;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: projetar métricas do canal pelo contexto de experimento sem vazar sua
 * implementação para outros módulos.
 */
@Component
@Slf4j
public class ExperimentAcquisitionMetricsReader {
  private final ChatgptAdsRepository chatgptAds;
  private final ObjectMapper json;

  /** Recebe a fonte persistida do novo canal, preservando a projeção histórica da Meta. */
  public ExperimentAcquisitionMetricsReader(ChatgptAdsRepository chatgptAds, ObjectMapper json) {
    this.chatgptAds = chatgptAds;
    this.json = json;
  }

  /** Lê métricas e causa persistida da parada sem reconstruir diagnósticos históricos ausentes. */
  public ExperimentAcquisitionMetricsSnapshot read(Experiment experiment) {
    if (experiment.getPlatform() == ExperimentPlatform.CHATGPT_ADS) return readChatgpt(experiment);
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

  /** Projeta apenas o experimento exato e não converte moeda, ausência ou erro em custo zero. */
  private ExperimentAcquisitionMetricsSnapshot readChatgpt(Experiment experiment) {
    try {
      var campaigns = chatgptAds.list(experiment.getId());
      if (campaigns.size() != 1) return unavailable("Campanha ChatGPT Ads ainda não conciliada.");
      var c = campaigns.getFirst();
      JsonNode m = json.readTree(c.metrics() == null ? "{}" : c.metrics());
      JsonNode remote = json.readTree(c.remote());
      String error = remote.path("errorCode").asText(null);
      boolean brl = "BRL".equals(m.path("currency").asText());
      boolean complete =
          m.path("spend").isNumber()
              && m.path("spend").decimalValue().signum() >= 0
              && m.path("impressions").isIntegralNumber()
              && m.path("impressions").canConvertToLong()
              && m.path("impressions").asLong() >= 0
              && m.path("clicks").isIntegralNumber()
              && m.path("clicks").canConvertToLong()
              && m.path("clicks").asLong() >= 0;
      if (!complete) error = "A fotografia de gasto e exposição do ChatGPT Ads está incompleta.";
      else if (!brl)
        error =
            "Gasto em "
                + m.path("currency").asText()
                + ": concilie a conversão para BRL antes de calcular contribuição.";
      Instant observed = timestamp(m, "observedAt");
      Instant stopped = timestamp(remote, "stopCompletedAt");
      Instant finalAt = timestamp(m, "finalSyncedAt");
      if (!c.status().equals("PAUSED")
          || stopped == null
          || observed == null
          || finalAt == null
          || finalAt.isAfter(observed)
          || !m.path("coverageEnd").canConvertToLong()
          || Instant.ofEpochSecond(m.path("coverageEnd").asLong()).isBefore(stopped))
        finalAt = null;
      return new ExperimentAcquisitionMetricsSnapshot(
          ExperimentPlatform.CHATGPT_ADS,
          complete && brl && error == null ? m.path("spend").decimalValue() : null,
          observed,
          finalAt,
          error,
          complete ? m.path("impressions").asLong() : null,
          complete ? m.path("clicks").asLong() : null,
          remote.hasNonNull("campaignId"),
          remote.path("stopReason").asText(null),
          timestamp(remote, "stopRequestedAt"),
          stopped,
          remote.path("stopExplanation").asText(null));
    } catch (Exception ex) {
      log.error(
          "Aquisição ChatGPT Ads: fonte indisponível experimentId={}", experiment.getId(), ex);
      return unavailable("A fonte ChatGPT Ads está indisponível; nenhum custo foi presumido.");
    }
  }

  /** Preserva datas ausentes como desconhecidas e recusa datas malformadas. */
  private Instant timestamp(JsonNode node, String key) {
    return node.hasNonNull(key) ? Instant.parse(node.path(key).asText()) : null;
  }

  /** Expõe falha de leitura sem fabricar métricas ou sincronização final. */
  private ExperimentAcquisitionMetricsSnapshot unavailable(String reason) {
    return new ExperimentAcquisitionMetricsSnapshot(
        ExperimentPlatform.CHATGPT_ADS, null, null, null, reason, null, null, false);
  }
}
