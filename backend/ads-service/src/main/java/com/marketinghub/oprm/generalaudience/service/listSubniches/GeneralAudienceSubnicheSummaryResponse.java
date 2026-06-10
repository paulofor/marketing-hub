package com.marketinghub.oprm.generalaudience.service.listSubniches;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubnicheStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Contrato de saída resumido para revisão operacional de subnichos de uma semente. */
public record GeneralAudienceSubnicheSummaryResponse(
        Long id,
        Long seedId,
        String name,
        String personaSummary,
        String painSummary,
        String channelsSummary,
        String qualificationQuestion,
        OprmGeneralAudienceSubnicheStatus status,
        BigDecimal opportunityScore,
        BigDecimal riskScore,
        Long marketNicheId,
        Instant updatedAt
) {
}
