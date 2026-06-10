package com.marketinghub.oprm.generalaudience.service.convertToMarketNiche;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubnicheStatus;
import java.time.Instant;

/** Contrato de saída da conversão controlada de público geral para MarketNiche. */
public record GeneralAudienceMarketNicheConversionResponse(
        Long subnicheId,
        Long seedId,
        Long marketNicheId,
        String marketNicheName,
        OprmGeneralAudienceSubnicheStatus subnicheStatus,
        boolean reusedExistingMarketNiche,
        Instant convertedAt
) {
}
