package com.marketinghub.experiment.dto;

/** Recebe o objetivo comercial e a função operacional atual editados no detalhe do experimento. */
public record UpdateExperimentStrategicPositioningRequest(
    String commercialObjective, String currentOperationalFunction) {}
