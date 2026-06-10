package com.marketinghub.oprm.generalaudience.service.prepareTargeting;

import java.util.List;
import java.util.UUID;

/** Payload para registrar no backend os dados de público que o Facebook Ads buscará depois. */
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
