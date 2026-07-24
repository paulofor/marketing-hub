package com.marketinghub.creative.dto;

import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.hypothesis.HypothesisStatus;

import java.util.UUID;

/**
 * Representa um criativo de vídeo com contexto comercial suficiente para aprovação.
 */
public record CreativeVideoReviewDto(
        Long id,
        Long experimentId,
        String experimentName,
        ExperimentStatus experimentStatus,
        UUID hypothesisId,
        String hypothesisTitle,
        HypothesisStatus hypothesisStatus,
        Long nicheId,
        String nicheName,
        String format,
        String headline,
        String primaryText,
        String videoId,
        String videoUrl,
        String description,
        String cta,
        String destinationUrl,
        CreativeStatus status) {
}
