package com.marketinghub.geralanding;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.geralanding.presetdesign.service.PresetDesignCompletedEvent;
import com.marketinghub.geralanding.qualityreview.service.BackendQualityReviewService;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Coordena no backend o avanço entre etapas independentes do pipeline GeraLanding. */
@Component
public class GeraLandingStageTransitionCoordinator {

  private final ExperimentRepository experimentRepository;
  private final BackendQualityReviewService qualityReviewService;

  /** Inicializa o coordenador com as dependências necessárias para avançar o pipeline. */
  public GeraLandingStageTransitionCoordinator(
      ExperimentRepository experimentRepository, BackendQualityReviewService qualityReviewService) {
    this.experimentRepository = experimentRepository;
    this.qualityReviewService = qualityReviewService;
  }

  /** Agenda a revisão de qualidade depois que a transação do design preset for confirmada. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void scheduleQualityReview(PresetDesignCompletedEvent event) {
    Experiment experiment =
        experimentRepository
            .findById(event.experimentId())
            .orElseThrow(
                () -> new EntityNotFoundException("Experiment not found: " + event.experimentId()));
    qualityReviewService.reviewAfterHtmlGeneration(experiment);
  }
}
