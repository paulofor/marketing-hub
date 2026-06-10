package com.marketinghub.oprm.generalaudience.service.updateSeed;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeedStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeedType;
import jakarta.validation.constraints.Size;

/** Contrato de entrada para revisar manualmente uma semente de público geral. */
public record UpdateGeneralAudienceSeedRequest(
        @Size(max = 191) String name,
        String description,
        String marketContext,
        @Size(max = 64) String country,
        @Size(max = 32) String language,
        OprmGeneralAudienceSeedType seedType,
        OprmGeneralAudienceSeedStatus status,
        String businessGoal,
        String riskNotes
) {
}
