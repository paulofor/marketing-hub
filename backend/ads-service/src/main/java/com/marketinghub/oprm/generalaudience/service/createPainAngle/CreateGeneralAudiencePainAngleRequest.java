package com.marketinghub.oprm.generalaudience.service.createPainAngle;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngleStatus;
import jakarta.validation.constraints.NotBlank;

/** Contrato de entrada para cadastrar dor e ângulo testável de público geral. */
public record CreateGeneralAudiencePainAngleRequest(
        @NotBlank String pain,
        @NotBlank String desiredResult,
        String mechanismDirection,
        String proofOrLeadMagnet,
        String safePromise,
        String firstAdHook,
        String landingConfirmationQuestion,
        String complianceNotes,
        OprmGeneralAudiencePainAngleStatus status
) {
}
