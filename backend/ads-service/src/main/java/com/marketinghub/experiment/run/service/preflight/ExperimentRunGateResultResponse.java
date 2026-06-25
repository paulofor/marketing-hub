package com.marketinghub.experiment.run.service.preflight;

import com.marketinghub.experiment.run.ExperimentRunGateEvaluatorType;
import com.marketinghub.experiment.run.ExperimentRunGateGroup;
import com.marketinghub.experiment.run.ExperimentRunGateSeverity;
import com.marketinghub.experiment.run.ExperimentRunGateStatus;

import java.time.Instant;

/**
 * Contrato de leitura de um gate avaliado para um run de experimento.
 */
public record ExperimentRunGateResultResponse(
        String gateCode,
        ExperimentRunGateGroup gateGroup,
        ExperimentRunGateStatus status,
        ExperimentRunGateSeverity severity,
        String summary,
        String evidenceReference,
        String remediationCode,
        Instant evaluatedAt,
        ExperimentRunGateEvaluatorType evaluatorType,
        String evaluatorVersion
) {
}
