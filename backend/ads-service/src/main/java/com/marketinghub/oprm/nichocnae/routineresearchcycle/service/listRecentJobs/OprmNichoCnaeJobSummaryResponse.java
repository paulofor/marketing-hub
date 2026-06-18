package com.marketinghub.oprm.nichocnae.routineresearchcycle.service.listRecentJobs;

import java.math.BigDecimal;
import java.time.Instant;

/** Representa um job recente do pipeline OPRM NichoCNAE para acompanhamento administrativo. */
public record OprmNichoCnaeJobSummaryResponse(
    Long id,
    String cnaeCode,
    String cnaeDescription,
    String subniche,
    String status,
    BigDecimal costUsd,
    String lastStageCode,
    Instant lastStageAt,
    String reportUrl,
    String trackingUrl) {}
