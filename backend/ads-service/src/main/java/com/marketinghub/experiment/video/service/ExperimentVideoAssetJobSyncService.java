package com.marketinghub.experiment.video.service;

import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.dto.JobCompletionRequest;
import com.marketinghub.salesvideo.service.SalesVideoCompletedRenderAssetSync;
import com.marketinghub.salesvideo.service.SalesVideoProductionCostCalculator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Atualiza ativos de video de experimento a partir do resultado final de um job SalesVideo.
 */
@Service
public class ExperimentVideoAssetJobSyncService implements SalesVideoCompletedRenderAssetSync {
    private final ExperimentVideoAssetRepository repository;
    private final SalesVideoProductionCostCalculator costCalculator;

    /** Inicializa a sincronizacao com persistencia dos ativos e calculo oficial de custo. */
    public ExperimentVideoAssetJobSyncService(ExperimentVideoAssetRepository repository,
                                              SalesVideoProductionCostCalculator costCalculator) {
        this.repository = repository;
        this.costCalculator = costCalculator;
    }

    /** Propaga o asset final, poster, duracao, custo e metadata para os videos vinculados ao job. */
    @Override
    public void syncCompletedRender(SalesVideoJob job,
                                    JobCompletionRequest request,
                                    Integer durationSeconds,
                                    String resolution) {
        if (job == null || job.getId() == null) {
            return;
        }
        List<ExperimentVideoAsset> videoAssets = repository.findBySalesVideoJobId(job.getId());
        if (videoAssets.isEmpty()) {
            return;
        }
        BigDecimal costUsd = Optional.ofNullable(request.getCostUsd())
                .orElseGet(() -> costCalculator.estimateUsd(
                        job.getProviderName(),
                        job.getProviderName(),
                        durationSeconds,
                        resolution));
        for (ExperimentVideoAsset videoAsset : videoAssets) {
            applyCompletedRender(videoAsset, job, request, durationSeconds, costUsd);
        }
        repository.saveAll(videoAssets);
    }

    /** Aplica os campos auditaveis do render concluido em um ativo de experimento. */
    private void applyCompletedRender(ExperimentVideoAsset videoAsset,
                                      SalesVideoJob job,
                                      JobCompletionRequest request,
                                      Integer durationSeconds,
                                      BigDecimal costUsd) {
        if (job.getAsset() != null) {
            videoAsset.setAsset(job.getAsset());
            videoAsset.setAssetUrl(job.getAsset().getUrl());
        }
        if (job.getPosterAsset() != null) {
            videoAsset.setThumbnailUrl(job.getPosterAsset().getUrl());
        }
        if (durationSeconds != null) {
            videoAsset.setDurationSeconds(durationSeconds);
        }
        if (costUsd != null) {
            videoAsset.setCost(costUsd);
        }
        videoAsset.setResponseJson(request.getMetadataJson());
        videoAsset.setStatus(ExperimentVideoStatus.READY);
    }
}
