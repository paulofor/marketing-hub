package com.marketinghub.experiment.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleVideoBinding;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Responsabilidade: aplicar ao gate de vídeos a seleção explícita e vigente do ciclo. */
@Component
@Slf4j
public class ExperimentVideoReleaseSelection {
  private final LearningCycleVideoBinding binding;
  private final ExperimentVideoAssetRepository videos;

  /** Reutiliza a identidade canônica das peças sem criar outra regra de seleção por data. */
  public ExperimentVideoReleaseSelection(
      @Lazy LearningCycleVideoBinding binding, ExperimentVideoAssetRepository videos) {
    this.binding = binding;
    this.videos = videos;
  }

  /**
   * Confere os dois vídeos selecionados e preserva os demais slots obrigatórios; sem recibo, mantém
   * a política legada do experimento.
   */
  public Optional<Boolean> blocksRelease(Long experimentId) {
    try {
      var receipt = binding.forReference("experiment:" + experimentId);
      if (receipt.isEmpty()) return Optional.empty();
      JsonNode proof = receipt.orElseThrow();
      if (proof.path("experimentId").asLong(-1) != experimentId
          || !LearningCycleVideoBinding.CONTRACT.equals(proof.path("evidenceType").asText()))
        return Optional.of(true);
      var selected =
          Map.of(
              ExperimentVideoSlot.AD, proof.path("campaignVideo"),
              ExperimentVideoSlot.LANDING_HERO, proof.path("heroVideo"));
      var assets = videos.findByExperimentIdOrderByCreatedAtDesc(experimentId);
      for (var entry : selected.entrySet()) {
        var matches =
            assets.stream()
                .filter(v -> Objects.equals(v.getId(), entry.getValue().path("assetId").asLong(-1)))
                .toList();
        if (matches.size() != 1
            || !sameApprovedMedia(
                matches.getFirst(), experimentId, entry.getKey(), entry.getValue()))
          return Optional.of(true);
      }
      return Optional.of(
          assets.stream()
              .filter(ExperimentVideoAsset::isRequiredForRelease)
              .filter(v -> !selected.containsKey(v.getSlot()))
              .anyMatch(v -> !ready(v)));
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao conferir seleção audiovisual para liberação experimentId={}", experimentId, ex);
      return Optional.of(true);
    }
  }

  /** Exige experimento, papel, arquivo e aprovação da mídia exata, sem aceitar outro produto. */
  private boolean sameApprovedMedia(
      ExperimentVideoAsset asset, Long experimentId, ExperimentVideoSlot slot, JsonNode proof) {
    return asset.getExperiment() != null
        && Objects.equals(experimentId, asset.getExperiment().getId())
        && asset.getSlot() == slot
        && slot.name().equals(proof.path("role").asText())
        && Objects.equals(asset.getAssetUrl(), proof.path("assetUrl").asText())
        && asset.getAssetUrl() != null
        && !asset.getAssetUrl().isBlank()
        && "APPROVED".equals(proof.path("reviewStatus").asText())
        && ready(asset);
  }

  /** Mantém os dois estados obrigatórios da política de liberação já existente. */
  private boolean ready(ExperimentVideoAsset asset) {
    return asset.getStatus() == ExperimentVideoStatus.READY
        && asset.getReviewStatus() == ExperimentVideoReviewStatus.APPROVED;
  }
}
