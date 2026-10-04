package com.marketinghub.creative.service.videoreview;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Classifica a prontidão da revisão sem alterar decisões, versões, evidências ou orçamento. */
public final class VideoReviewPolicy {
  private static final Set<ExperimentStatus> CLOSED =
      Set.of(
          ExperimentStatus.USER_STOPPED,
          ExperimentStatus.VALIDATED,
          ExperimentStatus.INVALIDATED,
          ExperimentStatus.INCONCLUSIVE,
          ExperimentStatus.FINISHED,
          ExperimentStatus.FAILED);

  /** Impede instanciação da política determinística. */
  private VideoReviewPolicy() {}

  /** Reconhece ancestrais substituídos por descendente aprovado no mesmo experimento. */
  public static Map<Long, Long> supersededBy(List<Creative> creatives) {
    Map<Long, Long> result = new HashMap<>();
    for (Creative approved : creatives) {
      if (approved.getStatus() != CreativeStatus.READY
          || (approved.getAgentReviewStatus() != null
              && approved.getAgentReviewStatus() != CreativeAgentReviewStatus.APPROVED)) continue;
      Set<Long> visited = new HashSet<>();
      visited.add(approved.getId());
      Creative ancestor = approved.getSourceCreative();
      while (ancestor != null
          && ancestor.getId() != null
          && visited.add(ancestor.getId())
          && ancestor.getExperiment() != null
          && approved.getExperiment() != null
          && Objects.equals(ancestor.getExperiment().getId(), approved.getExperiment().getId())) {
        result.merge(ancestor.getId(), approved.getId(), Math::max);
        ancestor = ancestor.getSourceCreative();
      }
    }
    return result;
  }

  /** Explica por que uma tentativa não deve solicitar nova decisão ou análise paga. */
  public static String historicalReason(Experiment experiment, Long replacementId) {
    if (replacementId != null)
      return "Versão substituída pelo criativo aprovado #"
          + replacementId
          + ". Aprovação e histórico preservados; não é necessário aprovar novamente.";
    if (experiment != null
        && experiment.getStatus() != null
        && CLOSED.contains(experiment.getStatus())) {
      return "Experimento encerrado ("
          + experiment.getStatus()
          + "). Tentativa preservada no histórico; não exige aprovação para continuar o fluxo atual.";
    }
    return null;
  }

  /** Preserva decisões existentes e separa a espera humana dos impedimentos técnicos. */
  public static VideoReviewEligibility classify(
      CreativeStatus status,
      String historicalReason,
      String blockedReason,
      boolean agentRetryAvailable) {
    if (status == CreativeStatus.READY)
      return new VideoReviewEligibility(
          VideoReviewState.APPROVED,
          "Aprovação já registrada. Não é necessário aprovar novamente.",
          false,
          false);
    if (status == CreativeStatus.REJECTED)
      return new VideoReviewEligibility(
          VideoReviewState.REJECTED,
          "Reprovação registrada; consulte o motivo antes de qualquer nova decisão.",
          historicalReason == null && blockedReason == null,
          historicalReason == null && agentRetryAvailable);
    if (historicalReason != null)
      return new VideoReviewEligibility(
          VideoReviewState.HISTORICAL, historicalReason, false, false);
    if (blockedReason != null)
      return new VideoReviewEligibility(
          VideoReviewState.BLOCKED, blockedReason, false, agentRetryAvailable);
    return new VideoReviewEligibility(
        VideoReviewState.AWAITING_REVIEW,
        "Nova peça disponível para revisão. A decisão não publica campanha nem autoriza gasto.",
        true,
        false);
  }
}
