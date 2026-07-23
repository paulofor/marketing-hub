package com.marketinghub.experiment.video.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.dto.JobCompletionRequest;
import com.marketinghub.salesvideo.dto.JobFailureRequest;
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
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

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

    /** Propaga falha definitiva de render para os videos de experimento vinculados ao job. */
    @Override
    public void syncFailedRender(SalesVideoJob job, JobFailureRequest request) {
        if (job == null || job.getId() == null) {
            return;
        }
        List<ExperimentVideoAsset> videoAssets = repository.findBySalesVideoJobId(job.getId());
        if (videoAssets.isEmpty()) {
            return;
        }
        for (ExperimentVideoAsset videoAsset : videoAssets) {
            applyFailedRender(videoAsset, job, request);
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

    /** Registra a causa da falha no ativo para a tela não permanecer como gerando. */
    private void applyFailedRender(ExperimentVideoAsset videoAsset,
                                   SalesVideoJob job,
                                   JobFailureRequest request) {
        videoAsset.setStatus(ExperimentVideoStatus.FAILED);
        videoAsset.setResponseJson(failureSnapshot(job, request));
    }

    /** Monta um JSON simples com a causa operacional da falha do render. */
    private String failureSnapshot(SalesVideoJob job, JobFailureRequest request) {
        String message = request != null ? request.getMessage() : null;
        String failureCode = request != null && request.getFailureCode() != null
                ? request.getFailureCode()
                : job.getFailureCode();
        String failureDetail = request != null && request.getFailureDetail() != null
                ? request.getFailureDetail()
                : job.getFailureDetail();
        try {
            return OBJECT_MAPPER.writeValueAsString(new FailureSnapshot(
                    "VIDEO_FAILED",
                    job.getId(),
                    failureCode,
                    message,
                    failureDetail));
        } catch (JsonProcessingException ex) {
            return "{\"status\":\"VIDEO_FAILED\",\"jobId\":%d}".formatted(job.getId());
        }
    }

    /** Snapshot persistido para explicar a falha operacional na tela do experimento. */
    private record FailureSnapshot(String status,
                                   Long jobId,
                                   String failureCode,
                                   String message,
                                   String failureDetail) {}
}
