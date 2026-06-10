package com.marketinghub.oprm.generalaudience.service.createQualityReading;

import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;

/** Contrato de entrada para registrar sinais de qualidade real dos leads de um público geral. */
public record CreateGeneralAudienceQualityReadingRequest(
        Long painAngleId,
        Long experimentId,
        @PositiveOrZero Integer totalLeads,
        @PositiveOrZero Integer correctProfessionLeads,
        @PositiveOrZero Integer realPainResponses,
        @PositiveOrZero Integer materialRequests,
        @PositiveOrZero Integer whatsappReplies,
        @PositiveOrZero Integer priceOrNextStepQuestions,
        @PositiveOrZero Integer outOfProfileLeads,
        @PositiveOrZero Integer curiousWithoutProfession,
        @PositiveOrZero Integer lowCompletionEvents,
        @PositiveOrZero Integer confusingPromiseReports,
        @PositiveOrZero Integer leadMagnetNoResponse,
        String notes,
        Instant capturedAt
) {
}
