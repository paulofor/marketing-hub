package com.marketinghub.experiment.service;

import com.marketinghub.cost.CostAttributionService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.run.service.ExperimentRunMetricLifecycleService;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mantém as métricas agregadas de campanha e propaga o custo de mídia para o experimento. */
@Service
public class ExperimentCampaignMetricService {
  private static final Logger log = LoggerFactory.getLogger(ExperimentCampaignMetricService.class);

  private final ExperimentCampaignMetricRepository repository;
  private final FacebookAdsCampaignRepository campaignRepository;
  private final CostAttributionService costAttributionService;
  private final ExperimentRunMetricLifecycleService runMetricLifecycleService;

  /** Inicializa o serviço com repositórios de campanha, métricas e atribuição de custo. */
  public ExperimentCampaignMetricService(
      ExperimentCampaignMetricRepository repository,
      FacebookAdsCampaignRepository campaignRepository,
      CostAttributionService costAttributionService,
      ExperimentRunMetricLifecycleService runMetricLifecycleService) {
    this.repository = repository;
    this.campaignRepository = campaignRepository;
    this.costAttributionService = costAttributionService;
    this.runMetricLifecycleService = runMetricLifecycleService;
  }

  /** Cria ou atualiza as métricas sincronizadas da campanha Facebook de um experimento. */
  @Transactional
  public ExperimentCampaignMetric upsert(
      String campaignId,
      LocalDate dateStart,
      LocalDate dateStop,
      Long reach,
      Long impressions,
      Long clicks,
      Long leads,
      BigDecimal spend) {
    FacebookAdsCampaign campaign =
        campaignRepository
            .findById(campaignId)
            .orElseThrow(
                () -> new IllegalArgumentException("Facebook campaign not found: " + campaignId));
    Experiment experiment = campaign.getExperiment();
    BigDecimal normalizedSpend = spend != null ? spend.setScale(2, RoundingMode.HALF_UP) : null;
    ExperimentCampaignMetric metric =
        repository
            .findByExperiment(experiment)
            .orElseGet(() -> ExperimentCampaignMetric.builder().experiment(experiment).build());
    Long previousImpressions = metric.getImpressions();
    BigDecimal previousSpend = metric.getSpend();
    logCommercialStart(experiment, previousImpressions, impressions);
    metric.setCampaign(campaign);
    metric.setDateStart(dateStart);
    metric.setDateStop(dateStop);
    metric.setReach(reach);
    metric.setImpressions(impressions);
    metric.setClicks(clicks);
    metric.setLeads(leads);
    metric.setSpend(normalizedSpend);
    metric.setCpc(calculateCpc(normalizedSpend, clicks));
    metric.setCpl(calculateCpl(normalizedSpend, leads));
    ExperimentCampaignMetric saved = repository.save(metric);
    applySpendDelta(experiment, normalizedSpend, previousSpend);
    runMetricLifecycleService.synchronize(experiment, campaign, impressions);
    return saved;
  }

  /** Registra o primeiro lote real sem apagar eventos auditáveis nem bloquear a métrica oficial. */
  private void logCommercialStart(
      Experiment experiment, Long previousImpressions, Long newImpressions) {
    if (experiment == null || experiment.getId() == null) {
      return;
    }
    long previous = previousImpressions == null ? 0L : previousImpressions;
    long current = newImpressions == null ? 0L : newImpressions;
    if (previous > 0 || current <= 0) {
      return;
    }
    log.info(
        "Primeiras impressões confirmadas; preservando eventos brutos e usando qualidade de tráfego para a leitura comercial. experimentId={} impressions={}",
        experiment.getId(),
        current);
  }

  /** Calcula o custo por clique a partir do gasto e dos cliques sincronizados. */
  private BigDecimal calculateCpc(BigDecimal spend, Long clicks) {
    if (spend == null || clicks == null || clicks == 0) {
      return null;
    }
    return spend.divide(BigDecimal.valueOf(clicks), 2, RoundingMode.HALF_UP);
  }

  /** Calcula o custo por lead a partir do gasto e dos leads sincronizados. */
  private BigDecimal calculateCpl(BigDecimal spend, Long leads) {
    if (spend == null || leads == null || leads == 0) {
      return null;
    }
    return spend.divide(BigDecimal.valueOf(leads), 2, RoundingMode.HALF_UP);
  }

  /** Aplica ao experimento apenas a diferença entre o gasto novo e o gasto anterior. */
  private void applySpendDelta(
      Experiment experiment, BigDecimal newSpend, BigDecimal previousSpend) {
    if (experiment == null) {
      return;
    }
    BigDecimal current = newSpend == null ? BigDecimal.ZERO : newSpend;
    BigDecimal previous = previousSpend == null ? BigDecimal.ZERO : previousSpend;
    BigDecimal delta = current.subtract(previous);
    if (delta.compareTo(BigDecimal.ZERO) == 0) {
      return;
    }
    costAttributionService.addCostToExperimentHierarchy(experiment, delta);
  }
}
