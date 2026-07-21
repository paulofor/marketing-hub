package com.marketinghub.experiment.monitoring.dto;

import java.time.Instant;
import java.util.List;

/** Resume os logs recentes da Meta Ads usados no monitoramento pós-deploy. */
public record PostDeployFacebookLogSummaryDto(
        int totalLogs,
        int errorLogs,
        Instant lastLogAt,
        List<String> recentErrors
) {}
