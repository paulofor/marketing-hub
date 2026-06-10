package com.marketinghub.oprm.generalaudience.service.qualityGate;

import java.util.List;

/** Contrato de saída do gate que impede subnicho genérico ou contaminado por promessa arriscada. */
public record GeneralAudienceQualityGateResponse(
        Long subnicheId,
        boolean approved,
        List<String> blockers,
        List<String> recommendations
) {
}
