package com.marketinghub.oprm.generalaudience.service.listSeeds;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeedStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeedType;
import java.time.Instant;

/** Contrato de saída resumido para seleção e listagem de sementes de público geral. */
public record GeneralAudienceSeedSummaryResponse(
        Long id,
        String name,
        String marketContext,
        String country,
        String language,
        OprmGeneralAudienceSeedType seedType,
        OprmGeneralAudienceSeedStatus status,
        Instant updatedAt
) {
}
