package com.marketinghub.creative.dto;

import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.CreativeVideoReviewSourceType;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.hypothesis.HypothesisStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Representa um criativo de vídeo com contexto comercial suficiente para aprovação e apuração de
 * custo.
 */
public record CreativeVideoReviewDto(
    Long id,
    CreativeVideoReviewSourceType sourceType,
    ExperimentVideoSlot funnelSlot,
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
    CreativeStatus status,
    CreativeAgentReviewStatus agentReviewStatus,
    String agentReviewSummary,
    String approvalBlockedReason,
    String rejectionReason,
    Instant reviewedAt,
    Instant createdAt,
    BigDecimal videoCostUsd,
    BigDecimal audioCostUsd,
    BigDecimal totalProductionCostUsd,
    String visualSourceType,
    String visualSourceKey,
    String visualSourceDescription,
    String visualSimilarityOverrideReason) {}
