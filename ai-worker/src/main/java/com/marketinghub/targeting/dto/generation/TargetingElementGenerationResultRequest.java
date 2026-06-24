package com.marketinghub.targeting.dto.generation;

import com.marketinghub.targeting.dto.CreateTargetingElementRequest;
import java.util.List;

/**
 * Responsabilidade: representar o payload enviado pelo AI Worker ao backend com públicos gerados.
 */
public record TargetingElementGenerationResultRequest(List<CreateTargetingElementRequest> items) {
}
