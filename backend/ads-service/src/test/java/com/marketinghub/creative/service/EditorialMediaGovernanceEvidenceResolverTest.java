package com.marketinghub.creative.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.dto.CreativeMediaGovernanceEvidenceDto;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProviderModelRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.dto.DeliveryExampleDto;
import com.marketinghub.salesvideo.service.VideoDeliveryExampleSource;
import com.marketinghub.salesvideo.service.VideoProductProofService;
import com.marketinghub.salesvideo.service.VideoProductProofSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Homologa a projeção comercial real com provas e recibos sintéticos de duas identidades. */
class EditorialMediaGovernanceEvidenceResolverTest {
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final AssetRepository assets = mock(AssetRepository.class);
  private final SalesVideoJobRepository jobs = mock(SalesVideoJobRepository.class);
  private final VideoProjectRepository projects = mock(VideoProjectRepository.class);
  private final ExperimentVideoAssetRepository videos = mock(ExperimentVideoAssetRepository.class);
  private final VideoProductProofSource proofSource = mock(VideoProductProofSource.class);
  private final VideoDeliveryExampleSource examples = mock(VideoDeliveryExampleSource.class);
  private final Map<Long, Asset> storedAssets = new HashMap<>();
  private final Map<Long, SalesVideoJob> storedJobs = new HashMap<>();
  private VideoProject project;
  private ExperimentVideoAsset video;
  private Creative creative;
  private ObjectNode lineage, finalMetadata, finalJobMetadata;
  private long productId, experimentId, projectId, sourceId, finalId, originId;
  private List<DeliveryExampleDto> approved;
  private CreativeMediaGovernanceEvidenceService service;

  /**
   * Prova a composição original e outra identidade, preservando arquivos, custo e aceite humano.
   */
  @ParameterizedTest
  @ValueSource(longs = {105, 9405})
  void resolvesEditorialProofAndOriginalBuiltInVoice(long id) throws Exception {
    fixture(id);
    var result = resolve();
    assertThat(result.contractVersion()).isEqualTo("CREATIVE_MEDIA_GOVERNANCE_V5");
    assertThat(result.verificationStatus()).isEqualTo("VERIFIED");
    assertThat(result.presenterIsSynthetic()).isFalse();
    assertThat(result.presenterReference()).isNull();
    assertThat(result.presenterReferenceMode())
        .isEqualTo("APPROVED_PRODUCT_PROOF_WITH_BUILT_IN_TTS");
    assertThat(result.syntheticMediaDisclosureVerified()).isTrue();
    assertThat(result.providerLicense().providerName()).isEqualTo("OPENAI_TTS");
    assertThat(result.editorialMediaEvidence().blockers()).isEmpty();
    assertThat(result.editorialMediaEvidence().productProofSources()).hasSize(3);
    assertThat(result.editorialMediaEvidence().deliverySources()).hasSize(2);
    assertThat(result.editorialMediaEvidence().audio().segments()).hasSize(2);
    assertThat(result.editorialMediaEvidence().audio().costStatus()).contains("NOT_RECONCILED");
    assertThat(json.writeValueAsString(result))
        .doesNotContain("raw_request", "instructions", "authorization");
    assertThat(video.getReviewStatus()).isEqualTo(ExperimentVideoReviewStatus.APPROVED);
    assertThat(result.finalArtifact().sha256()).isEqualTo("f".repeat(64));
  }

