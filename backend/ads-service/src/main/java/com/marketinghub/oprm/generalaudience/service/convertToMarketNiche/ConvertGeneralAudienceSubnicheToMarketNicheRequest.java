package com.marketinghub.oprm.generalaudience.service.convertToMarketNiche;

import jakarta.validation.constraints.Size;

/** Contrato de entrada para conversão controlada de subnicho geral aprovado em MarketNiche. */
public record ConvertGeneralAudienceSubnicheToMarketNicheRequest(
        @Size(max = 191) String name,
        String description,
        String baseSegmentation,
        String interests,
        String demographicFilters
) {
}
