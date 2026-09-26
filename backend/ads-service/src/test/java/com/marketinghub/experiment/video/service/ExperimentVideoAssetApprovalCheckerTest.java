package com.marketinghub.experiment.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
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

  /** Libera substituição somente quando existe ativo e todos foram explicitamente reprovados. */
  @Test
  void shouldRequireAllJobAssetsRejectedForReplacement() {
    var checker = new ExperimentVideoAssetApprovalChecker(repository);
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
}
