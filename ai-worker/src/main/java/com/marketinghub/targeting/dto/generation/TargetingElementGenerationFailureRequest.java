package com.marketinghub.targeting.dto.generation;

/**
 * Responsabilidade: representar o payload enviado pelo AI Worker ao backend quando a geração falha.
 */
public record TargetingElementGenerationFailureRequest(String error) {
}
