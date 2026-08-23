package com.marketinghub.experiment.run.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.run.ExperimentEvidenceValidity;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunFailureClassification;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Reconcilia o ciclo produtivo do run com a publicação e a primeira impressão da campanha. */
@Service
public class ExperimentRunMetricLifecycleService {
  private static final Logger log =
      LoggerFactory.getLogger(ExperimentRunMetricLifecycleService.class);
  private static final Set<ExperimentRunStatus> EXPOSURE_ELIGIBLE_STATUSES =
      EnumSet.of(
          ExperimentRunStatus.READY_TO_PUBLISH,
          ExperimentRunStatus.PUBLICATION_PENDING,
          ExperimentRunStatus.PUBLISHING,
          ExperimentRunStatus.PUBLISHED_AWAITING_EXPOSURE,
          ExperimentRunStatus.RUNNING);

  private final ExperimentRunRepository experimentRunRepository;

  /** Inicializa o reconciliador com a persistência canônica dos runs. */
  public ExperimentRunMetricLifecycleService(ExperimentRunRepository experimentRunRepository) {
    this.experimentRunRepository = experimentRunRepository;
  }

  /**
   * Marca a campanha como publicada e abre a janela comercial somente na primeira impressão
   * confirmada pela Meta.
   */
  public void synchronize(
      Experiment experiment, FacebookAdsCampaign campaign, Long synchronizedImpressions) {
    if (experiment == null || experiment.getId() == null || campaign == null) {
      return;
    }
    experimentRunRepository
        .findTopByExperimentIdAndModeOrderByRunNumberDesc(
            experiment.getId(), ExperimentRunMode.PRODUCTION)
        .filter(run -> EXPOSURE_ELIGIBLE_STATUSES.contains(run.getStatus()))
        .ifPresent(run -> synchronize(run, experiment, campaign, synchronizedImpressions));
  }

  /**
   * Encerra a tentativa produtiva quando uma regra comercial determinística invalida a campanha.
   */
  public void completeCommercialStop(
      Experiment experiment, FacebookCampaignStopReason stopReason, String detail) {
    if (experiment == null || experiment.getId() == null || stopReason == null) {
      return;
    }
    experimentRunRepository
        .findTopByExperimentIdAndModeOrderByRunNumberDesc(
            experiment.getId(), ExperimentRunMode.PRODUCTION)
        .filter(run -> !isTerminal(run.getStatus()))
        .ifPresent(run -> completeCommercialStop(run, experiment, stopReason, detail));
  }

  /**
   * Persiste o resultado comercial final sem transformar uma reprovação válida em falha técnica.
   */
  private void completeCommercialStop(
      ExperimentRun run,
      Experiment experiment,
      FacebookCampaignStopReason stopReason,
      String detail) {
    run.setStatus(ExperimentRunStatus.COMPLETED);
    run.setStopReason(stopReason.name());
    run.setFailureClassification(resolveFailureClassification(stopReason));
    run.setFailureDetail(detail);
    run.setEvidenceValidity(resolveEvidenceValidity(stopReason));
    run.setEndedAt(Instant.now());
    experimentRunRepository.save(run);
    log.info(
        "experiment_run_commercial_stop experimentId={} runId={} stopReason={} evidenceValidity={} failureClassification={}",
        experiment.getId(),
        run.getId(),
        stopReason,
        run.getEvidenceValidity(),
        run.getFailureClassification());
  }

  /** Identifica estados que não podem ser reabertos pela reconciliação financeira. */
  private boolean isTerminal(ExperimentRunStatus status) {
    return status == ExperimentRunStatus.COMPLETED
        || status == ExperimentRunStatus.FAILED
        || status == ExperimentRunStatus.CANCELLED;
  }

  /** Classifica a validade da evidência conforme a causa comercial da parada. */
  private ExperimentEvidenceValidity resolveEvidenceValidity(
      FacebookCampaignStopReason stopReason) {
    return stopReason == FacebookCampaignStopReason.LOW_IMPRESSIONS_AFTER_RUNNING_TIME
        ? ExperimentEvidenceValidity.INSUFFICIENT_DATA
        : ExperimentEvidenceValidity.COMMERCIALLY_VALID;
  }

  /** Classifica o motivo operacional preservando a distinção entre audiência e hipótese. */
  private ExperimentRunFailureClassification resolveFailureClassification(
      FacebookCampaignStopReason stopReason) {
    return stopReason == FacebookCampaignStopReason.LOW_IMPRESSIONS_AFTER_RUNNING_TIME
        ? ExperimentRunFailureClassification.AUDIENCE_FAILURE
        : ExperimentRunFailureClassification.COMMERCIAL_HYPOTHESIS_FAILURE;
  }

  /** Aplica os marcos ausentes sem reabrir runs terminais nem deslocar uma janela já iniciada. */
  private void synchronize(
      ExperimentRun run,
      Experiment experiment,
      FacebookAdsCampaign campaign,
      Long synchronizedImpressions) {
    Instant now = Instant.now();
    boolean changed = false;
    if (run.getPublicationRequestedAt() == null) {
      run.setPublicationRequestedAt(
          firstInstant(experiment.getFacebookReleaseRequestedAt(), campaign.getCreatedAt(), now));
      changed = true;
    }
    if (run.getPublishedAt() == null) {
      run.setPublishedAt(firstInstant(campaign.getCreatedAt(), now));
      changed = true;
    }
    if (run.getStatus() != ExperimentRunStatus.RUNNING
        && run.getStatus() != ExperimentRunStatus.PUBLISHED_AWAITING_EXPOSURE) {
      run.setStatus(ExperimentRunStatus.PUBLISHED_AWAITING_EXPOSURE);
      changed = true;
    }
    if (synchronizedImpressions != null && synchronizedImpressions > 0) {
      if (run.getFirstVerifiedImpressionAt() == null) {
        run.setFirstVerifiedImpressionAt(now);
        changed = true;
      }
      if (run.getCommercialWindowStartedAt() == null) {
        run.setCommercialWindowStartedAt(now);
        changed = true;
      }
      if (run.getStatus() != ExperimentRunStatus.RUNNING) {
        run.setStatus(ExperimentRunStatus.RUNNING);
        changed = true;
      }
    }
    if (!changed) {
      return;
    }
    experimentRunRepository.save(run);
    log.info(
        "experiment_run_metric_lifecycle experimentId={} runId={} campaignId={} impressions={} status={} publishedAt={} firstVerifiedImpressionAt={} commercialWindowStartedAt={}",
        experiment.getId(),
        run.getId(),
        campaign.getId(),
        synchronizedImpressions,
        run.getStatus(),
        run.getPublishedAt(),
        run.getFirstVerifiedImpressionAt(),
        run.getCommercialWindowStartedAt());
  }

  /** Retorna o primeiro instante disponível para manter a reconciliação idempotente. */
  private Instant firstInstant(Instant... candidates) {
    for (Instant candidate : candidates) {
      if (candidate != null) {
        return candidate;
      }
    }
    return null;
  }
}
