package com.marketinghub.experiment.funnel;

import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelDiagnosticsResponseDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDiagnosticDto;
import com.marketinghub.experiment.funnel.dto.FunnelDiagnosticStatus;
import com.marketinghub.experiment.run.service.ExperimentRunMetricLifecycleService;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável por invalidar experimentos automaticamente quando há evidência operacional
 * ruim.
 */
@Service
public class ExperimentFunnelAutoStopService {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExperimentFunnelAutoStopService.class);
  private static final BigDecimal ZERO_PRIMARY_RESULT_MINIMUM_SPEND = new BigDecimal("25.00");
  private static final Duration LOW_IMPRESSIONS_MIN_CAMPAIGN_AGE = Duration.ofHours(48);
  private static final long LOW_IMPRESSIONS_MINIMUM = 100L;

  private final ExperimentFunnelDiagnosticService diagnosticService;
  private final ExperimentFunnelStandbyService standbyService;
  private final ExperimentCampaignMetricRepository campaignMetricRepository;
  private final ExperimentRunMetricLifecycleService runMetricLifecycleService;
  private final AgentTaskService agentTaskService;

  /**
   * Cria o serviço com diagnóstico de funil, campanhas e métricas para registrar pausas
   * automáticas.
   */
  public ExperimentFunnelAutoStopService(
      ExperimentFunnelDiagnosticService diagnosticService,
      ExperimentFunnelStandbyService standbyService,
      ExperimentCampaignMetricRepository campaignMetricRepository,
      ExperimentRunMetricLifecycleService runMetricLifecycleService,
      AgentTaskService agentTaskService) {
    this.diagnosticService = diagnosticService;
    this.standbyService = standbyService;
    this.campaignMetricRepository = campaignMetricRepository;
    this.runMetricLifecycleService = runMetricLifecycleService;
    this.agentTaskService = agentTaskService;
  }

  /**
   * Aplica a regra única de campanha inclusive na liquidação final após uma pausa observada na
   * Meta: após R$ 25,00, campanha sem resultado primário deve parar.
   *
   * @return {@code true} quando o experimento foi parado automaticamente, {@code false} caso
   *     contrário.
   */
  public boolean stopIfNoPrimaryResultAfterMinimumSpend(Experiment experiment) {
    if (!isEligibleForZeroPrimaryResultStop(experiment)) {
      return false;
    }
    ZeroPrimaryResultEvidence evidence = resolveZeroPrimaryResultEvidence(experiment);
    if (!evidence.reachedStopThreshold()) {
      return false;
    }
    LOGGER.warn(
        "Automatic campaign stop triggered for experiment {} due to zero primary result after minimum spend: spend={}, minimumSpend={}, formSubmissions={}, sampleEmailOpens={}, purchases={}",
        experiment.getId(),
        evidence.campaignSpend(),
        ZERO_PRIMARY_RESULT_MINIMUM_SPEND,
        evidence.formSubmissions(),
        evidence.sampleEmailOpens(),
        evidence.purchases());
    invalidateExperimentAndRequestStops(
        experiment,
        FacebookCampaignStopReason.CAMPAIGN_ZERO_RESULT_AFTER_MINIMUM_SPEND,
        "campanha gastou R$ 25,00 sem resultado primário: envio de formulário, abertura de email de amostra ou compra");
    return true;
  }

  /**
   * Interrompe o experimento no primeiro gasto sincronizado que alcançar o teto total autorizado.
   *
   * @return {@code true} quando a trava financeira foi aplicada.
   */
  public boolean stopIfMediaSpendLimitReached(Experiment experiment) {
    if (experiment == null
        || experiment.getStatus() != ExperimentStatus.RUNNING
        || experiment.getMediaSpendLimit() == null
        || experiment.getMediaSpendLimit().compareTo(BigDecimal.ZERO) <= 0) {
      return false;
    }
    BigDecimal campaignSpend = resolveCampaignSpend(experiment);
    if (campaignSpend.compareTo(experiment.getMediaSpendLimit()) < 0) {
      return false;
    }
    LOGGER.warn(
        "Automatic campaign stop triggered for experiment {} after media spend limit: spend={}, mediaSpendLimit={}",
        experiment.getId(),
        campaignSpend,
        experiment.getMediaSpendLimit());
    invalidateExperimentAndRequestStops(
        experiment,
        FacebookCampaignStopReason.CAMPAIGN_MEDIA_SPEND_LIMIT_REACHED,
        "teto total autorizado de mídia atingido");
    return true;
  }

  /**
   * Mantém elegíveis a execução ativa e a pausa externa recém-reconciliada, sem reabrir estados
   * comerciais já encerrados.
   */
  private boolean isEligibleForZeroPrimaryResultStop(Experiment experiment) {
    return experiment != null
        && (experiment.getStatus() == ExperimentStatus.RUNNING
            || experiment.getStatus() == ExperimentStatus.USER_STOPPED);
  }

  /**
   * Indica se uma pausa manual possui gasto mínimo e ausência comprovada de resultado primário para
   * oferecer a reconciliação financeira na interface.
   */
  public boolean isFinancialReconciliationAvailable(Experiment experiment) {
    return experiment != null
        && experiment.getStatus() == ExperimentStatus.USER_STOPPED
        && resolveZeroPrimaryResultEvidence(experiment).reachedStopThreshold();
  }

  /** Consolida uma única leitura canônica do gasto e dos resultados primários do experimento. */
  private ZeroPrimaryResultEvidence resolveZeroPrimaryResultEvidence(Experiment experiment) {
    BigDecimal campaignSpend = resolveCampaignSpend(experiment);
    if (campaignSpend.compareTo(ZERO_PRIMARY_RESULT_MINIMUM_SPEND) < 0) {
      return new ZeroPrimaryResultEvidence(campaignSpend, 0, 0, 0);
    }
    ExperimentFunnelDiagnosticsResponseDto diagnostics =
        diagnosticService.diagnose(experiment.getId());
    return new ZeroPrimaryResultEvidence(
        campaignSpend,
        successesFor(diagnostics, ExperimentFunnelStage.ENVIO_FORM),
        successesFor(diagnostics, ExperimentFunnelStage.ABERTURA_EMAIL_AMOSTRA),
        successesFor(diagnostics, ExperimentFunnelStage.COMPRA));
  }

  /** Representa a evidência mínima necessária para aplicar a trava comercial sem falso positivo. */
  private record ZeroPrimaryResultEvidence(
      BigDecimal campaignSpend, long formSubmissions, long sampleEmailOpens, long purchases) {

    /** Confirma simultaneamente o limite de gasto e a ausência de todos os resultados primários. */
    private boolean reachedStopThreshold() {
      return campaignSpend.compareTo(ZERO_PRIMARY_RESULT_MINIMUM_SPEND) >= 0
          && formSubmissions == 0
          && sampleEmailOpens == 0
          && purchases == 0;
    }
  }

  /**
   * Aplica a regra única de campanha: qualquer etapa prioritária estatisticamente reprovada deve
   * parar a campanha.
   *
   * @return {@code true} quando o experimento foi parado automaticamente, {@code false} caso
   *     contrário.
   */
  public boolean stopIfAnyPrioritizedStageStatisticallyFailed(Experiment experiment) {
    if (experiment == null || experiment.getStatus() != ExperimentStatus.RUNNING) {
      return false;
    }
    ExperimentFunnelDiagnosticsResponseDto diagnostics =
        diagnosticService.diagnose(experiment.getId());
    ExperimentFunnelStageDiagnosticDto failedStage =
        diagnostics.diagnostics().stream()
            .filter(dto -> dto.status() == FunnelDiagnosticStatus.STATISTICALLY_FAILED)
            .findFirst()
            .orElse(null);
    if (failedStage == null) {
      return false;
    }
    LOGGER.warn(
        "Automatic campaign stop triggered for experiment {} due to statistically failed funnel stage: stage={}, attempts={}, successes={}, observedRate={}, minimumRate={}",
        experiment.getId(),
        failedStage.stageKey(),
        failedStage.attempts(),
        failedStage.successes(),
        failedStage.observedRate(),
        failedStage.minAcceptableRate());
    invalidateExperimentAndRequestStops(
        experiment,
        FacebookCampaignStopReason.CAMPAIGN_STATISTICALLY_FAILED_STAGE,
        "etapa prioritária do funil reprovada estatisticamente para a política única de campanha");
    return true;
  }

  /**
   * Recupera o gasto de mídia sincronizado para decidir se a parada por baixo interesse já pode ser
   * executada.
   */
  private BigDecimal resolveCampaignSpend(Experiment experiment) {
    if (experiment == null) {
      return BigDecimal.ZERO;
    }
    return campaignMetricRepository
        .findByExperiment(experiment)
        .map(ExperimentCampaignMetric::getSpend)
        .orElse(BigDecimal.ZERO);
  }

  /**
   * Avalia se a campanha rodou tempo suficiente com impressões muito baixas e invalida o
   * experimento.
   */
  public boolean stopIfLowImpressionsAfterRunningTime(
      Experiment experiment, Long impressions, Instant campaignCreatedAt) {
    if (experiment == null
        || experiment.getStatus() != ExperimentStatus.RUNNING
        || campaignCreatedAt == null) {
      return false;
    }
    Instant minimumAgeInstant = Instant.now().minus(LOW_IMPRESSIONS_MIN_CAMPAIGN_AGE);
    if (campaignCreatedAt.isAfter(minimumAgeInstant)) {
      return false;
    }
    long normalizedImpressions = impressions == null ? 0L : impressions;
    if (normalizedImpressions >= LOW_IMPRESSIONS_MINIMUM) {
      return false;
    }
    LOGGER.warn(
        "Automatic stop triggered for experiment {} due to low impressions after {} hours: impressions={}, minimum={}",
        experiment.getId(),
        LOW_IMPRESSIONS_MIN_CAMPAIGN_AGE.toHours(),
        normalizedImpressions,
        LOW_IMPRESSIONS_MINIMUM);
    invalidateExperimentAndRequestStops(
        experiment,
        FacebookCampaignStopReason.LOW_IMPRESSIONS_AFTER_RUNNING_TIME,
        "menos de 100 impressões após 48 horas de campanha");
    return true;
  }

  /**
   * Coloca o experimento em standby no primeiro envio válido usando o serviço sem dependência
   * circular.
   *
   * @return {@code true} quando o standby foi aplicado, {@code false} caso o experimento não esteja
   *     elegível.
   */
  public boolean standbyOnFirstValidFormSubmission(Experiment experiment) {
    return standbyService.standbyOnFirstValidFormSubmission(experiment);
  }

  /** Localiza o diagnóstico de uma etapa específica dentro da resposta consolidada. */
  private ExperimentFunnelStageDiagnosticDto findStageDiagnostic(
      ExperimentFunnelDiagnosticsResponseDto diagnostics, ExperimentFunnelStage stage) {
    if (diagnostics == null || diagnostics.diagnostics() == null || stage == null) {
      return null;
    }
    return diagnostics.diagnostics().stream()
        .filter(dto -> dto.stageKey() == stage)
        .findFirst()
        .orElse(null);
  }

  /** Soma os sucessos de uma etapa quando o diagnóstico contém essa etapa. */
  private long successesFor(
      ExperimentFunnelDiagnosticsResponseDto diagnostics, ExperimentFunnelStage stage) {
    ExperimentFunnelStageDiagnosticDto diagnostic = findStageDiagnostic(diagnostics, stage);
    return diagnostic != null ? diagnostic.successes() : 0L;
  }

  /** Atualiza o status do experimento e registra o motivo de parada nas campanhas vinculadas. */
  private void invalidateExperimentAndRequestStops(
      Experiment experiment, FacebookCampaignStopReason stopReason, String businessReason) {
    experiment.setStatus(ExperimentStatus.INVALIDATED);
    standbyService.requestFacebookCampaignStops(experiment.getId(), stopReason, businessReason);
    runMetricLifecycleService.completeCommercialStop(experiment, stopReason, businessReason);
    agentTaskService.cancelActiveTasksBySourceReference(
        "experiment:" + experiment.getId(), businessReason);
  }
}
