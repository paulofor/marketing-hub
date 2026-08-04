package com.marketinghub.experiment.funnel.service.analytics;

/** Resume sessões humanas e automatizadas sem descartar a evidência técnica recebida. */
public record ExperimentLandingAnalyticsTrafficQualityDto(
    long humanSessions, long automatedSessions, long unknownSessions) {}
