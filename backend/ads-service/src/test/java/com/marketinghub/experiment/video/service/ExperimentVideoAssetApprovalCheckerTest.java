package com.marketinghub.experiment.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida a fronteira de revisão humana consultada pelo módulo de vídeo. */
@ExtendWith(MockitoExtension.class)
class ExperimentVideoAssetApprovalCheckerTest {
  @Mock private ExperimentVideoAssetRepository repository;
  @Mock private com.marketinghub.agenttask.CreativeFinalVideoEvidence finalReview;

  /** Candidata opcional deixa de notificar, mas permanece impedida de publicação sem aprovação. */
  @Test
  void optionalCandidateStillRequiresApprovalBeforePublication() {
    var checker = new ExperimentVideoAssetApprovalChecker(repository, finalReview);
    var optional =
        ExperimentVideoAsset.builder()
            .id(831L)
            .status(ExperimentVideoStatus.READY)
            .reviewStatus(ExperimentVideoReviewStatus.PENDING)
            .requiredForRelease(false)
            .build();
    given(repository.findByAssetId(832L)).willReturn(List.of(optional));
    assertThat(checker.isApprovedForPublication(832L)).isFalse();
    optional.setReviewStatus(ExperimentVideoReviewStatus.APPROVED);
    assertThat(checker.isApprovedForPublication(832L)).isTrue();
    optional.setReviewStatus(ExperimentVideoReviewStatus.REJECTED);
    assertThat(checker.isApprovedForPublication(832L)).isFalse();
  }

  /** Libera substituição somente quando existe ativo e todos foram explicitamente reprovados. */
  @Test
  void shouldRequireAllJobAssetsRejectedForReplacement() {
    var checker = new ExperimentVideoAssetApprovalChecker(repository, finalReview);
    var rejected =
        ExperimentVideoAsset.builder()
            .id(46L)
            .reviewStatus(ExperimentVideoReviewStatus.REJECTED)
            .build();
    var pending =
        ExperimentVideoAsset.builder()
            .id(47L)
            .reviewStatus(ExperimentVideoReviewStatus.PENDING)
            .build();
    given(repository.findBySalesVideoJobId(21248L))
        .willReturn(List.of(rejected), List.of(rejected, pending), List.of());

    assertThat(checker.isRejectedForReplacement(21248L)).isTrue();
    assertThat(checker.isRejectedForReplacement(21248L)).isFalse();
    assertThat(checker.isRejectedForReplacement(21248L)).isFalse();
    assertThat(checker.isRejectedForReplacement(null)).isFalse();
  }

  /** Correção técnica não muda seleção humana nem libera campanha ou arquivo sem parecer. */
  @Test
  void permitsOnlyPendingCandidateWithIndependentCorrection() {
    var checker = new ExperimentVideoAssetApprovalChecker(repository, finalReview);
    var experiment = new com.marketinghub.experiment.Experiment();
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.PLANNED);
    var video =
        ExperimentVideoAsset.builder()
            .id(93072L)
            .experiment(experiment)
            .status(ExperimentVideoStatus.READY)
            .reviewStatus(ExperimentVideoReviewStatus.PENDING)
            .build();
    given(repository.findBySalesVideoJobId(93090L)).willReturn(List.of(video));
    given(finalReview.correctionRequested(video)).willReturn(true);
    assertThat(checker.isCorrectionRequested(93090L)).isTrue();
    assertThat(video.getReviewStatus()).isEqualTo(ExperimentVideoReviewStatus.PENDING);
    for (var status :
        List.of(ExperimentVideoReviewStatus.APPROVED, ExperimentVideoReviewStatus.REJECTED)) {
      video.setReviewStatus(status);
      assertThat(checker.isCorrectionRequested(93090L)).isFalse();
    }
    video.setReviewStatus(ExperimentVideoReviewStatus.PENDING);
    for (var status :
        List.of(
            com.marketinghub.experiment.ExperimentStatus.RUNNING,
            com.marketinghub.experiment.ExperimentStatus.FINISHED)) {
      experiment.setStatus(status);
      assertThat(checker.isCorrectionRequested(93090L)).isFalse();
    }
    experiment.setStatus(null);
    assertThat(checker.isCorrectionRequested(93090L)).isFalse();
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.PLANNED);
    given(finalReview.correctionRequested(video)).willReturn(false);
    assertThat(checker.isCorrectionRequested(93090L)).isFalse();
    given(repository.findBySalesVideoJobId(93090L)).willReturn(List.of());
    assertThat(checker.isCorrectionRequested(93090L)).isFalse();
    assertThat(checker.isCorrectionRequested(null)).isFalse();
  }
}