  /** Toda fonte incompleta ou trocada continua bloqueante; não há exceções por produto ou ID. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "sourceHash",
        "sourceJob",
        "finalJob",
        "sourceJobVersion",
        "finalJobVersion",
        "frozenSourceVersion",
        "frozenSourceHash",
        "proof",
        "captureHash",
        "captureCrop",
        "deliveryHash",
        "deliveryRights",
        "audioHash",
        "audioRequest",
        "customVoice",
        "model",
        "disclosure",
        "audioOwner",
        "finalReceipts",
        "presenter",
        "music",
        "tenant",
        "product",
        "version"
      })
  void rejectsMissingOrMismatchedEvidence(String fault) throws Exception {
    fixture(105);
    switch (fault) {
      case "sourceHash" -> {}
      case "sourceJob" -> storedJobs.get(sourceId).setProviderName("UNVERIFIED_PROVIDER");
      case "finalJob" -> storedJobs.get(finalId).setAsset(storedAssets.get(sourceId));
      case "sourceJobVersion" ->
          storedJobs
              .get(sourceId)
              .setMetadataJson(context().put("campaign_key", "another-version").toString());
      case "finalJobVersion" -> finalJobMetadata.put("campaign_key", "another-version");
      case "frozenSourceVersion", "frozenSourceHash" -> {
        var context = (ObjectNode) json.readTree(storedJobs.get(sourceId).getMetadataJson());
        ((ObjectNode) context.at("/post_production/product_proof"))
            .put(
                fault.equals("frozenSourceVersion") ? "productVersion" : "sha256",
                fault.equals("frozenSourceVersion") ? "another-version" : "0".repeat(64));
        storedJobs.get(sourceId).setMetadataJson(context.toString());
      }
      case "proof" -> when(proofSource.find(700L, 90L)).thenReturn(Optional.empty());
      case "captureHash" ->
          ((ObjectNode) finalMetadata.at("/product_reference_overlay/productProof/shots/0"))
              .put("sha256", "0".repeat(64));
      case "captureCrop" ->
          ((ObjectNode) finalMetadata.at("/product_reference_overlay/productProof/shots/0"))
              .putArray("crop")
              .add(1)
              .add(2)
              .add(4)
              .add(99);
      case "deliveryHash" ->
          ((ObjectNode) finalMetadata.at("/product_reference_overlay/deliveryExamples/shots/0"))
              .put("sha256", "0".repeat(64));
      case "deliveryRights" ->
          approved.set(
              0,
              new DeliveryExampleDto(
                  155L,
                  2L,
                  "Arte",
                  "https://cdn.test/a.png",
                  "c".repeat(64),
                  "Acervo auditado",
                  ""));
      case "audioHash" -> changeAudio(0, "hash");
      case "audioRequest" -> changeAudio(0, "request");
      case "customVoice" -> changeAudio(0, "voice");
      case "model" -> ((ObjectNode) finalMetadata.at("/audio/review")).put("model", "other-model");
      case "disclosure" ->
          ((ObjectNode) finalMetadata.path("synthetic_media_disclosure")).put("required", false);
      case "audioOwner" -> storedJobs.get(originId).setTenantId("other-tenant");
      case "finalReceipts" ->
          ((ObjectNode) finalMetadata.path("tts_interactions").get(0))
              .put("source_sha256", "0".repeat(64));
      case "presenter" -> project.setCharacterPerformanceUri("https://cdn.test/real-person.png");
      case "music" -> ((ObjectNode) finalMetadata.path("audio")).put("music", "licensed-unknown");
      case "tenant" -> project.setTenantId("other-tenant");
      case "product" -> project.setProductId(9999L);
      case "version" -> project.setCampaignKey("different-version");
      default -> throw new IllegalArgumentException(fault);
    }
    if (fault.equals("sourceHash")) {
      var meta = assetMetadata(sourceId);
      ((ObjectNode) meta.at("/provider_metadata/sources/0")).put("sha256", "0".repeat(64));
      storeMetadata(sourceId, meta);
    }
    var result = resolve();
    assertThat(result.verificationStatus()).isEqualTo("INCOMPLETE");
    assertThat(result.editorialMediaEvidence().blockers()).isNotEmpty();
    assertThat(video.getReviewStatus()).isEqualTo(ExperimentVideoReviewStatus.APPROVED);
  }

  /** Monta o contrato completo com serialização, recuperação de voz e resolvedor de prova reais. */
  private void fixture(long id) throws Exception {
    experimentId = id;
    productId = id + 100;
    projectId = id + 200;
    sourceId = id + 300;
    finalId = id + 400;
    originId = id + 500;
    var experiment =
        Experiment.builder().id(id).product(Product.builder().id(productId).build()).build();
    creative =
        Creative.builder()
            .id(id + 600)
            .format("VIDEO")
            .experiment(experiment)
            .videoUrl("https://cdn.test/final-" + id + ".mp4")
            .build();
    project =
        VideoProject.builder()
            .id(projectId)
            .productId(productId)
            .experimentId(id)
            .tenantId("default")
            .campaignKey("candidate-v3")
            .referencePerformanceUri(
                "internal://agent-tasks/700/visual-evidence/90#crop=1,2,100,100")
            .build();
    when(projects.findById(projectId)).thenReturn(Optional.of(project));
    when(assets.findById(anyLong()))
        .thenAnswer(c -> Optional.ofNullable(storedAssets.get(c.getArgument(0))));
    when(jobs.findById(anyLong()))
        .thenAnswer(c -> Optional.ofNullable(storedJobs.get(c.getArgument(0))));
    proof(700, 90, "a");
    proof(701, 91, "b");
    approved =
        new ArrayList<>(
            List.of(
                new DeliveryExampleDto(
                    155L,
                    2L,
                    "Arte A",
                    "https://cdn.test/a.png",
                    "c".repeat(64),
                    "Acervo auditado",
                    "Uso comercial declarado e auditado"),
                new DeliveryExampleDto(
                    133L,
                    2L,
                    "Arte B",
                    "https://cdn.test/b.png",
                    "d".repeat(64),
                    "Acervo auditado",
                    "Uso comercial declarado e auditado")));
    when(examples.approved(productId)).thenAnswer(c -> approved);
    lineage =
        context()
            .put("generation_strategy", EditorialMediaGovernanceEvidenceResolver.STRATEGY)
            .put("sourceJobId", sourceId)
            .put("sourceAssetId", sourceId)
            .put("captionText", "Primeiro trecho | Segundo trecho")
            .put("voiceOverScript", "Primeiro trecho Segundo trecho");
    lineage
        .putArray("requestedProofShots")
        .add(
            json.valueToTree(
                Map.of(
                    "reference",
                    "internal://agent-tasks/700/visual-evidence/90#crop=1,2,100,100",
                    "captionCueIndex",
                    0,
                    "minimumSeconds",
                    3)))
        .add(
            json.valueToTree(
                Map.of(
                    "reference",
                    "internal://agent-tasks/701/visual-evidence/91#crop=1,2,100,100",
                    "captionCueIndex",
                    1,
                    "minimumSeconds",
                    3)));
    lineage
        .putArray("requestedDeliveryExamples")
        .add(json.valueToTree(Map.of("visualAssetId", 155, "captionCueIndex", 0)))
        .add(json.valueToTree(Map.of("visualAssetId", 133, "captionCueIndex", 1)));
    var sourceMeta =
        json.createObjectNode()
            .put("job_id", sourceId)
            .put("provider_job_id", "editorial-" + sourceId)
            .put("sha256", "e".repeat(64));
    sourceMeta
        .putObject("provider_metadata")
        .put("provider", "EDITORIAL_MOTION")
        .putArray("sources")
        .add(json.valueToTree(Map.of("asset_id", 90, "sha256", "a".repeat(64))));
    Asset sourceAsset = asset(sourceId, AssetType.VIDEO, "https://cdn.test/source.mp4", sourceMeta);
    var sourceContext = context();
    sourceContext.remove("campaign_key");
    sourceContext
        .putObject("post_production")
        .set(
            "product_proof",
            json.valueToTree(
                new VideoProductProofService(proofSource, projects, json).resolve(project)));
    var sourceJob =
        job(sourceId, "EDITORIAL_MOTION", "editorial-" + sourceId, sourceAsset, sourceContext);
    finalMetadata =
        json.createObjectNode()
            .put("provider", "MUSA_POST_PRODUCTION")
            .put("provider_job_id", "post-" + finalId);
    finalMetadata
        .putObject("audio")
        .put("music", "none")
        .put("mix", "VOICE_ONLY")
        .put("ai_generated_disclosure", true)
        .putObject("review")
        .put("provider", "OPENAI_TTS")
        .put("model", "gpt-4o-mini-tts-2025-12-15")
        .put("voice", "marin");
    finalMetadata
        .putObject("synthetic_media_disclosure")
        .put("voice_synthetic", true)
        .put("presenter_synthetic", false)
        .put("required", true)
        .put("text", "Voz gerada por IA");
    var overlay = finalMetadata.putObject("product_reference_overlay");
    var appliedProofs = overlay.putObject("productProof").putArray("shots");
    for (int i = 0; i < 2; i++)
      appliedProofs.add(
          json.valueToTree(
              Map.of(
                  "status",
                  "APPLIED",
                  "sha256",
                  (i == 0 ? "a" : "b").repeat(64),
                  "source",
                  "/api/sales-videos/projects/"
                      + projectId
                      + "/product-proof/"
                      + (700 + i)
                      + "/"
                      + (90 + i),
                  "captionCueIndex",
                  i,
                  "crop",
                  List.of(1, 2, 100, 100),
                  "commercialEvidenceClaimed",
                  false,
                  "syntheticScenario",
                  true)));
    var appliedArts = overlay.putObject("deliveryExamples").putArray("shots");
    for (int i = 0; i < 2; i++)
      appliedArts.add(
          json.valueToTree(
              Map.of(
                  "visualAssetId",
                  i == 0 ? 155 : 133,
                  "sha256",
                  (i == 0 ? "c" : "d").repeat(64),
                  "captionCueIndex",
                  i)));
    finalJobMetadata = lineage.deepCopy();
    var preserved =
        finalJobMetadata
            .putObject("preservedNarration")
            .put("contractVersion", "PRESERVED_TTS_NARRATION_V1")
            .put("newTtsAuthorized", false)
            .put("sourceJobId", originId)
            .put("tenantId", "default")
            .put("captionText", lineage.path("captionText").asText());
    var segments = preserved.putArray("segments");
    var receipts = finalJobMetadata.putArray("tts_interactions");
    job(originId, "MUSA_POST_PRODUCTION", "original-" + originId, null, context());
    for (int i = 0; i < 2; i++) {
      long audioId = id + 1000 + i;
      String hash = String.valueOf(i + 1).repeat(64),
          text = i == 0 ? "Primeiro trecho" : "Segundo trecho";
      var pricing = Map.of("status", "PENDING_PROVIDER_RECONCILIATION");
      segments.add(
          json.valueToTree(
              Map.of(
                  "segmentIndex",
                  i + 1,
                  "assetId",
                  audioId,
                  "url",
                  "https://cdn.test/a-" + i + ".mp3",
                  "sha256",
                  hash,
                  "text",
                  text,
                  "durationSeconds",
                  3.0,
                  "model",
                  "gpt-4o-mini-tts-2025-12-15",
                  "voice",
                  "marin",
                  "sourcePricing",
                  pricing)));
      var receipt =
          json.createObjectNode()
              .put("interaction_type", "TEXT_TO_SPEECH_REUSE")
              .put("provider", "OPENAI")
              .put("status", "REUSED")
              .put("job_id", finalId)
              .put("source_job_id", originId)
              .put("source_asset_id", audioId)
              .put("source_sha256", hash)
              .put("segment_index", i + 1)
              .put("model", "gpt-4o-mini-tts-2025-12-15")
              .put("output_duration_seconds", 3.0)
              .put("incremental_cost_usd", 0);
      receipt.set("source_pricing", json.valueToTree(pricing));
      receipts.add(receipt);
      var original =
          json.createObjectNode()
              .put("interaction_type", "TEXT_TO_SPEECH")
              .put("provider", "OPENAI")
              .put("status", "COMPLETED")
              .put("job_id", originId)
              .put("segment_index", i + 1)
              .put("endpoint", "https://api.openai.com/v1/audio/speech");
      original
          .putObject("raw_request")
          .put("model", "gpt-4o-mini-tts-2025-12-15")
          .put("voice", "marin")
          .put("input", text)
          .put("response_format", "mp3");
      original
          .putObject("raw_response")
          .put("sha256", hash)
          .put("bytes", 100)
          .put("asset_file_name", "part-" + i + ".mp3")
          .put("request_id", "req-test-" + i);
      var audioMeta =
          json.createObjectNode()
              .put("job_id", originId)
              .put("sha256", hash)
              .put("size_bytes", 100)
              .put("file_name", "part-" + i + ".mp3");
      audioMeta.putObject("provider_metadata").putArray("tts_interactions").add(original);
      asset(audioId, AssetType.AUDIO, "https://cdn.test/a-" + i + ".mp3", audioMeta);
    }
    finalMetadata.set("tts_interactions", receipts.deepCopy());
    var finalMeta =
        json.createObjectNode()
            .put("job_id", finalId)
            .put("sha256", "f".repeat(64))
            .put("provider_job_id", "post-" + finalId);
    finalMeta.set("provider_metadata", finalMetadata);
    Asset finalAsset = asset(finalId, AssetType.VIDEO, creative.getVideoUrl(), finalMeta);
    var finalJob =
        job(finalId, "MUSA_POST_PRODUCTION", "post-" + finalId, finalAsset, finalJobMetadata);
    finalJob.setRetryOfJob(sourceJob);
    video =
        ExperimentVideoAsset.builder()
            .id(id + 2000)
            .experiment(experiment)
            .provider("MUSA_POST_PRODUCTION")
            .model("gpt-4o-mini-tts-2025-12-15")
            .status(ExperimentVideoStatus.READY)
            .reviewStatus(ExperimentVideoReviewStatus.APPROVED)
            .asset(finalAsset)
            .assetUrl(creative.getVideoUrl())
            .salesVideoJob(finalJob)
            .durationSeconds(8)
            .reviewedBy("Operador do teste")
            .reviewedAt(Instant.parse("2026-10-16T20:00:00Z"))
            .build();
    when(videos.findFirstByExperimentIdAndAssetUrlOrderByIdDesc(id, creative.getVideoUrl()))
        .thenReturn(Optional.of(video));
    var proofService = new VideoProductProofService(proofSource, projects, json);
    var audio = new BuiltInTtsEvidenceResolver(assets, jobs, json);
    var editorial =
        new EditorialMediaGovernanceEvidenceResolver(
            assets, jobs, proofService, examples, audio, json);
    service =
        new CreativeMediaGovernanceEvidenceService(
            videos,
            projects,
            assets,
            mock(SalesVideoProviderModelRepository.class),
            mock(CreativeRepository.class),
            json,
            editorial);
  }

