package com.marketinghub.oprm.generalaudience.service.prepareTargeting;

import java.util.List;
import java.util.UUID;

/** Resposta do registro de dados de público no backend para coleta posterior pelo Facebook Ads. */
public record GeneralAudienceTargetingPreparationResponse(
        Long painAngleId,
        Long subnicheId,
        Long marketNicheId,
        UUID hypothesisId,
        boolean publishableForCurrentPublisher,
        List<String> blockers,
        List<String> recommendations,
        List<GeneralAudienceTargetingElementResponse> elements) {
}
