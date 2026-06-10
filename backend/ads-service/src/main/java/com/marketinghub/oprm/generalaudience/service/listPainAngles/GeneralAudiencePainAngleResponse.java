package com.marketinghub.oprm.generalaudience.service.listPainAngles;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngleStatus;
import java.time.Instant;

/** Contrato de saída com dor, resultado e ângulo seguro para público geral. */
public record GeneralAudiencePainAngleResponse(
        Long id,
        Long subnicheId,
        String pain,
        String desiredResult,
        String mechanismDirection,
        String proofOrLeadMagnet,
        String safePromise,
        String firstAdHook,
        String landingConfirmationQuestion,
        String complianceNotes,
        OprmGeneralAudiencePainAngleStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
