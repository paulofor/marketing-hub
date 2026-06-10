package com.marketinghub.oprm.generalaudience.service.landingConfirmation;

import java.util.List;

/** Payload para criar landing/formulário de confirmação de público geral. */
public record CreateGeneralAudienceLandingConfirmationRequest(
        Long experimentId,
        String name,
        String audienceConfirmationQuestion,
        List<String> qualificationOptions,
        String deliveryDescription,
        String whyItMakesSense,
        String nextStep,
        String painConfirmationQuestion) {
}
