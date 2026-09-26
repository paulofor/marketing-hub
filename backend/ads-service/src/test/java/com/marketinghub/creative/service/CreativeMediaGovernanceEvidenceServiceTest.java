package com.marketinghub.creative.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
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
  @Mock CreativeRepository creatives;

  private CreativeMediaGovernanceEvidenceService service;

  /** Monta o resolvedor com serialização real e portas de persistência controladas. */
  @BeforeEach
  void setup() {
    service =
        new CreativeMediaGovernanceEvidenceService(
            videoAssets, videoProjects, assets, providerModels, creatives, new ObjectMapper());
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
    provider.setLifecycleStatus("ACTIVE");
    provider.setAdapterVerified(true);
    provider.setQualityGateVerified(true);
    provider.setCommercialLicenseVerified(true);
    provider.setUpdatedAt(Instant.parse("2026-09-04T00:00:00Z"));
    when(videoAssets.findFirstByExperimentIdAndAssetUrlOrderByIdDesc(91L, FINAL_URL))
        .thenReturn(Optional.of(video));
    when(videoProjects.findById(3L)).thenReturn(Optional.of(project));
    when(assets.findById(2772L)).thenReturn(Optional.of(sourceAsset));
    when(assets.findByUrlIn(List.of(PRESENTER_URL))).thenReturn(List.of(presenter));
    when(providerModels.findByProviderName("RUNWAY_PRODUCT_UGC")).thenReturn(Optional.of(provider));

    var evidence = service.resolve(creative);

    assertThat(evidence.contractVersion()).isEqualTo("CREATIVE_MEDIA_GOVERNANCE_V2");
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
    assertThat(evidence.presenterReferenceMode()).isEqualTo("EXPLICIT_REFERENCE");
    assertThat(evidence.syntheticMediaDisclosureVerified()).isFalse();
    assertThat(evidence.providerLicense().commercialLicenseVerified()).isTrue();
    assertThat(evidence.providerLicense().evidenceUrl())
        .isEqualTo(CreativeMediaGovernanceEvidenceService.RUNWAY_COMMERCIAL_USE_POLICY_URL);
  }

  /** Aprova montagem versionada somente quando o arquivo e todas as fontes convergem no banco. */
  @Test
  void resolvesVersionedMontageFromApprovedCreativeSources() throws Exception {
    Experiment sourceExperiment = Experiment.builder().id(88L).build();
    Experiment experiment = Experiment.builder().id(94L).sourceExperiment(sourceExperiment).build();
    String finalUrl = "https://cdn.test/capella-montage.mp4";
    Creative creative =
        Creative.builder()
            .id(532L)
            .experiment(experiment)
            .format("VIDEO")
            .videoUrl(finalUrl)
            .build();
    Asset finalAsset =
        asset(
            2824L,
            finalUrl,
            MediaProvider.USER_UPLOAD,
            "{\"metadata\":{\"sha256\":\"" + "c".repeat(64) + "\"}}");
    Instant reviewedAt = Instant.parse("2026-09-24T12:00:00Z");
    Creative sourceCreative =
        Creative.builder()
            .id(522L)
            .experiment(sourceExperiment)
            .format("IMAGE")
            .imageUrl("https://cdn.test/capella-post.png")
            .status(CreativeStatus.READY)
            .agentReviewStatus(CreativeAgentReviewStatus.APPROVED)
            .reviewedAt(reviewedAt)
            .build();
    ExperimentVideoAsset video =
        ExperimentVideoAsset.builder()
            .id(44L)
            .experiment(experiment)
            .provider("USER_UPLOAD")
            .model("VERSIONED_FFMPEG_MONTAGE_V1")
            .status(ExperimentVideoStatus.READY)
            .reviewStatus(ExperimentVideoReviewStatus.APPROVED)
            .assetUrl(finalUrl)
            .asset(finalAsset)
            .requestJson(
                """
                {"artifactType":"experiment.userAdVideoUpload.v2",
                 "generationStrategy":"VERSIONED_APPROVED_CREATIVE_MONTAGE",
                 "productionReference":"scripts/marketing/create-capella-successor-video-v1.sh",
                 "approvedSourceCreatives":[{"creativeId":522,"experimentId":88,
                   "format":"IMAGE","mediaUrl":"https://cdn.test/capella-post.png",
                   "status":"READY","agentReviewStatus":"APPROVED",
                   "reviewedAt":1790251200.000000000}]}
                """)
            .reviewedBy("time@marketinghub.io")
            .reviewedAt(Instant.parse("2026-09-26T03:00:00Z"))
            .build();
    when(videoAssets.findFirstByExperimentIdAndAssetUrlOrderByIdDesc(94L, finalUrl))
        .thenReturn(Optional.of(video));
    when(creatives.findByIdWithExperiment(522L)).thenReturn(Optional.of(sourceCreative));

    var evidence = service.resolve(creative);

    assertThat(evidence.contractVersion()).isEqualTo("CREATIVE_MEDIA_GOVERNANCE_V3");
    assertThat(evidence.verificationStatus()).isEqualTo("VERIFIED");
    assertThat(evidence.generationStrategy()).isEqualTo("VERSIONED_APPROVED_CREATIVE_MONTAGE");
    assertThat(evidence.productionReference())
        .isEqualTo("scripts/marketing/create-capella-successor-video-v1.sh");
    assertThat(evidence.finalArtifact().sha256()).isEqualTo("c".repeat(64));
    assertThat(evidence.presenterReferenceMode()).isEqualTo("APPROVED_PRODUCT_ASSETS");
    assertThat(evidence.approvedCreativeSources())
        .singleElement()
        .satisfies(source -> assertThat(source.creativeId()).isEqualTo(522L));
    assertThat(evidence.providerLicense()).isNull();
    assertThat(evidence.approvedCreativeSources().get(0).reviewedAt())
        .isEqualTo("2026-09-24T12:00:00Z");
  }

  /** Mantém a montagem bloqueada quando a fonte aprovada não pode ser confirmada novamente. */
  @Test
  void keepsVersionedMontageIncompleteWhenSourceSnapshotDoesNotMatch() throws Exception {
    Experiment sourceExperiment = Experiment.builder().id(88L).build();
    Experiment experiment = Experiment.builder().id(94L).sourceExperiment(sourceExperiment).build();
    String finalUrl = "https://cdn.test/capella-montage.mp4";
    Creative creative =
        Creative.builder().experiment(experiment).format("VIDEO").videoUrl(finalUrl).build();
    Asset finalAsset =
        asset(
            2824L,
            finalUrl,
            MediaProvider.USER_UPLOAD,
            "{\"metadata\":{\"sha256\":\"" + "c".repeat(64) + "\"}}");
    ExperimentVideoAsset video =
        ExperimentVideoAsset.builder()
            .id(44L)
            .experiment(experiment)
            .model("VERSIONED_FFMPEG_MONTAGE_V1")
            .status(ExperimentVideoStatus.READY)
            .reviewStatus(ExperimentVideoReviewStatus.APPROVED)
            .assetUrl(finalUrl)
            .asset(finalAsset)
            .requestJson(
                """
                {"artifactType":"experiment.userAdVideoUpload.v2",
                 "generationStrategy":"VERSIONED_APPROVED_CREATIVE_MONTAGE",
                 "productionReference":"scripts/marketing/create-capella-successor-video-v1.sh",
                 "approvedSourceCreatives":[{"creativeId":522,"experimentId":88,
                   "format":"IMAGE","mediaUrl":"https://cdn.test/forged.png",
                   "status":"READY","agentReviewStatus":"APPROVED",
                   "reviewedAt":"2026-09-24T12:00:00Z"}]}
                """)
            .build();
    when(videoAssets.findFirstByExperimentIdAndAssetUrlOrderByIdDesc(94L, finalUrl))
        .thenReturn(Optional.of(video));
    when(creatives.findByIdWithExperiment(522L)).thenReturn(Optional.empty());

    assertThat(service.resolve(creative).verificationStatus()).isEqualTo("INCOMPLETE");
  }

  /** Aprova vídeo sintético por texto sem inventar consentimento ou referência inexistente. */
  @Test
  void resolvesPromptOnlySyntheticVideoWithDisclosureAndProviderLicense() throws Exception {
    PromptOnlyScenario scenario = promptOnlyScenario(true, true, false, "gen4.5");

    var evidence = service.resolve(scenario.creative());

    assertThat(evidence.contractVersion()).isEqualTo("CREATIVE_MEDIA_GOVERNANCE_V2");
    assertThat(evidence.verificationStatus()).isEqualTo("VERIFIED");
    assertThat(evidence.presenterIsSynthetic()).isTrue();
    assertThat(evidence.presenterReferenceMode()).isEqualTo("PROMPT_ONLY_SYNTHETIC");
    assertThat(evidence.presenterReference()).isNull();
    assertThat(evidence.presenterConsentEvidence()).isNull();
    assertThat(evidence.referenceRightsEvidence()).isNull();
    assertThat(evidence.syntheticMediaDisclosureVerified()).isTrue();
    assertThat(evidence.generatedSourceArtifact().provider()).isEqualTo("RUNWAY");
    assertThat(evidence.providerLicense().catalogCode()).isEqualTo("runway-gen-4-5");
  }

  /**
   * Bloqueia rota sintética por texto quando o disclosure não está incorporado ao arquivo final.
   */
  @Test
  void keepsPromptOnlySyntheticVideoIncompleteWithoutDisclosure() throws Exception {
    PromptOnlyScenario scenario = promptOnlyScenario(false, true, false, "gen4.5");

    var evidence = service.resolve(scenario.creative());

    assertThat(evidence.presenterReferenceMode()).isEqualTo("PROMPT_ONLY_SYNTHETIC");
    assertThat(evidence.syntheticMediaDisclosureVerified()).isFalse();
    assertThat(evidence.verificationStatus()).isEqualTo("INCOMPLETE");
  }

  /**
   * Recusa declarar geração por texto quando o request contém entrada de referência não governada.
   */
  @Test
  void keepsUnknownReferenceInputIncomplete() throws Exception {
    PromptOnlyScenario scenario = promptOnlyScenario(true, true, true, "gen4.5");

    var evidence = service.resolve(scenario.creative());

    assertThat(evidence.presenterReferenceMode()).isEqualTo("UNRESOLVED");
    assertThat(evidence.verificationStatus()).isEqualTo("INCOMPLETE");
  }

  /** Impede que a licença de um modelo conhecido seja atribuída a outro escolhido pelo Router. */
  @Test
  void keepsUnknownRouterModelIncomplete() throws Exception {
    PromptOnlyScenario scenario = promptOnlyScenario(true, true, false, "modelo-nao-homologado");

    var evidence = service.resolve(scenario.creative());

    assertThat(evidence.providerLicense()).isNull();
    assertThat(evidence.verificationStatus()).isEqualTo("INCOMPLETE");
  }

  /** Mantém a peça bloqueada quando o arquivo final não preserva a tarefa que o produziu. */
  @Test
  void keepsPromptOnlySyntheticVideoIncompleteWithoutFinalProviderTask() throws Exception {
    PromptOnlyScenario scenario = promptOnlyScenario(true, false, false, "gen4.5");

    var evidence = service.resolve(scenario.creative());

    assertThat(evidence.finalArtifact().providerTaskId()).isNull();
    assertThat(evidence.verificationStatus()).isEqualTo("INCOMPLETE");
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

  /** Reproduz a linhagem real de Vega sem realizar chamadas externas nem reutilizar uma pessoa. */
  private PromptOnlyScenario promptOnlyScenario(
      boolean disclosure, boolean finalProviderTask, boolean unknownReference, String sourceModel)
      throws Exception {
    Experiment experiment = Experiment.builder().id(92L).build();
    Creative creative =
        Creative.builder()
            .id(529L)
            .experiment(experiment)
            .format("VIDEO")
            .videoUrl(FINAL_URL)
            .build();
    Asset finalAsset =
        asset(
            2804L,
            FINAL_URL,
            MediaProvider.VIDEO_MODULE,
            promptOnlyFinalPayload(disclosure, finalProviderTask));
    Asset sourceAsset =
        asset(
            2794L,
            "https://cdn.test/runway-montage.mp4",
            MediaProvider.VIDEO_MODULE,
            promptOnlySourcePayload(sourceModel));
    SalesVideoJob job = new SalesVideoJob();
    job.setId(21239L);
    ExperimentVideoAsset video =
        ExperimentVideoAsset.builder()
            .id(41L)
            .experiment(experiment)
            .slot(ExperimentVideoSlot.AD)
            .provider("MUSA_POST_PRODUCTION")
            .status(ExperimentVideoStatus.READY)
            .reviewStatus(ExperimentVideoReviewStatus.APPROVED)
            .assetUrl(FINAL_URL)
            .asset(finalAsset)
            .salesVideoJob(job)
            .requestJson(promptOnlyRequestJson(unknownReference))
            .reviewedBy("Marketing Hub")
            .reviewedAt(Instant.parse("2026-09-14T22:46:04Z"))
            .build();
    VideoProject project =
        VideoProject.builder()
            .id(4L)
            .experimentId(92L)
            .referencePerformanceUri("internal://pde-proof/vega-v12")
            .build();
    SalesVideoProviderModel provider = new SalesVideoProviderModel();
    provider.setCode("runway-gen-4-5");
    provider.setProviderName("RUNWAY");
    provider.setExternalModelId("gen4.5");
    provider.setLifecycleStatus("ACTIVE");
    provider.setAdapterVerified(true);
    provider.setQualityGateVerified(true);
    provider.setCommercialLicenseVerified(true);
    provider.setUpdatedAt(Instant.parse("2026-09-04T18:06:48Z"));
    when(videoAssets.findFirstByExperimentIdAndAssetUrlOrderByIdDesc(92L, FINAL_URL))
        .thenReturn(Optional.of(video));
    when(videoProjects.findById(4L)).thenReturn(Optional.of(project));
    when(assets.findById(2794L)).thenReturn(Optional.of(sourceAsset));
    when(providerModels.findByProviderName("RUNWAY")).thenReturn(Optional.of(provider));
    return new PromptOnlyScenario(creative);
  }

  /** Serializa o vídeo bruto com o modelo efetivamente escolhido pelo Router. */
  private String promptOnlySourcePayload(String sourceModel) throws Exception {
    var providerMetadata = java.util.Map.of("provider", "RUNWAY", "model", sourceModel);
    var metadata = new java.util.LinkedHashMap<String, Object>();
    metadata.put("provider_job_id", "runway-task-1,runway-task-2");
    metadata.put("sha256", "b".repeat(64));
    metadata.put("provider_metadata", providerMetadata);
    return new ObjectMapper().writeValueAsString(java.util.Map.of("metadata", metadata));
  }

  /** Serializa a peça final com ou sem o disclosure sintético inseparável. */
  private String promptOnlyFinalPayload(boolean disclosure, boolean finalProviderTask)
      throws Exception {
    var providerMetadata = new java.util.LinkedHashMap<String, Object>();
    providerMetadata.put("provider", "MUSA_POST_PRODUCTION");
    if (disclosure) {
      providerMetadata.put(
          "synthetic_media_disclosure",
          java.util.Map.of(
              "presenter_synthetic", true,
              "required", true,
              "text", "Apresentadora e voz geradas por IA"));
    }
    var metadata = new java.util.LinkedHashMap<String, Object>();
    if (finalProviderTask) {
      metadata.put("provider_job_id", "post-production-21239");
    }
    metadata.put("sha256", "a".repeat(64));
    metadata.put("provider_metadata", providerMetadata);
    return new ObjectMapper().writeValueAsString(java.util.Map.of("metadata", metadata));
  }

  /** Serializa o contrato de geração por texto e permite inserir uma referência adversarial. */
  private String promptOnlyRequestJson(boolean unknownReference) throws Exception {
    var input = new java.util.LinkedHashMap<String, Object>();
    input.put("promptText", "Participante adulta fictícia e sintética em cenário neutro.");
    input.put("duration", 10);
    input.put("aspectRatio", "9:16");
    input.put("resolution", "720p");
    input.put("audio", false);
    if (unknownReference) {
      input.put("promptImage", "https://external.test/person.png");
    }
    String requests =
        new ObjectMapper()
            .writeValueAsString(
                java.util.List.of(
                    java.util.Map.of(
                        "configId", "marketing-hub-campaign-final-v1", "input", input)));
    var metadata = new java.util.LinkedHashMap<String, Object>();
    metadata.put("videoProjectId", 4);
    metadata.put("generation_strategy", "PROVIDER_CLIPS_WITH_POST_PRODUCTION_CUTS");
    metadata.put("sourceAssetId", 2794);
    metadata.put("sourceProviderName", "RUNWAY_ROUTER");
    metadata.put("runwayRouterRequestsJson", requests);
    metadata.put(
        "referenceGovernance",
        java.util.Map.of("presenterIsSynthetic", true, "productIsDigitalExperience", true));
    return new ObjectMapper()
        .writeValueAsString(
            java.util.Map.of(
                "postProductionMetadataJson", new ObjectMapper().writeValueAsString(metadata)));
  }

  /** Agrupa o criativo preparado para os cenários de geração sintética por texto. */
  private record PromptOnlyScenario(Creative creative) {}

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
