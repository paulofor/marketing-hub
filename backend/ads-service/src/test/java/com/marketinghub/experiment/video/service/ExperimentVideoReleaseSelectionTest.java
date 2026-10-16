package com.marketinghub.experiment.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleVideoBinding;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.video.*;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Responsabilidade: prevenir bloqueio por versões substituídas sem relaxar os gates vigentes. */
class ExperimentVideoReleaseSelectionTest {
  private final ObjectMapper json = new ObjectMapper();
  private final LearningCycleVideoBinding binding = mock(LearningCycleVideoBinding.class);
  private final ExperimentVideoAssetRepository videos = mock(ExperimentVideoAssetRepository.class);
  private final ExperimentVideoReleaseSelection selection =
      new ExperimentVideoReleaseSelection(binding, videos);
  private ObjectNode receipt;
  private Experiment experiment;
  private ExperimentVideoAsset ad;
  private ExperimentVideoAsset hero;
  private List<ExperimentVideoAsset> assets;

  /** Prepara o mesmo padrão histórico de rejeições e duas peças explicitamente integradas. */
  @BeforeEach
  void setup() {
    configure(105L, 76L, 75L);
  }

  /** Muda todos os identificadores para exercitar isolamento sem exceções por produto. */
  private void configure(long experimentId, long adId, long heroId) {
    experiment = Experiment.builder().id(experimentId).build();
    ad = asset(adId, ExperimentVideoSlot.AD, ExperimentVideoReviewStatus.APPROVED);
    hero = asset(heroId, ExperimentVideoSlot.LANDING_HERO, ExperimentVideoReviewStatus.APPROVED);
    assets = new ArrayList<>(List.of(ad, hero));
    assets.add(asset(adId - 30, ExperimentVideoSlot.AD, ExperimentVideoReviewStatus.REJECTED));
    assets.add(
        asset(heroId - 30, ExperimentVideoSlot.LANDING_HERO, ExperimentVideoReviewStatus.PENDING));
    receipt =
        json.createObjectNode()
            .put("evidenceType", LearningCycleVideoBinding.CONTRACT)
            .put("experimentId", experimentId);
    receipt.set("campaignVideo", media(ad));
    receipt.set("heroVideo", media(hero));
    when(binding.forReference("experiment:" + experimentId)).thenReturn(Optional.of(receipt));
    when(videos.findByExperimentIdOrderByCreatedAtDesc(experimentId)).thenReturn(assets);
  }

  /** Cria um registro obrigatório com estados independentes de produção e revisão humana. */
  private ExperimentVideoAsset asset(
      long id, ExperimentVideoSlot role, ExperimentVideoReviewStatus status) {
    return ExperimentVideoAsset.builder()
        .id(id)
        .experiment(experiment)
        .slot(role)
        .requiredForRelease(true)
        .status(ExperimentVideoStatus.READY)
        .reviewStatus(status)
        .assetUrl("https://fixture.invalid/" + id + ".mp4")
        .build();
  }

  /** Representa os campos de mídia lidos do recibo canônico, sem escolher por data ou nome. */
  private ObjectNode media(ExperimentVideoAsset asset) {
    return json.createObjectNode()
        .put("assetId", asset.getId())
        .put("assetUrl", asset.getAssetUrl())
        .put("role", asset.getSlot().name())
        .put("reviewStatus", "APPROVED");
  }

  /** Libera somente o requisito audiovisual atual em dois contextos, mantendo rejeições e flags. */
  @ParameterizedTest
  @CsvSource({"105,76,75", "8812,9802,9803"})
  void ignoresOnlyHistoricalVersionsOfExplicitlySelectedRoles(
      long experimentId, long adId, long heroId) {
    configure(experimentId, adId, heroId);
    assertThat(selection.blocksRelease(experimentId)).contains(false);
    assertThat(assets).allMatch(ExperimentVideoAsset::isRequiredForRelease);
    assertThat(assets.get(2).getReviewStatus()).isEqualTo(ExperimentVideoReviewStatus.REJECTED);
    verify(videos, never()).save(any());
  }

  /** Mantém o caminho anterior de experimento sem integração registrada. */
  @Test
  void delegatesToLegacyPolicyWithoutReceipt() {
    when(binding.forReference("experiment:105")).thenReturn(Optional.empty());
    assertThat(selection.blocksRelease(105L)).isEmpty();
    verify(videos, never()).findByExperimentIdOrderByCreatedAtDesc(anyLong());
  }

  /** Uma reprovação atual continua bloqueante mesmo quando existem versões antigas aprovadas. */
  @Test
  void blocksRevokedApproval() {
    ad.setReviewStatus(ExperimentVideoReviewStatus.REJECTED);
    assets.get(2).setReviewStatus(ExperimentVideoReviewStatus.APPROVED);
    assertThat(selection.blocksRelease(105L)).contains(true);
  }

  /** Um slot adicional obrigatório nunca é dispensado pela escolha dos dois vídeos do ciclo. */
  @Test
  void preservesOtherRequiredSlots() {
    var extra = asset(99L, ExperimentVideoSlot.PRE_CHECKOUT, ExperimentVideoReviewStatus.PENDING);
    assets.add(extra);
    assertThat(selection.blocksRelease(105L)).contains(true);
    extra.setReviewStatus(ExperimentVideoReviewStatus.APPROVED);
    assertThat(selection.blocksRelease(105L)).contains(false);
  }

  /** Ausência, identidade cruzada, papel ou arquivo divergente impedem a aprovação do requisito. */
  @Test
  void blocksMissingOrCrossContextMedia() {
    assets.remove(hero);
    assertThat(selection.blocksRelease(105L)).contains(true);
    assets.add(hero);
    hero.setExperiment(Experiment.builder().id(9000L).build());
    assertThat(selection.blocksRelease(105L)).contains(true);
    hero.setExperiment(experiment);
    hero.setSlot(ExperimentVideoSlot.AD);
    assertThat(selection.blocksRelease(105L)).contains(true);
    hero.setSlot(ExperimentVideoSlot.LANDING_HERO);
    hero.setAssetUrl("https://fixture.invalid/changed.mp4");
    assertThat(selection.blocksRelease(105L)).contains(true);
  }

  /** Recibo de outra identidade ou falha de revalidação nunca libera tráfego por omissão. */
  @Test
  void blocksInvalidReceiptAndBindingFailure() {
    receipt.put("experimentId", 9999);
    assertThat(selection.blocksRelease(105L)).contains(true);
    when(binding.forReference("experiment:105"))
        .thenThrow(new IllegalStateException("Recibo alterado"));
    assertThat(selection.blocksRelease(105L)).contains(true);
  }
}
