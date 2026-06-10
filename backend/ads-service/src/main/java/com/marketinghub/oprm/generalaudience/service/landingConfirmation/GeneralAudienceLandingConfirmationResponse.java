package com.marketinghub.oprm.generalaudience.service.landingConfirmation;

import java.util.List;

/** Resposta da criação de landing/formulário de confirmação para público geral. */
public record GeneralAudienceLandingConfirmationResponse(
        Long painAngleId,
        Long subnicheId,
        Long marketNicheId,
        Long experimentId,
        Long leadPortalFlowId,
        String slug,
        String name,
        String audienceSummary,
        String painSummary,
        String deliveryDescription,
        String whyItMakesSense,
        String nextStep,
        List<GeneralAudienceLandingConfirmationQuestionResponse> questions) {
}
