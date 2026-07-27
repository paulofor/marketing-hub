package com.marketinghub.experiment.video.service;

import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.salesvideo.service.SalesVideoExperimentAssetApprovalChecker;
import java.util.List;
import org.springframework.stereotype.Service;

/** Verifica compliance de publicação para assets de vídeo gerados pelo domínio de experimento. */
@Service
public class ExperimentVideoAssetApprovalChecker
    implements SalesVideoExperimentAssetApprovalChecker {
  private final ExperimentVideoAssetRepository repository;

  /** Inicializa a verificação usando a fonte canônica dos vídeos de experimento. */
  public ExperimentVideoAssetApprovalChecker(ExperimentVideoAssetRepository repository) {
    this.repository = repository;
  }

  /** Retorna verdadeiro quando não existe vínculo pendente/reprovado para o asset informado. */
  @Override
  public boolean isApprovedForPublication(Long assetId) {
    if (assetId == null) {
      return true;
    }
    List<ExperimentVideoAsset> videoAssets = repository.findByAssetId(assetId);
    return videoAssets.stream()
        .noneMatch(
            videoAsset ->
                videoAsset.getStatus() != ExperimentVideoStatus.READY
                    || videoAsset.getReviewStatus() != ExperimentVideoReviewStatus.APPROVED);
  }
}
