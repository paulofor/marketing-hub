package com.marketinghub.metaaudience.service.linkExperiment;

/** Resposta do vínculo entre experimento e audiência CNAE. */
public record ExperimentMetaAudienceResponse(
        Long id,
        Long experimentId,
        Long marketNicheId,
        Long metaAudienceId,
        Long metaAudienceSegmentId,
        String cnaeCode,
        String audienceName,
        String segmentName,
        String activationStatus,
        String channel,
        String painAngle,
        String promise,
        String offer,
        String decisionSnapshotJson,
        String analysisSummary) {}