  /** Mantém o snapshot da resposta e o request auditado coerentes antes de cada leitura. */
  private CreativeMediaGovernanceEvidenceDto resolve() throws Exception {
    var meta = assetMetadata(finalId);
    meta.set("provider_metadata", finalMetadata);
    storeMetadata(finalId, meta);
    storedJobs.get(finalId).setMetadataJson(json.writeValueAsString(finalJobMetadata));
    video.setRequestJson(
        json.writeValueAsString(
            Map.of("postProductionMetadataJson", json.writeValueAsString(lineage))));
    return service.resolve(creative);
  }

  /**
   * Modela uma prova homologada realista com identidade, decisão, tráfego e versão obrigatórios.
   */
  private void proof(long task, long evidence, String hash) throws Exception {
    var result =
        context()
            .put("contractVersion", "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1")
            .put("decision", "APPROVED")
            .put("prototypeVersion", "candidate-v3")
            .put("trafficClass", "AGENT_VALIDATION")
            .put("commercialEvidenceClaimed", false);
    result
        .putArray("artifacts")
        .add(json.valueToTree(Map.of("artifactId", evidence, "sha256", hash.repeat(64))));
    when(proofSource.find(task, evidence))
        .thenReturn(
            Optional.of(
                new VideoProductProofSource.Proof(
                    task,
                    evidence,
                    "COMPLETED",
                    "experiment:" + experimentId,
                    json.writeValueAsString(result),
                    hash.repeat(64))));
  }

