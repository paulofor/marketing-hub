package com.marketinghub.targeting.dto.generation;

import com.marketinghub.targeting.TargetingElementType;

/**
 * Contrato de pendência de geração de público entregue ao AI Worker sem expor acesso ao banco.
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
