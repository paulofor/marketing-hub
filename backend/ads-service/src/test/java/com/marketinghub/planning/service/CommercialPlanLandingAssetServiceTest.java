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
        new CommercialPlanLandingAssetService(
            planRepository,
            assetRepository,
            mock(
                com.marketinghub.repository.jpa.planning.CommercialPlanImageStudioJobRepository
                    .class),
            new com.fasterxml.jackson.databind.ObjectMapper());

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
        new CommercialPlanLandingAssetService(
            planRepository,
            assetRepository,
            mock(
                com.marketinghub.repository.jpa.planning.CommercialPlanImageStudioJobRepository
                    .class),
            new com.fasterxml.jackson.databind.ObjectMapper());

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
        new CommercialPlanLandingAssetService(
            planRepository,
            assetRepository,
            mock(
                com.marketinghub.repository.jpa.planning.CommercialPlanImageStudioJobRepository
                    .class),
            new com.fasterxml.jackson.databind.ObjectMapper());
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

  /** Entrega direitos, fonte e hash já auditados, sem expor prompts nem binários ao revisor. */
  @Test
  void includesRecordedRightsAndGenerationProvenanceForLegacyAssets() {
    var plans = mock(CommercialPlanRepository.class);
    var assets = mock(CommercialPlanVisualAssetRepository.class);
    var jobs =
        mock(com.marketinghub.repository.jpa.planning.CommercialPlanImageStudioJobRepository.class);
    var plan = CommercialPlan.builder().id(41L).name("Produto independente").build();
    var approved = asset(901L, "https://assets.example/approved.png", "PRODUCT_PROOF", true);
    approved.setOrigin("Geração própria por IA");
    approved.setRightsStatement("Uso comercial autorizado pela operação");
    approved.setReviewerExecutionId("review-independent");
    var source = asset(899L, "https://assets.example/source.png", "PRODUCT_PROOF", true);
    source.setOrigin("Biblioteca produzida pela operação");
    source.setRightsStatement("Fonte autorizada para compor o produto");
    var job = new com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioJob();
    job.setId(701L);
    job.setResultVisualAsset(approved);
    job.setSourceVisualAsset(source);
    job.setStatus(
        com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioStatus.COMPLETED);
    job.setOperation(
        com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioOperation.EDIT);
    job.setModel("model-recorded");
    job.setProducerExecutionId("production-independent");
    job.setRequestJson("{\"prompt\":\"private-request-content\"}");
    job.setResponseJson(
        "{\"data\":[{\"image_sha256\":\""
            + "a".repeat(64)
            + "\",\"b64_json\":\"private-image-bytes\"}]}");
    when(plans.findByExperimentReference(731L)).thenReturn(List.of(plan));
    when(assets.findByCommercialPlanIdAndStatusOrderByCreatedAtAsc(
            41L, CommercialPlanVisualAssetStatus.APPROVED))
        .thenReturn(List.of(approved));
    when(jobs.findByResultVisualAssetIdInAndStatusOrderByIdAsc(List.of(901L), job.getStatus()))
        .thenReturn(List.of(job));
    var payload =
        new CommercialPlanLandingAssetService(
                plans, assets, jobs, new com.fasterxml.jackson.databind.ObjectMapper())
            .payloadForExperiment(731L);
    var proof =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .valueToTree(payload)
            .get(0)
            .path("provenance");
    assertThat(proof.path("rightsStatement").asText()).isEqualTo(approved.getRightsStatement());
    assertThat(proof.path("contentSha256").asText()).isEqualTo("a".repeat(64));
    assertThat(proof.path("generationAudit").path("sourceAssetId").asLong()).isEqualTo(899L);
    assertThat(proof.path("generationAudit").path("jobId").asLong()).isEqualTo(701L);
    assertThat(proof.path("generationAudit").path("model").asText()).isEqualTo("model-recorded");
    assertThat(proof.path("generationAudit").path("registeredHashMatchesAudit").asBoolean())
        .isTrue();
    assertThat(payload.toString()).doesNotContain("private-request-content", "private-image-bytes");
    assertThat(proof.path("limitations").asText()).contains("não é licença");
  }

  /** Mantém lacunas de direitos e hash explícitas, mesmo para uma imagem visualmente aprovada. */
  @Test
  void doesNotInventRightsOrHashesFromVisualApproval() {
    var plans = mock(CommercialPlanRepository.class);
    var assets = mock(CommercialPlanVisualAssetRepository.class);
    var jobs =
        mock(com.marketinghub.repository.jpa.planning.CommercialPlanImageStudioJobRepository.class);
    var plan = CommercialPlan.builder().id(64L).name("Outro produto").build();
    var approved = asset(844L, "https://assets.example/legacy.png", "LANDING", true);
    when(plans.findByExperimentReference(732L)).thenReturn(List.of(plan));
    when(assets.findByCommercialPlanIdAndStatusOrderByCreatedAtAsc(
            64L, CommercialPlanVisualAssetStatus.APPROVED))
        .thenReturn(List.of(approved));
    var service =
        new CommercialPlanLandingAssetService(
            plans, assets, jobs, new com.fasterxml.jackson.databind.ObjectMapper());
    var proof =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .valueToTree(service.payloadForExperiment(732L))
            .get(0)
            .path("provenance");
    assertThat(proof.path("origin").asText()).isEmpty();
    assertThat(proof.path("rightsStatement").asText()).isEmpty();
    assertThat(proof.path("contentSha256").asText()).isEmpty();
    assertThat(proof.path("generationAuditAvailable").asBoolean()).isFalse();
    var job = new com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioJob();
    job.setId(981L);
    job.setResultVisualAsset(approved);
    job.setStatus(
        com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioStatus.COMPLETED);
    job.setResponseJson("invalid audit");
    when(jobs.findByResultVisualAssetIdInAndStatusOrderByIdAsc(List.of(844L), job.getStatus()))
        .thenReturn(List.of(job));
    proof =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .valueToTree(service.payloadForExperiment(732L))
            .get(0)
            .path("provenance");
    assertThat(proof.path("hashAvailable").asBoolean()).isFalse();
    approved.setContentSha256("b".repeat(64));
    job.setResponseJson("{\"data\":[{\"image_sha256\":\"" + "a".repeat(64) + "\"}]}");
    proof =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .valueToTree(service.payloadForExperiment(732L))
            .get(0)
            .path("provenance");
    assertThat(proof.path("generationAudit").path("registeredHashMatchesAudit").asBoolean())
        .isFalse();
    assertThat(proof.path("contentSha256").asText()).isEqualTo("b".repeat(64));
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
