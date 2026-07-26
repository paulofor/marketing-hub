package com.marketinghub.experiment.video.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Painel consolidado de desempenho comercial dos vídeos de um experimento.
 */
public record ExperimentVideoPerformanceDashboardDto(
        ExperimentVideoPerformanceSummaryDto summary,
        List<ExperimentVideoPerformanceAssetDto> assets,
        List<ExperimentVideoPerformanceCampaignDto> campaigns) {

    /**
     * Resumo executivo dos marcos comerciais do vídeo no funil.
     */
    public record ExperimentVideoPerformanceSummaryDto(
            long approvedAssets,
            long metaVideoCreatives,
            long impressions,
            long clicks,
            long diagnosticStarts,
            long checkoutAccesses,
            long purchases,
            BigDecimal spend,
            Instant lastMetricAt,
            String recommendation) {}

    /**
     * Linha de leitura por asset aprovado ou relevante para campanha.
     */
    public record ExperimentVideoPerformanceAssetDto(
            Long assetId,
            String slot,
            String reviewStatus,
            String status,
            String provider,
            String model,
            String assetUrl,
            String attributionLevel,
            List<ExperimentVideoPerformanceCreativeDto> metaCreatives,
            long diagnosticStarts,
            long checkoutAccesses,
            long purchases) {}

    /**
     * Criativo/anúncio Meta associado ou candidato de associação do vídeo.
     */
    public record ExperimentVideoPerformanceCreativeDto(
            String creativeId,
            String creativeKind,
            String metaVideoId,
            String adId,
            String adName,
            String adStatus,
            long diagnosticStarts,
            long checkoutAccesses,
            long purchases) {}

    /**
     * Campanha Meta com métricas de topo de funil ligadas ao experimento.
     */
    public record ExperimentVideoPerformanceCampaignDto(
            String campaignId,
            String campaignName,
            String status,
            long impressions,
            long clicks,
            BigDecimal spend,
            Instant metricsLastSyncedAt) {}
}
