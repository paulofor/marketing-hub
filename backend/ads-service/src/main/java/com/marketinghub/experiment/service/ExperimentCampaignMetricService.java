package com.marketinghub.experiment.service;

import com.marketinghub.cost.CostAttributionService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentLandingAnalyticsEventRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mantém as métricas agregadas de campanha e propaga o custo de mídia para o experimento. */
@Service
public class ExperimentCampaignMetricService {
  private static final Logger log = LoggerFactory.getLogger(ExperimentCampaignMetricService.class);
  private static final String CLUBEMUSA_PRODUCT_SLUG = "metodo-musa-7-dias";

  private final ExperimentCampaignMetricRepository repository;
  private final FacebookAdsCampaignRepository campaignRepository;
  private final CostAttributionService costAttributionService;
  private final ExperimentRepository experimentRepository;
  private final ExperimentFunnelEventRepository funnelEventRepository;
  private final ExperimentLandingAnalyticsEventRepository landingAnalyticsEventRepository;
  private final String pdeCampaignAnalyticsResetUrl;
  private final HttpClient pdeResetHttpClient;

  /** Inicializa o serviço com repositórios de campanha, métricas e atribuição de custo. */
  public ExperimentCampaignMetricService(
      ExperimentCampaignMetricRepository repository,
      FacebookAdsCampaignRepository campaignRepository,
      CostAttributionService costAttributionService,
      ExperimentRepository experimentRepository,
      ExperimentFunnelEventRepository funnelEventRepository,
      ExperimentLandingAnalyticsEventRepository landingAnalyticsEventRepository,
      @Value("${pde.campaign-analytics-reset-url:}") String pdeCampaignAnalyticsResetUrl) {
    this.repository = repository;
    this.campaignRepository = campaignRepository;
    this.costAttributionService = costAttributionService;
    this.experimentRepository = experimentRepository;
    this.funnelEventRepository = funnelEventRepository;
    this.landingAnalyticsEventRepository = landingAnalyticsEventRepository;
    this.pdeCampaignAnalyticsResetUrl = pdeCampaignAnalyticsResetUrl;
    this.pdeResetHttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
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
    resetTestFunnelWhenImpressionsStart(experiment, previousImpressions, impressions);
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
    return saved;
  }

  /**
   * Remove automaticamente eventos de teste quando a campanha começa a receber impressões reais.
   */
  private void resetTestFunnelWhenImpressionsStart(
      Experiment experiment, Long previousImpressions, Long newImpressions) {
    if (experiment == null || experiment.getId() == null) {
      return;
    }
    long previous = previousImpressions == null ? 0L : previousImpressions;
    long current = newImpressions == null ? 0L : newImpressions;
    if (previous > 0 || current <= 0) {
      return;
    }
    landingAnalyticsEventRepository.deleteByExperimentId(experiment.getId());
    funnelEventRepository.deleteByExperimentIdAndSource(
        experiment.getId(), ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE);
    funnelEventRepository.deleteByExperimentId(experiment.getId());
    experiment.setFunnelResetAt(Instant.now());
    experimentRepository.save(experiment);
    resetPdeAnalyticsIfNeeded(experiment);
  }

  /** Limpa analytics do Clube MUSA quando a campanha paga real começa a receber impressões. */
  private void resetPdeAnalyticsIfNeeded(Experiment experiment) {
    if (!isClubMusaExperiment(experiment)) {
      return;
    }
    String resetUrl = resolvePdeResetUrl(experiment);
    if (resetUrl == null || resetUrl.isBlank()) {
      throw new IllegalStateException(
          "URL de reset de analytics PDE não configurada para Clube MUSA");
    }
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(resetUrl))
              .timeout(Duration.ofSeconds(10))
              .POST(HttpRequest.BodyPublishers.noBody())
              .build();
      HttpResponse<String> response =
          pdeResetHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException(
            "Reset de analytics PDE respondeu HTTP " + response.statusCode());
      }
    } catch (IOException ex) {
      log.error(
          "Falha de IO ao limpar analytics PDE no inicio da campanha; experimentId={}, resetUrl={}",
          experiment.getId(),
          resetUrl,
          ex);
      throw new IllegalStateException(
          "Não foi possível limpar analytics PDE no início da campanha", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error(
          "Reset de analytics PDE interrompido no inicio da campanha; experimentId={}, resetUrl={}",
          experiment.getId(),
          resetUrl,
          ex);
      throw new IllegalStateException("Reset de analytics PDE interrompido", ex);
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao limpar analytics PDE no inicio da campanha; experimentId={}, resetUrl={}",
          experiment.getId(),
          resetUrl,
          ex);
      throw ex;
    }
  }

  /** Identifica se o experimento usa a area publica do Clube MUSA como destino comercial. */
  private boolean isClubMusaExperiment(Experiment experiment) {
    return experiment != null
        && experiment.getFollowUpActionUrl() != null
        && experiment.getFollowUpActionUrl().toLowerCase().contains("clubemusa.com.br");
  }

  /** Resolve o endpoint operacional de reset do PDE para o produto MUSA. */
  private String resolvePdeResetUrl(Experiment experiment) {
    if (pdeCampaignAnalyticsResetUrl != null && !pdeCampaignAnalyticsResetUrl.isBlank()) {
      return pdeCampaignAnalyticsResetUrl.replace("{productSlug}", CLUBEMUSA_PRODUCT_SLUG);
    }
    String followUpActionUrl = experiment.getFollowUpActionUrl();
    if (followUpActionUrl == null || followUpActionUrl.isBlank()) {
      return null;
    }
    URI uri = URI.create(followUpActionUrl);
    String baseUrl = uri.getScheme() + "://" + uri.getHost();
    return baseUrl
        + "/api/pde/access/analytics/"
        + CLUBEMUSA_PRODUCT_SLUG
        + "/reset-campaign-start";
  }

  /** Calcula o custo por clique a partir do gasto e dos cliques sincronizados. */
  private BigDecimal calculateCpc(BigDecimal spend, Long clicks) {
    if (spend == null || clicks == null || clicks == 0) {
      return BigDecimal.ZERO;
    }
    return spend.divide(BigDecimal.valueOf(clicks), 2, RoundingMode.HALF_UP);
  }

  /** Calcula o custo por lead a partir do gasto e dos leads sincronizados. */
  private BigDecimal calculateCpl(BigDecimal spend, Long leads) {
    if (spend == null || leads == null || leads == 0) {
      return BigDecimal.ZERO;
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
