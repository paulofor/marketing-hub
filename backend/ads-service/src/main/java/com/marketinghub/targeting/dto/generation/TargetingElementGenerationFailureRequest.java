package com.marketinghub.targeting.dto.generation;

/**
 * Contrato usado pelo AI Worker para reportar falha de geração de público ao backend.
 */
public record TargetingElementGenerationFailureRequest(String error) {
}
