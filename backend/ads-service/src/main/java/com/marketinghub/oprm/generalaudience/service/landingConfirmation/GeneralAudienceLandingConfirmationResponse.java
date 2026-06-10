package com.marketinghub.oprm.generalaudience.service.landingConfirmation;

import java.util.List;

/** Resposta do registro OPRM da situação de confirmação para público geral. */
public record GeneralAudienceLandingConfirmationResponse(
        Long confirmationRecordId,
        Long painAngleId,
        Long subnicheId,
        Long marketNicheId,
        Long experimentId,
        String slug,
        String name,
        String audienceSummary,
        String painSummary,
        String deliveryDescription,
        String whyItMakesSense,
        String nextStep,
        String status,
        List<GeneralAudienceLandingConfirmationQuestionResponse> questions) {
}
