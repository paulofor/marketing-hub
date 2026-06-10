package com.marketinghub.oprm.generalaudience.service.createHypothesis;

import java.time.Instant;
import java.util.UUID;

/** Contrato de saída da hipótese criada para uma dor principal de público geral. */
public record GeneralAudienceHypothesisResponse(
        Long painAngleId,
        Long subnicheId,
        Long marketNicheId,
        UUID hypothesisId,
        String title,
        String status,
        String statement,
        Instant createdAt
) {
}
