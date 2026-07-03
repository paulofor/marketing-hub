package com.marketinghub.productai.dto;

import com.marketinghub.productai.ProductAiSubtype;
import java.math.BigDecimal;
import java.util.UUID;

/** Responsabilidade: representar o resultado do preparo sistêmico de uma hipótese para amostra personalizada. */
public record PersonalizedSamplePreparationDto(
        UUID hypothesisId,
        String hypothesisTitle,
        ProductAiSubtype productAiSubtype,
        BigDecimal price,
        Long offerPackageId,
        String offerPackageName,
        Long deliverableId,
        String deliverableTitle,
        ProductAiExperimentPreparationDto experimentPreparation) {
}
