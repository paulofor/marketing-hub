package com.marketinghub.targeting.dto.generation;

import com.marketinghub.targeting.dto.CreateTargetingElementRequest;
import java.util.List;

/**
 * Contrato usado pelo AI Worker para reportar públicos gerados ao backend.
 */
public record TargetingElementGenerationResultRequest(List<CreateTargetingElementRequest> items) {
}
