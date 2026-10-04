package com.marketinghub.creative.service.videoreview;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.creative.*;
import com.marketinghub.experiment.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Valida a classificação genérica sem confundir histórico, parecer técnico e decisão humana. */
class VideoReviewPolicyTest {
  /** Todos os estados finais removem rascunhos da fila humana sem fabricar aprovação. */
  @ParameterizedTest
  @EnumSource(
      value = ExperimentStatus.class,
      names = {"USER_STOPPED", "VALIDATED", "INVALIDATED", "INCONCLUSIVE", "FINISHED", "FAILED"})
  void classifiesClosedExperimentsAsHistory(ExperimentStatus state) {
    var reason =
        VideoReviewPolicy.historicalReason(Experiment.builder().status(state).build(), null);
    var result = VideoReviewPolicy.classify(CreativeStatus.DRAFT, reason, "Parecer ausente", true);
    assertThat(result.state()).isEqualTo(VideoReviewState.HISTORICAL);
    assertThat(result.approvalAvailable()).isFalse();
    assertThat(result.agentReviewRequestAvailable()).isFalse();
  }

  /** Mantém experimentos em preparação, execução ou pausa como contextos revisáveis. */
  @ParameterizedTest
  @EnumSource(
      value = ExperimentStatus.class,
      names = {"PLANNED", "RUNNING", "PAUSED", "STANDBY"})
  void keepsOpenExperimentsReviewable(ExperimentStatus state) {
    assertThat(VideoReviewPolicy.historicalReason(Experiment.builder().status(state).build(), null))
        .isNull();
    assertThat(
            VideoReviewPolicy.classify(CreativeStatus.DRAFT, null, null, false).approvalAvailable())
        .isTrue();
  }

  /** Reconhece ancestral distante e interrompe ciclos ou vínculos com outro experimento. */
  @Test
  void followsApprovedLineageWithoutCrossingScopeOrCycling() {
    var exp = Experiment.builder().id(810L).status(ExperimentStatus.PLANNED).build();
    var first = Creative.builder().id(901L).experiment(exp).status(CreativeStatus.DRAFT).build();
    var second =
        Creative.builder()
            .id(902L)
            .experiment(exp)
            .sourceCreative(first)
            .status(CreativeStatus.DRAFT)
            .build();
    var approved =
        Creative.builder()
            .id(903L)
            .experiment(exp)
            .sourceCreative(second)
            .status(CreativeStatus.READY)
            .agentReviewStatus(CreativeAgentReviewStatus.APPROVED)
            .build();
    first.setSourceCreative(approved);
    assertThat(VideoReviewPolicy.supersededBy(List.of(approved)))
        .containsEntry(901L, 903L)
        .containsEntry(902L, 903L)
        .doesNotContainKey(903L);
    approved.setStatus(CreativeStatus.DRAFT);
    assertThat(VideoReviewPolicy.supersededBy(List.of(approved))).isEmpty();
    approved.setStatus(CreativeStatus.READY);
    approved.setExperiment(Experiment.builder().id(811L).build());
    assertThat(VideoReviewPolicy.supersededBy(List.of(approved))).isEmpty();
  }

  /** Aprovação antiga permanece decisão válida; bloqueio exige correção, não clique humano. */
  @Test
  void preservesDecisionsAndSeparatesTechnicalBlocks() {
    var approved = VideoReviewPolicy.classify(CreativeStatus.READY, "Encerrado", null, true);
    assertThat(approved.state()).isEqualTo(VideoReviewState.APPROVED);
    assertThat(approved.approvalAvailable()).isFalse();
    assertThat(approved.agentReviewRequestAvailable()).isFalse();
    assertThat(
            VideoReviewPolicy.classify(CreativeStatus.REJECTED, null, "Parecer reprovado", true)
                .agentReviewRequestAvailable())
        .isTrue();
    var blocked = VideoReviewPolicy.classify(CreativeStatus.DRAFT, null, "Corrigir copy", true);
    assertThat(blocked.state()).isEqualTo(VideoReviewState.BLOCKED);
    assertThat(blocked.approvalAvailable()).isFalse();
    assertThat(blocked.reason()).isEqualTo("Corrigir copy");
    assertThat(
            VideoReviewPolicy.classify(CreativeStatus.REJECTED, "Encerrado", null, false)
                .approvalAvailable())
        .isFalse();
  }
}
