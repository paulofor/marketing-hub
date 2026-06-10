package com.marketinghub.oprm.generalaudience.service.prepareTargeting;

import java.util.List;
import java.util.UUID;

/** Resposta do preparo de targeting inicial indicando se o publicador atual pode avançar com segurança. */
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
