package com.marketinghub.oprm.api;

import com.marketinghub.oprm.domain.HypothesisPerformanceSnapshot;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record Phase5FeedbackRequest(
        @NotBlank String occupationLabel,
        @NotBlank String nicheName,
        @NotBlank String locale,
        String correlationId,
        List<HypothesisPerformanceSnapshot> hypothesisPerformance) {
}
