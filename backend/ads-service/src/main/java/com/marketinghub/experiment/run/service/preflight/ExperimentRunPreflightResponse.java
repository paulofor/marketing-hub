package com.marketinghub.experiment.run.service.preflight;

import com.marketinghub.experiment.run.ExperimentRunStatus;
import java.util.List;

/** Contrato de leitura do preflight atual de um run de experimento. */
public record ExperimentRunPreflightResponse(
    Long runId,
    ExperimentRunStatus runStatus,
    boolean hasBlockers,
    String requiredLandingEvidenceReference,
    String currentEvidenceBlockReason,
    List<ExperimentRunGateResultResponse> gates) {}
