package com.marketinghub.experiment.monitoring.dto;

import java.time.Instant;
import java.util.List;

/** Resposta consolidada do painel de monitoramento pós-deploy do experimento. */
public record PostDeployMonitorResponseDto(
        Long experimentId,
        String productSlug,
        Instant generatedAt,
        PostDeployMonitorDecision decision,
        String decisionLabel,
        String recommendation,
        PostDeployMetaAdsSummaryDto metaAds,
        PostDeployPdeSummaryDto pde,
        PostDeployFacebookLogSummaryDto logs,
        List<String> alerts
) {}
