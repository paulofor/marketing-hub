package com.marketinghub.experiment.service.cockpit;

/** Pergunta comercial que o experimento tenta responder no mercado. */
public record ExperimentCockpitQuestionDto(
    String pain,
    String promise,
    String mechanism,
    String offer,
    String primaryCta,
    String primaryVariable,
    String primaryMetric) {}
