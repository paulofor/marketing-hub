package com.marketinghub.oprm.generalaudience.service.prepareTargeting;

import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;

/** Resposta de um elemento de targeting criado para público geral. */
public record GeneralAudienceTargetingElementResponse(
        Long id,
        TargetingElementType type,
        String term,
        TargetingElementStatus status,
        String metaId,
        boolean publishableForCurrentPublisher) {
}
