package com.marketinghub.experiment.run.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.facebookads.FacebookAdsCampaign;
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
