package com.marketinghub.experiment.run;

/**
 * Classifica se a evidência de um run pode apoiar aprendizado comercial.
 */
public enum ExperimentEvidenceValidity {
    NOT_EVALUATED,
    TECHNICALLY_INVALID,
    MEASUREMENT_INVALID,
    STRATEGICALLY_INVALID,
    INSUFFICIENT_DATA,
    COMMERCIALLY_VALID
}
