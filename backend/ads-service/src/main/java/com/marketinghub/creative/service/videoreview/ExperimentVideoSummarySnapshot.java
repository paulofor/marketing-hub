package com.marketinghub.creative.service.videoreview;

import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;

/** Resume a revisão e o contexto de um vídeo sem hidratar arquivos e auditorias históricas. */
public record ExperimentVideoSummarySnapshot(
    Long id,
    ExperimentStatus experimentStatus,
    ExperimentVideoReviewStatus reviewStatus,
    boolean requiredForRelease) {}
