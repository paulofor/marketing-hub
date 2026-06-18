package com.marketinghub.experiment;

/**
 * Estado operacional da geração de criativos solicitada ao Worker AI.
 */
public enum CreativeGenerationStatus {
    IDLE,
    REQUESTED,
    PROCESSING,
    COMPLETED,
    FAILED,
    TIMEOUT
}
