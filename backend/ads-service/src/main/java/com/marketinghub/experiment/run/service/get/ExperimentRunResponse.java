package com.marketinghub.experiment.run.service.get;

import com.marketinghub.experiment.run.ExperimentEvidenceValidity;
import com.marketinghub.experiment.run.ExperimentRunDataQualityStatus;
import com.marketinghub.experiment.run.ExperimentRunFailureClassification;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.experiment.run.ExperimentRunStopPolicy;

import java.time.Instant;

/**
 * Contrato de leitura de uma execução operacional de experimento.
 */
public record ExperimentRunResponse(
        Long id,
        Long experimentId,
        Integer runNumber,
        ExperimentRunMode mode,
        ExperimentRunStatus status,
        ExperimentEvidenceValidity evidenceValidity,
        Integer strategyVersion,
        Integer assetBundleVersion,
        Integer audienceVersion,
        ExperimentRunStopPolicy stopPolicy,
        String stopReason,
        ExperimentRunFailureClassification failureClassification,
        String failureDetail,
        ExperimentRunDataQualityStatus dataQualityStatus,
        Instant requestedAt,
        Instant preflightStartedAt,
        Instant preflightCompletedAt,
        Instant publicationRequestedAt,
        Instant publishedAt,
        Instant firstVerifiedImpressionAt,
        Instant commercialWindowStartedAt,
        Instant endedAt,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
