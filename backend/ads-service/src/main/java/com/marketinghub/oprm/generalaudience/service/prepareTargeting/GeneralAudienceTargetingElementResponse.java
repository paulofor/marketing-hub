package com.marketinghub.oprm.generalaudience.service.prepareTargeting;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceAdSignalStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceAdSignalType;

/** Resposta de um dado de público registrado no OPRM para coleta posterior pelo Facebook Ads. */
public record GeneralAudienceTargetingElementResponse(
        Long id,
        OprmGeneralAudienceAdSignalType type,
        String term,
        OprmGeneralAudienceAdSignalStatus status,
        String metaId,
        boolean publishableForCurrentPublisher) {
}
