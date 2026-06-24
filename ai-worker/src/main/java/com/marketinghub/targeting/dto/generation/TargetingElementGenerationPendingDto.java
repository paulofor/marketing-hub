package com.marketinghub.targeting.dto.generation;

import com.marketinghub.targeting.TargetingElementType;

/**
 * Responsabilidade: representar a pendência de geração de público recebida do backend pelo AI Worker.
 */
public record TargetingElementGenerationPendingDto(
        Long nicheId,
        String name,
        String description,
        String baseSegmentation,
        String interests,
        String demographicFilters,
        String extraTips,
        String interestCategory,
        String roleCategory,
        TargetingElementType type,
        int quantity,
        String model) {
}
