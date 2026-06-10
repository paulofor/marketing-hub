package com.marketinghub.oprm.generalaudience.service.listQualityReadings;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Contrato de saída da leitura de qualidade real de um público geral. */
public record GeneralAudienceQualityReadingResponse(
        Long id,
        Long subnicheId,
        Long painAngleId,
        Long experimentId,
        Integer totalLeads,
        Integer correctProfessionLeads,
        Integer realPainResponses,
        Integer materialRequests,
        Integer whatsappReplies,
        Integer priceOrNextStepQuestions,
        Integer outOfProfileLeads,
        Integer curiousWithoutProfession,
        Integer lowCompletionEvents,
        Integer confusingPromiseReports,
        Integer leadMagnetNoResponse,
        BigDecimal qualityScore,
        boolean approved,
        List<String> blockers,
        List<String> recommendations,
        String notes,
        Instant capturedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
