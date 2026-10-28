package com.marketinghub.creative.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.marketinghub.agenttask.CreativeFinalVideoEvidence;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.service.videoreview.*;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.video.*;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Protege os estados da fila e impede carregar auditorias históricas nos alertas globais. */
@ExtendWith(MockitoExtension.class)
class VideoReviewSummaryTest {
  @Mock CreativeRepository creatives;
  @Mock ExperimentVideoAssetRepository videos;
  @Mock CreativeFinalVideoEvidence evidence;
  @InjectMocks CreativeService service;

  /** Injeta o gate opcional como faz o Spring após construir o serviço. */
  @org.junit.jupiter.api.BeforeEach
  void injectIndependentReviewGate() {
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "finalVideoEvidence", evidence);
  }

  /** Mantém o custo do resumo independente da quantidade de auditorias encerradas. */
  @Test
  void terminalAndHistoricalVideosDoNotLoadFullAssetsOrReviewEvidence() {
    var rows = new ArrayList<ExperimentVideoSummarySnapshot>();
    for (long id = 1; id <= 500; id++) {
      rows.add(
          new ExperimentVideoSummarySnapshot(
              id, ExperimentStatus.FINISHED, ExperimentVideoReviewStatus.PENDING, true));
    }
    rows.add(
        new ExperimentVideoSummarySnapshot(
            501L, ExperimentStatus.FINISHED, ExperimentVideoReviewStatus.APPROVED, true));
    rows.add(
        new ExperimentVideoSummarySnapshot(
            502L, ExperimentStatus.FINISHED, ExperimentVideoReviewStatus.REJECTED, true));
    when(videos.findVideoSummarySnapshots(null, null)).thenReturn(rows);

    var summary = service.videoReviewSummary(null, null);

    assertThat(summary.historicalCount()).isEqualTo(500);
    assertThat(summary.approvedCount()).isOne();
    assertThat(summary.rejectedCount()).isOne();
    assertThat(summary.awaitingReviewCount()).isZero();
    verify(videos, never()).findById(anyLong());
    verifyNoInteractions(evidence);
    verify(videos, never()).findReadyExperimentVideosForReview(any(), any(), any(), any());
    verify(creatives, never()).findVideoCreativesForReview(any(), any(), any());
  }

  /** Revalida cada decisão atual uma vez, preservando gate independente e peças opcionais. */
  @Test
  void currentRequiredOptionalAndBlockedVideosKeepTheirRealGates() {
    var required = video(41L, true);
    var optional = video(42L, false);
    var blocked = video(43L, true);
    var assets = List.of(required, optional, blocked);
    when(videos.findVideoSummarySnapshots(12L, 701L))
        .thenReturn(
            assets.stream()
                .map(
                    v ->
                        new ExperimentVideoSummarySnapshot(
                            v.getId(),
                            ExperimentStatus.PLANNED,
                            ExperimentVideoReviewStatus.PENDING,
                            v.isRequiredForRelease()))
                .toList());
    for (var asset : assets) when(videos.findById(asset.getId())).thenReturn(Optional.of(asset));
    when(evidence.approvalBlockReason(any()))
        .thenAnswer(
            invocation ->
                invocation.getArgument(0) == blocked
                    ? "Psique precisa revisar o hash atual."
                    : null);

    var summary = service.videoReviewSummary(12L, 701L);

    assertThat(summary.awaitingReviewCount()).isOne();
    assertThat(summary.optionalReviewCount()).isOne();
    assertThat(summary.blockedCount()).isOne();
    assertThat(summary.productId()).isEqualTo(12L);
    assertThat(summary.experimentId()).isEqualTo(701L);
    for (var asset : assets) verify(evidence).approvalBlockReason(asset);
    verify(creatives).findVideoSummarySnapshots(12L, 701L);
    verifyNoMoreInteractions(evidence);
  }

  /** Preserva ancestrais, decisões finais e isolamento mesmo com elo intermediário sem vídeo. */
  @Test
  void creativeLineageAndCopyRulesRemainIdentical() {
    when(creatives.findVideoSummarySnapshots(28L, null))
        .thenReturn(
            List.of(
                row(1, 201, null, CreativeStatus.DRAFT, CreativeAgentReviewStatus.PENDING, true),
                row(2, 201, 1L, CreativeStatus.DRAFT, null, false),
                row(3, 201, 2L, CreativeStatus.READY, CreativeAgentReviewStatus.APPROVED, true),
                row(4, 202, 3L, CreativeStatus.DRAFT, null, true),
                row(5, 202, null, CreativeStatus.REJECTED, CreativeAgentReviewStatus.FAILED, true),
                new CreativeVideoSummarySnapshot(
                    6L,
                    202L,
                    ExperimentStatus.PLANNED,
                    null,
                    CreativeStatus.DRAFT,
                    null,
                    "x".repeat(126),
                    "Amostra",
                    "Veja",
                    true),
                row(7, 202, null, CreativeStatus.DRAFT, CreativeAgentReviewStatus.ADJUST, true)));

    var summary = service.videoReviewSummary(28L, null);

    assertThat(summary.historicalCount()).isOne();
    assertThat(summary.approvedCount()).isOne();
    assertThat(summary.rejectedCount()).isOne();
    assertThat(summary.awaitingReviewCount()).isOne();
    assertThat(summary.blockedCount()).isEqualTo(2);
    verifyNoInteractions(evidence);
  }

  /** Produz um vídeo sintético publicável, sem acessar provedores nem gerar custos. */
  private ExperimentVideoAsset video(long id, boolean required) {
    return ExperimentVideoAsset.builder()
        .id(id)
        .experiment(Experiment.builder().id(701L).status(ExperimentStatus.PLANNED).build())
        .status(ExperimentVideoStatus.READY)
        .reviewStatus(ExperimentVideoReviewStatus.PENDING)
        .assetUrl("https://media.test/" + id + ".mp4")
        .hasAudio(true)
        .requiredForRelease(required)
        .build();
  }

  /** Cria um nó leve da linhagem para validar a classificação compartilhada. */
  private CreativeVideoSummarySnapshot row(
      long id,
      long experimentId,
      Long parent,
      CreativeStatus status,
      CreativeAgentReviewStatus review,
      boolean eligible) {
    return new CreativeVideoSummarySnapshot(
        id,
        experimentId,
        ExperimentStatus.PLANNED,
        parent,
        status,
        review,
        "Veja a amostra",
        "Amostra",
        "Personalizada",
        eligible);
  }
}
