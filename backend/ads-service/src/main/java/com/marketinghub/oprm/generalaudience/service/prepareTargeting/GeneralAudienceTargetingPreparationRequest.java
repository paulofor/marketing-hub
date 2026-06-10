package com.marketinghub.oprm.generalaudience.service.prepareTargeting;

import java.util.List;
import java.util.UUID;

/** Payload para preparar targeting inicial conservador de público geral sem depender de CNAE. */
public record GeneralAudienceTargetingPreparationRequest(
        UUID hypothesisId,
        List<String> jobTitles,
        List<String> jobTitleMetaIds,
        List<String> interests,
        List<String> behaviors,
        String creativeScreeningPhrase,
        String demographicGuidance,
        String landingConfirmationInstruction,
        Boolean approvedJobTitlesAlreadyResolved,
        String reviewedBy) {
}
