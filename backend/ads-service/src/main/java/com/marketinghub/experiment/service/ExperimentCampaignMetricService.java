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
    BigDecimal normalizedSpend = aggregateSpend(campaign, spend);
    ExperimentCampaignMetric metric =
        repository
            .findByExperiment(experiment)
            .orElseGet(() -> ExperimentCampaignMetric.builder().experiment(experiment).build());
    Long previousImpressions = metric.getImpressions();
    BigDecimal previousSpend = metric.getSpend();
    logCommercialStart(experiment, previousImpressions, impressions);
    metric.setCampaign(campaign);
    metric.setDateStart(earliest(metric.getDateStart(), dateStart));
    metric.setDateStop(latest(metric.getDateStop(), dateStop));
    metric.setReach(aggregate(campaign.getPriorReach(), reach));
    metric.setImpressions(aggregate(campaign.getPriorImpressions(), impressions));
    metric.setClicks(aggregate(campaign.getPriorClicks(), clicks));
    metric.setLeads(aggregate(campaign.getPriorLeads(), leads));
    metric.setSpend(normalizedSpend);
    metric.setCpc(calculateCpc(normalizedSpend, metric.getClicks()));
    metric.setCpl(calculateCpl(normalizedSpend, metric.getLeads()));
    ExperimentCampaignMetric saved = repository.save(metric);
    applySpendDelta(experiment, normalizedSpend, previousSpend);
    runMetricLifecycleService.synchronize(experiment, campaign, metric.getImpressions());
    return saved;
  }

  /**
   * Congela as métricas anteriores na campanha substituta e mantém o total conciliado do
   * experimento antes do primeiro sync da nova campanha Meta.
   */
  @Transactional
  public ExperimentCampaignMetric activateReplacement(
      FacebookAdsCampaign replacement, BigDecimal confirmedPriorSpend) {
    if (replacement == null || replacement.getExperiment() == null) {
      throw new IllegalArgumentException("Campanha substituta e experimento são obrigatórios");
    }
    BigDecimal confirmed =
        confirmedPriorSpend == null ? null : confirmedPriorSpend.setScale(2, RoundingMode.HALF_UP);
    if (confirmed == null || confirmed.signum() < 0) {
      throw new IllegalArgumentException("Gasto anterior confirmado é obrigatório");
    }
    Experiment experiment = replacement.getExperiment();
    ExperimentCampaignMetric metric =
        repository
            .findByExperiment(experiment)
            .orElseGet(() -> ExperimentCampaignMetric.builder().experiment(experiment).build());
    BigDecimal previousSpend = metric.getSpend();
    if (previousSpend != null && confirmed.compareTo(previousSpend) < 0) {
      throw new IllegalArgumentException(
          "Gasto Meta confirmado não pode ser menor que o total já conciliado");
    }
    replacement.setPriorReach(metric.getReach());
    replacement.setPriorImpressions(metric.getImpressions());
    replacement.setPriorClicks(metric.getClicks());
    replacement.setPriorLeads(metric.getLeads());
    replacement.setPriorSpendMinor(toMinor(confirmed));
    metric.setCampaign(replacement);
    metric.setSpend(confirmed);
    metric.setCpc(calculateCpc(confirmed, metric.getClicks()));
    metric.setCpl(calculateCpl(confirmed, metric.getLeads()));
    ExperimentCampaignMetric saved = repository.save(metric);
    applySpendDelta(experiment, confirmed, previousSpend);
    return saved;
  }

  /** Soma o retrato anterior imutável à métrica corrente devolvida pela campanha substituta. */
  private Long aggregate(Long prior, Long current) {
    if (prior == null && current == null) {
      return null;
    }
    return Math.addExact(prior == null ? 0L : prior, current == null ? 0L : current);
  }

  /** Soma o gasto anterior em centavos sem transformar ausência do insight atual em zero. */
  private BigDecimal aggregateSpend(FacebookAdsCampaign campaign, BigDecimal currentSpend) {
    if (currentSpend == null) {
      return null;
    }
    BigDecimal prior =
        campaign.getPriorSpendMinor() == null
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(campaign.getPriorSpendMinor(), 2);
    return prior.add(currentSpend).setScale(2, RoundingMode.HALF_UP);
  }

  /** Converte reais conciliados para centavos exatos usados no contrato da Meta. */
  private long toMinor(BigDecimal value) {
    return value.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
  }

  /** Preserva o primeiro dia observado ao trocar a campanha física do mesmo experimento. */
  private LocalDate earliest(LocalDate current, LocalDate candidate) {
    if (current == null) return candidate;
    if (candidate == null) return current;
    return current.isBefore(candidate) ? current : candidate;
  }

  /** Preserva o último dia observado no agregado do experimento. */
  private LocalDate latest(LocalDate current, LocalDate candidate) {
    if (current == null) return candidate;
    if (candidate == null) return current;
    return current.isAfter(candidate) ? current : candidate;
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
    if (newSpend == null) {
      return;
    }
    BigDecimal current = newSpend;
    BigDecimal previous = previousSpend == null ? BigDecimal.ZERO : previousSpend;
    BigDecimal delta = current.subtract(previous);
    if (delta.compareTo(BigDecimal.ZERO) == 0) {
      return;
    }
    costAttributionService.addCostToExperimentHierarchy(experiment, delta);
  }
}
