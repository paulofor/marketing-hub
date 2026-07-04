package com.marketinghub.metaaudience.service.linkExperiment;

/** Contrato para vincular uma audiência CNAE planejada a um experimento. */
public record LinkMetaAudienceExperimentRequest(
        Long metaAudienceId,
        Long metaAudienceSegmentId,
        Long experimentId,
        String channel,
        String painAngle,
        String promise,
        String offer,
        String activationStatus,
        String decisionSnapshotJson) {}
