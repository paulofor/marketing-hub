package com.marketinghub.oprm.generalaudience.service.getSeed;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeedStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeedType;
import java.time.Instant;

/** Contrato de saída com todos os campos editáveis da semente de público geral. */
public record GeneralAudienceSeedResponse(
        Long id,
        String name,
        String description,
        String marketContext,
        String country,
        String language,
        OprmGeneralAudienceSeedType seedType,
        OprmGeneralAudienceSeedStatus status,
        String businessGoal,
        String riskNotes,
        Instant createdAt,
        Instant updatedAt
) {
}
