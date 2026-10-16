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
  private final com.marketinghub.agenttask.CreativeFinalVideoEvidence finalReview;

  /** Consulta a fonte canônica de vídeos e a revisão independente dos mesmos arquivos. */
  public ExperimentVideoAssetApprovalChecker(
      ExperimentVideoAssetRepository repository,
      com.marketinghub.agenttask.CreativeFinalVideoEvidence finalReview) {
    this.repository = repository;
    this.finalReview = finalReview;
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

  /** Exige ao menos um ativo e reprovação humana explícita em todos os vínculos do acabamento. */
  @Override
  public boolean isRejectedForReplacement(Long salesVideoJobId) {
    if (salesVideoJobId == null) {
      return false;
    }
    List<ExperimentVideoAsset> videoAssets = repository.findBySalesVideoJobId(salesVideoJobId);
    return !videoAssets.isEmpty()
        && videoAssets.stream()
            .allMatch(
                videoAsset -> videoAsset.getReviewStatus() == ExperimentVideoReviewStatus.REJECTED);
  }

  /** Autoriza somente correção de candidatas pendentes fora de uma campanha em operação. */
  @Override
  public boolean isCorrectionRequested(Long salesVideoJobId) {
    if (salesVideoJobId == null) return false;
    var assets = repository.findBySalesVideoJobId(salesVideoJobId);
    return !assets.isEmpty()
        && assets.stream()
            .allMatch(
                video ->
                    video.getStatus() == ExperimentVideoStatus.READY
                        && video.getReviewStatus() == ExperimentVideoReviewStatus.PENDING
                        && video.getExperiment() != null
                        && video.getExperiment().getStatus() != null
                        && java.util.Set.of(
                                com.marketinghub.experiment.ExperimentStatus.PLANNED,
                                com.marketinghub.experiment.ExperimentStatus.PAUSED,
                                com.marketinghub.experiment.ExperimentStatus.USER_STOPPED)
                            .contains(video.getExperiment().getStatus())
                        && finalReview.correctionRequested(video));
  }
}
