package com.marketinghub.creative.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.Creative;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProviderModelRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoProviderModel;
import com.marketinghub.salesvideo.VideoProject;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar a prova de direitos ligada ao arquivo final de um anúncio em vídeo. */
@ExtendWith(MockitoExtension.class)
class CreativeMediaGovernanceEvidenceServiceTest {
  private static final String FINAL_URL = "https://cdn.test/vega91-final.mp4";
  private static final String PRESENTER_URL = "https://cdn.test/presenter.png";

  @Mock ExperimentVideoAssetRepository videoAssets;
  @Mock VideoProjectRepository videoProjects;
  @Mock AssetRepository assets;
  @Mock SalesVideoProviderModelRepository providerModels;

  private CreativeMediaGovernanceEvidenceService service;

  /** Monta o resolvedor com serialização real e portas de persistência controladas. */
  @BeforeEach
  void setup() {
    service =
        new CreativeMediaGovernanceEvidenceService(
            videoAssets, videoProjects, assets, providerModels, new ObjectMapper());
  }

  /** Expõe a cadeia completa somente quando arquivo, projeto, referência e licença convergem. */
  @Test
  void resolvesVerifiedEvidenceForExactApprovedVideo() throws Exception {
    Experiment experiment = Experiment.builder().id(91L).build();
    Creative creative =
        Creative.builder()
            .id(524L)
            .experiment(experiment)
            .format("VIDEO")
            .videoUrl(FINAL_URL)
            .build();
    Asset finalAsset =
        asset(
            2780L,
            FINAL_URL,
            MediaProvider.VIDEO_MODULE,
            payload("MUSA_POST_PRODUCTION", "post-production-21234", "a".repeat(64), 2772L));
    Asset sourceAsset =
        asset(
            2772L,
            "https://cdn.test/runway-raw.mp4",
            MediaProvider.VIDEO_MODULE,
            payload("RUNWAY_PRODUCT_UGC", "runway-task-1", "b".repeat(64), null));
    Asset presenter =
        asset(
            1925L, PRESENTER_URL, MediaProvider.OPENAI, "{\"generation_job_id\":\"image-job-1\"}");
    presenter.setModel("gpt-image-2");
    presenter.setPrompt("Crie uma personagem adulta sintética sem copiar uma pessoa real.");
    SalesVideoJob job = new SalesVideoJob();
    job.setId(21234L);
    ExperimentVideoAsset video =
        ExperimentVideoAsset.builder()
            .id(38L)
            .experiment(experiment)
            .slot(ExperimentVideoSlot.AD)
            .objective("Venda")
            .primaryMetric("purchase")
            .provider("MUSA_POST_PRODUCTION")
            .model("gpt-4o-mini-tts")
            .status(ExperimentVideoStatus.READY)
            .reviewStatus(ExperimentVideoReviewStatus.APPROVED)
            .assetUrl(FINAL_URL)
            .asset(finalAsset)
            .salesVideoJob(job)
            .requestJson(requestJson())
            .reviewedBy("Marketing Hub")
            .reviewedAt(Instant.parse("2026-09-05T09:40:16Z"))
            .build();
    VideoProject project =
        VideoProject.builder()
            .id(3L)
            .experimentId(91L)
            .characterPerformanceUri(PRESENTER_URL)
            .referencePerformanceUri("https://product.test/reference.png")
            .build();
    SalesVideoProviderModel provider = new SalesVideoProviderModel();
    provider.setCode("runway-product-ugc-2026-06");
    provider.setProviderName("RUNWAY_PRODUCT_UGC");
    provider.setCommercialLicenseVerified(true);
    provider.setUpdatedAt(Instant.parse("2026-09-04T00:00:00Z"));
    when(videoAssets.findFirstByExperimentIdAndAssetUrlOrderByIdDesc(91L, FINAL_URL))
        .thenReturn(Optional.of(video));
    when(videoProjects.findById(3L)).thenReturn(Optional.of(project));
    when(assets.findById(2772L)).thenReturn(Optional.of(sourceAsset));
    when(assets.findByUrlIn(List.of(PRESENTER_URL))).thenReturn(List.of(presenter));
    when(providerModels.findByProviderName("RUNWAY_PRODUCT_UGC")).thenReturn(Optional.of(provider));

    var evidence = service.resolve(creative);

    assertThat(evidence.verificationStatus()).isEqualTo("VERIFIED");
    assertThat(evidence.experimentVideoAssetId()).isEqualTo(38L);
    assertThat(evidence.salesVideoJobId()).isEqualTo(21234L);
    assertThat(evidence.finalArtifact().url()).isEqualTo(FINAL_URL);
    assertThat(evidence.finalArtifact().sha256()).isEqualTo("a".repeat(64));
    assertThat(evidence.generatedSourceArtifact().providerTaskId()).isEqualTo("runway-task-1");
    assertThat(evidence.presenterReference().provider()).isEqualTo("OPENAI");
    assertThat(evidence.presenterReference().generationReference()).isEqualTo("image-job-1");
    assertThat(evidence.presenterConsentEvidence()).contains("nenhuma pessoa real");
    assertThat(evidence.referenceRightsEvidence()).contains("script versionado");
    assertThat(evidence.providerLicense().commercialLicenseVerified()).isTrue();
    assertThat(evidence.providerLicense().evidenceUrl())
        .isEqualTo(CreativeMediaGovernanceEvidenceService.RUNWAY_COMMERCIAL_USE_POLICY_URL);
  }

