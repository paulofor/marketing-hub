package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Valida a linhagem obrigatoria entre a landing e os arquivos premium aprovados do produto. */
class CommercialPlanLandingAssetServiceTest {
  /** Deve selecionar somente arquivos aprovados independentemente e destinados a landing. */
  @Test
  void selectsOnlyIndependentlyApprovedLandingAssets() {
    CommercialPlanRepository planRepository = mock(CommercialPlanRepository.class);
    CommercialPlanVisualAssetRepository assetRepository =
        mock(CommercialPlanVisualAssetRepository.class);
    CommercialPlan plan = CommercialPlan.builder().id(2L).name("Agenda Cheia").build();
    when(planRepository.findByExperimentReference(88L)).thenReturn(List.of(plan));
    when(assetRepository.findByCommercialPlanIdAndStatusOrderByCreatedAtAsc(
            2L, CommercialPlanVisualAssetStatus.APPROVED))
        .thenReturn(
            List.of(
                asset(133L, "https://assets.example/post-01.png", "LANDING", true),
                asset(134L, "https://assets.example/story-01.png", "PRODUCT_PROOF", true),
                asset(135L, "https://assets.example/rejected.png", "LANDING", false)));

    CommercialPlanLandingAssetService service =
        new CommercialPlanLandingAssetService(planRepository, assetRepository);

    assertThat(service.referencesForExperiment(88L))
        .extracting(CommercialPlanLandingAssetService.LandingAssetReference::assetId)
        .containsExactly(133L, 134L);
    assertThat(service.requiredReferenceCount(88L)).isEqualTo(2);
  }

  /** Deve bloquear a landing quando nenhuma prova aprovada foi disponibilizada. */
  @Test
  void blocksEmptyApprovedEvidenceInsteadOfDisablingTheGate() {
    CommercialPlanRepository planRepository = mock(CommercialPlanRepository.class);
    CommercialPlanVisualAssetRepository assetRepository =
        mock(CommercialPlanVisualAssetRepository.class);
    CommercialPlan plan = CommercialPlan.builder().id(4L).name("Rigel").build();
    when(planRepository.findByExperimentReference(89L)).thenReturn(List.of(plan));
    when(assetRepository.findByCommercialPlanIdAndStatusOrderByCreatedAtAsc(
            4L, CommercialPlanVisualAssetStatus.APPROVED))
        .thenReturn(List.of());
    CommercialPlanLandingAssetService service =
        new CommercialPlanLandingAssetService(planRepository, assetRepository);

    assertThat(service.requiredReferenceCount(89L)).isEqualTo(1);
    assertThat(service.hasRequiredApprovedAssetReferences(89L, "<main>Sem prova</main>")).isFalse();
    assertThatThrownBy(() -> service.validateApprovedAssetReferences(89L, "<main>Sem prova</main>"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("1 arquivo APPROVED");
  }

  /** Deve exigir quatro URLs exatas quando a biblioteca possui ao menos quatro provas aprovadas. */
  @Test
  void validatesFourExactApprovedUrlsWithoutRedrawing() {
    CommercialPlanRepository planRepository = mock(CommercialPlanRepository.class);
    CommercialPlanVisualAssetRepository assetRepository =
        mock(CommercialPlanVisualAssetRepository.class);
    CommercialPlan plan = CommercialPlan.builder().id(2L).name("Agenda Cheia").build();
    when(planRepository.findByExperimentReference(88L)).thenReturn(List.of(plan));
    when(assetRepository.findByCommercialPlanIdAndStatusOrderByCreatedAtAsc(
            2L, CommercialPlanVisualAssetStatus.APPROVED))
        .thenReturn(
            List.of(
                asset(1L, "https://assets.example/1.png", "LANDING", true),
                asset(2L, "https://assets.example/2.png", "LANDING", true),
                asset(3L, "https://assets.example/3.png", "LANDING", true),
                asset(4L, "https://assets.example/4.png", "LANDING", true),
                asset(5L, "https://assets.example/5.png", "LANDING", true)));
    CommercialPlanLandingAssetService service =
        new CommercialPlanLandingAssetService(planRepository, assetRepository);
    String validHtml =
        "<img src='https://assets.example/1.png'><img src='https://assets.example/2.png'>"
            + "<img src='https://assets.example/3.png'><img src='https://assets.example/4.png'>";
    String invalidHtml = validHtml.replace("https://assets.example/4.png", "generated-placeholder");
    String hiddenUrlHtml =
        "<div data-approved='https://assets.example/1.png https://assets.example/2.png "
            + "https://assets.example/3.png https://assets.example/4.png'>Sem imagens reais</div>";

    assertThat(service.hasRequiredApprovedAssetReferences(88L, validHtml)).isTrue();
    assertThat(service.hasRequiredApprovedAssetReferences(88L, invalidHtml)).isFalse();
    assertThat(service.hasRequiredApprovedAssetReferences(88L, hiddenUrlHtml)).isFalse();
    assertThatThrownBy(() -> service.validateApprovedAssetReferences(88L, invalidHtml))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("4 arquivos APPROVED");
  }

  /** Cria uma referencia visual com os campos usados pelo contrato de landing. */
  private CommercialPlanVisualAsset asset(
      Long id, String url, String purpose, boolean independentlyApproved) {
    CommercialPlanVisualAsset asset = new CommercialPlanVisualAsset();
    asset.setId(id);
    asset.setAssetUrl(url);
    asset.setLabel("Entregavel " + id);
    asset.setVersionNumber(1);
    asset.setPurpose(purpose);
    asset.setPurposesJson("[\"" + purpose + "\"]");
    asset.setStatus(CommercialPlanVisualAssetStatus.APPROVED);
    asset.setAgentReviewStatus(
        independentlyApproved
            ? CommercialPlanVisualAssetReviewStatus.APPROVED
            : CommercialPlanVisualAssetReviewStatus.ADJUST);
    return asset;
  }
}
