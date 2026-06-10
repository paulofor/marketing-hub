package com.marketinghub.oprm.generalaudience.service.updateSubniche;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubnicheStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Contrato de entrada para revisar manualmente um subnicho de público geral. */
public record UpdateGeneralAudienceSubnicheRequest(
        @Size(max = 191) String name,
        String personaSummary,
        String painSummary,
        String desiredOutcomeSummary,
        String languagePatterns,
        String channelsSummary,
        String qualificationQuestion,
        OprmGeneralAudienceSubnicheStatus status,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal opportunityScore,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal riskScore,
        Long marketNicheId
) {
}
