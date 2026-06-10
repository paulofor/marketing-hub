package com.marketinghub.oprm.generalaudience.service.landingConfirmation;

import java.util.List;

/** Resposta resumida de pergunta criada no formulário de confirmação. */
public record GeneralAudienceLandingConfirmationQuestionResponse(
        String title,
        String dataKey,
        String type,
        boolean required,
        List<String> options) {
}
