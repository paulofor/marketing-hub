package com.marketinghub.oprm.generalaudience.service.updatePainAngle;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngleStatus;

/** Contrato de entrada para revisar uma dor e seu ângulo testável. */
public record UpdateGeneralAudiencePainAngleRequest(
        String pain,
        String desiredResult,
        String mechanismDirection,
        String proofOrLeadMagnet,
        String safePromise,
        String firstAdHook,
        String landingConfirmationQuestion,
        String complianceNotes,
        OprmGeneralAudiencePainAngleStatus status
) {
}
