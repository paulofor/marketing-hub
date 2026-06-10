package com.marketinghub.oprm.generalaudience.service.getSubniche;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubnicheStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Contrato de saída com todos os campos decisórios do subnicho de público geral. */
public record GeneralAudienceSubnicheResponse(
        Long id,
        Long seedId,
        String name,
        String personaSummary,
        String painSummary,
        String desiredOutcomeSummary,
        String languagePatterns,
        String channelsSummary,
        String qualificationQuestion,
        OprmGeneralAudienceSubnicheStatus status,
        BigDecimal opportunityScore,
        BigDecimal riskScore,
        Long marketNicheId,
        Instant createdAt,
        Instant updatedAt
) {
}