  /** Constrói contexto isolado que acompanha todos os jobs e segmentos do produto sintético. */
  private ObjectNode context() {
    return json.createObjectNode()
        .put("videoProjectId", projectId)
        .put("productId", productId)
        .put("experimentId", experimentId)
        .put("tenantId", "default")
        .put("campaign_key", "candidate-v3");
  }

  /** Registra job local com os mesmos estados e tipos do fluxo produtivo. */
  private SalesVideoJob job(
      long id, String provider, String providerId, Asset asset, ObjectNode metadata)
      throws Exception {
    var job =
        SalesVideoJob.builder()
            .id(id)
            .tenantId("default")
            .providerName(provider)
            .providerJobId(providerId)
            .status(SalesVideoStatus.VIDEO_READY)
            .jobType(
                provider.equals("EDITORIAL_MOTION")
                    ? SalesVideoJobType.RENDER
                    : SalesVideoJobType.POST_PRODUCTION)
            .asset(asset)
            .metadataJson(json.writeValueAsString(metadata))
            .build();
    storedJobs.put(id, job);
    return job;
  }

  /** Registra arquivo sintético com hash e metadados nos mesmos envelopes do uploader real. */
  private Asset asset(long id, AssetType type, String url, ObjectNode metadata) throws Exception {
    var value =
        Asset.builder()
            .id(id)
            .type(type)
            .provider(MediaProvider.VIDEO_MODULE)
            .status(AssetStatus.READY)
            .url(url)
            .build();
    storedAssets.put(id, value);
    storeMetadata(id, metadata);
    return value;
  }

  /** Lê os metadados persistidos do arquivo sintético para uma mutação negativa específica. */
  private ObjectNode assetMetadata(long id) throws Exception {
    return (ObjectNode) json.readTree(storedAssets.get(id).getPayload()).path("metadata");
  }

  /** Atualiza somente o arquivo de teste, preservando o envelope do uploader. */
  private void storeMetadata(long id, ObjectNode metadata) throws Exception {
    storedAssets.get(id).setPayload(json.writeValueAsString(Map.of("metadata", metadata)));
  }

  /** Corrompe identidade ou request original para provar bloqueio antes de qualquer aprovação. */
  private void changeAudio(int index, String fault) throws Exception {
    long id = experimentId + 1000 + index;
    var meta = assetMetadata(id);
    if (fault.equals("hash")) meta.put("sha256", "0".repeat(64));
    else if (fault.equals("request"))
      ((ObjectNode) meta.at("/provider_metadata/tts_interactions/0/raw_request"))
          .put("input", "Outro texto");
    else
      ((ObjectNode) meta.at("/provider_metadata/tts_interactions/0/raw_request"))
          .putObject("voice")
          .put("id", "custom-voice");
    storeMetadata(id, meta);
  }
}