  /** Mantém mídia de imagem fora do contrato específico de linhagem audiovisual. */
  @Test
  void ignoresImageCreative() {
    Creative creative = Creative.builder().format("IMAGE").build();

    assertThat(service.resolve(creative)).isNull();
    verifyNoInteractions(videoAssets, videoProjects, assets, providerModels);
  }

  /** Bloqueia vídeo que não corresponde exatamente a um ativo aprovado do mesmo experimento. */
  @Test
  void marksVideoWithoutExactAssetAsUnavailable() {
    Experiment experiment = Experiment.builder().id(91L).build();
    Creative creative =
        Creative.builder().experiment(experiment).format("VIDEO").videoUrl(FINAL_URL).build();
    when(videoAssets.findFirstByExperimentIdAndAssetUrlOrderByIdDesc(91L, FINAL_URL))
        .thenReturn(Optional.empty());

    var evidence = service.resolve(creative);

    assertThat(evidence.verificationStatus()).isEqualTo("NO_APPROVED_ASSET_MATCH");
    assertThat(evidence.finalArtifact().url()).isEqualTo(FINAL_URL);
    assertThat(evidence.providerLicense()).isNull();
  }

  /** Bloqueia auditoria corrompida sem transformar a revisão em aprovação ou falha não tratada. */
  @Test
  void marksMalformedAuditAsInvalid() {
    Experiment experiment = Experiment.builder().id(91L).build();
    Creative creative =
        Creative.builder()
            .id(524L)
            .experiment(experiment)
            .format("VIDEO")
            .videoUrl(FINAL_URL)
            .build();
    ExperimentVideoAsset video =
        ExperimentVideoAsset.builder().id(38L).experiment(experiment).requestJson("{").build();
    when(videoAssets.findFirstByExperimentIdAndAssetUrlOrderByIdDesc(91L, FINAL_URL))
        .thenReturn(Optional.of(video));

    var evidence = service.resolve(creative);

    assertThat(evidence.verificationStatus()).isEqualTo("INVALID_AUDIT_PAYLOAD");
    assertThat(evidence.finalArtifact().url()).isEqualTo(FINAL_URL);
  }

  /** Cria um asset mínimo com identidade e payload auditável. */
  private Asset asset(Long id, String url, MediaProvider provider, String payload) {
    return Asset.builder()
        .id(id)
        .type(url.endsWith(".png") ? AssetType.IMAGE : AssetType.VIDEO)
        .provider(provider)
        .status(AssetStatus.READY)
        .url(url)
        .payload(payload)
        .build();
  }

  /** Serializa os metadados imutáveis armazenados junto ao arquivo. */
  private String payload(String provider, String providerTaskId, String sha256, Long sourceAssetId)
      throws Exception {
    var metadata = new java.util.LinkedHashMap<String, Object>();
    metadata.put("provider", provider);
    metadata.put("provider_job_id", providerTaskId);
    metadata.put("sha256", sha256);
    if (sourceAssetId != null) {
      metadata.put("source_asset_id", sourceAssetId);
    }
    return new ObjectMapper().writeValueAsString(java.util.Map.of("metadata", metadata));
  }

  /** Reproduz o snapshot mínimo da pós-produção premium persistido em produção. */
  private String requestJson() throws Exception {
    var governance =
        java.util.Map.of(
            "presenterConsentEvidence",
            "Referência sintética aprovada; nenhuma pessoa real é representada.",
            "referenceRightsEvidence",
            "Referência aprovada e tela gerada por script versionado.",
            "productIsDigitalExperience",
            true);
    var metadata = new java.util.LinkedHashMap<String, Object>();
    metadata.put("videoProjectId", 3);
    metadata.put("generation_strategy", "RUNWAY_PRODUCT_UGC_WITH_DETERMINISTIC_POST_PRODUCTION");
    metadata.put("sourceAssetId", 2772);
    metadata.put("sourceProviderName", "RUNWAY_PRODUCT_UGC");
    metadata.put("referenceGovernance", governance);
    String nested = new ObjectMapper().writeValueAsString(metadata);
    return new ObjectMapper()
        .writeValueAsString(java.util.Map.of("postProductionMetadataJson", nested));
  }
}
