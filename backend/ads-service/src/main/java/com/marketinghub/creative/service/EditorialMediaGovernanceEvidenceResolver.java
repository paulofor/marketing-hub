package com.marketinghub.creative.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.dto.CreativeMediaGovernanceEvidenceDto;
import com.marketinghub.creative.dto.CreativeMediaGovernanceEvidenceDto.MediaArtifact;
import com.marketinghub.creative.dto.EditorialMediaEvidenceDto;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.dto.DeliveryExampleDto;
import com.marketinghub.salesvideo.service.VideoDeliveryExampleSource;
import com.marketinghub.salesvideo.service.VideoProductProofService;
import com.marketinghub.salesvideo.service.postproduction.ProductProofShot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Resolve a linhagem da montagem editorial contra provas, fontes e recibos de voz persistidos. */
@Component
@Slf4j
public class EditorialMediaGovernanceEvidenceResolver {
  static final String STRATEGY = "DETERMINISTIC_EDITORIAL_MOTION_FROM_APPROVED_PRODUCT_PROOF";
  static final String CONTRACT = "CREATIVE_MEDIA_GOVERNANCE_V5";
  static final String REFERENCE_MODE = "APPROVED_PRODUCT_PROOF_WITH_BUILT_IN_TTS";
  private final AssetRepository assets;
  private final SalesVideoJobRepository jobs;
  private final VideoProductProofService proofs;
  private final VideoDeliveryExampleSource deliveryExamples;
  private final BuiltInTtsEvidenceResolver speech;
  private final ObjectMapper json;

  /**
   * Conecta os leitores auditáveis; a prova é resolvida sob demanda porque o contexto de agentes
   * também apresenta a governança, sem que a leitura de pixels chame esse contexto novamente.
   */
  public EditorialMediaGovernanceEvidenceResolver(
      AssetRepository assets,
      SalesVideoJobRepository jobs,
      @org.springframework.context.annotation.Lazy VideoProductProofService proofs,
      VideoDeliveryExampleSource deliveryExamples,
      BuiltInTtsEvidenceResolver speech,
      ObjectMapper json) {
    this.assets = assets;
    this.jobs = jobs;
    this.proofs = proofs;
    this.deliveryExamples = deliveryExamples;
    this.speech = speech;
    this.json = json;
  }

  /**
   * Reconhece somente a receita editorial explícita, mantendo as rotas anteriores independentes.
   */
  public boolean supports(JsonNode lineage) {
    return STRATEGY.equals(lineage.path("generation_strategy").asText());
  }

  /** Confronta o arquivo exato com todas as fontes sem alterar aprovação ou gerar nova mídia. */
  public CreativeMediaGovernanceEvidenceDto resolve(
      Creative creative,
      ExperimentVideoAsset video,
      VideoProject project,
      JsonNode lineage,
      MediaArtifact finalArtifact,
      MediaArtifact sourceArtifact)
      throws JsonProcessingException {
    var blockers = new ArrayList<String>();
    var proofSources = new ArrayList<Map<String, Object>>();
    var visualSources = new ArrayList<Map<String, Object>>();
    SalesVideoJob finalJob = video.getSalesVideoJob();
    SalesVideoJob sourceJob = jobs.findById(lineage.path("sourceJobId").asLong()).orElse(null);
    if (!owned(creative, project, lineage) || finalJob == null || sourceJob == null) {
      blockers.add("Projeto, produto, versão ou jobs não comprovam o mesmo contexto.");
      return evidence(
          video,
          project,
          lineage,
          finalArtifact,
          sourceArtifact,
          proofSources,
          visualSources,
          null,
          blockers);
    }
    JsonNode finalMetadata = metadata(video.getAsset()).path("provider_metadata");
    var source = assets.findById(lineage.path("sourceAssetId").asLong()).orElse(null);
    JsonNode sourceMetadata = metadata(source);
    boolean identity =
        video.getStatus() == ExperimentVideoStatus.READY
            && video.getReviewStatus() == ExperimentVideoReviewStatus.APPROVED
            && hash(finalArtifact.sha256())
            && hash(sourceArtifact.sha256())
            && finalJob.getStatus() == SalesVideoStatus.VIDEO_READY
            && sourceJob.getStatus() == SalesVideoStatus.VIDEO_READY
            && "EDITORIAL_MOTION".equals(sourceJob.getProviderName())
            && "MUSA_POST_PRODUCTION".equals(finalJob.getProviderName())
            && sameContext(finalJob, project)
            && sameContext(sourceJob, project)
            && finalJob.getAsset() != null
            && Objects.equals(finalJob.getAsset().getId(), finalArtifact.assetId())
            && sourceJob.getAsset() != null
            && Objects.equals(sourceJob.getAsset().getId(), sourceArtifact.assetId())
            && Objects.equals(finalJob.getProviderJobId(), finalArtifact.providerTaskId())
            && Objects.equals(sourceJob.getProviderJobId(), sourceArtifact.providerTaskId())
            && sourceMetadata.path("job_id").asLong() == sourceJob.getId()
            && finalMetadata.path("provider_job_id").asText().equals(finalJob.getProviderJobId());
    if (!identity)
      blockers.add("A cadeia entre arquivo final, fonte e jobs auditados é divergente.");
    if (project.getCharacterPerformanceUri() != null
        && !project.getCharacterPerformanceUri().isBlank())
      blockers.add(
          "Referência de apresentador não é coberta pela rota editorial sem apresentador.");
    if (!"none".equals(finalMetadata.at("/audio/music").asText())
        || !"VOICE_ONLY".equals(finalMetadata.at("/audio/mix").asText()))
      blockers.add("Esta rota não comprova direitos de trilha ou áudio externo adicional.");
    try {
      Map<String, Object> primary = proofs.resolve(project);
      JsonNode sourceInputs = sourceMetadata.at("/provider_metadata/sources");
      JsonNode frozenProof = read(sourceJob.getMetadataJson()).at("/post_production/product_proof");
      if (primary.isEmpty()
          || !sourceInputs.isArray()
          || sourceInputs.size() != 1
          || !Objects.equals(primary.get("sha256"), sourceInputs.get(0).path("sha256").asText())
          || !Objects.equals(primary.get("sha256"), frozenProof.path("sha256").asText())
          || ((Number) primary.get("evidenceId")).longValue()
              != sourceInputs.get(0).path("asset_id").asLong())
        blockers.add("O vídeo fonte não corresponde à prova homologada do produto.");
      else proofSources.add(primary);
      verifyProofShots(project, lineage, finalMetadata, proofSources, blockers);
    } catch (ResponseStatusException ex) {
      log.warn(
          "Governança editorial: prova não verificável. creativeId={} projectId={}",
          creative.getId(),
          project.getId(),
          ex);
      blockers.add("Prova técnica não verificável: " + ex.getReason());
    }
    verifyDelivery(project, lineage, finalMetadata, visualSources, blockers);
    var audio =
        speech.resolve(
            sourceJob,
            finalJob,
            project,
            finalMetadata,
            video.getDurationSeconds() == null ? 0 : video.getDurationSeconds());
    blockers.addAll(audio.blockers());
    return evidence(
        video,
        project,
        lineage,
        finalArtifact,
        sourceArtifact,
        proofSources,
        visualSources,
        audio,
        blockers);
  }

  /** Revalida cada captura aplicada, incluindo recorte, trecho, hash e autorização da versão. */
  private void verifyProofShots(
      VideoProject project,
      JsonNode lineage,
      JsonNode finalMetadata,
      List<Map<String, Object>> result,
      List<String> blockers) {
    JsonNode requested = lineage.path("requestedProofShots");
    JsonNode applied = finalMetadata.at("/product_reference_overlay/productProof/shots");
    if (!requested.isArray()
        || requested.isEmpty()
        || !applied.isArray()
        || requested.size() != applied.size()) {
      blockers.add("Sequência de capturas ausente ou incompleta.");
      return;
    }
    var shots = new ArrayList<ProductProofShot>();
    for (JsonNode shot : requested)
      shots.add(
          new ProductProofShot(
              shot.path("reference").asText(),
              shot.path("captionCueIndex").asInt(),
              shot.path("minimumSeconds").asDouble()));
    var verified = proofs.verifiedSequence(project, shots, lineage.path("captionText").asText());
    for (int i = 0; i < verified.size(); i++) {
      Map<String, Object> proof = verified.get(i);
      JsonNode used = applied.get(i);
      boolean matches =
          "APPLIED".equals(used.path("status").asText())
              && Objects.equals(proof.get("sha256"), used.path("sha256").asText())
              && Objects.equals(proof.get("contentPath"), used.path("source").asText())
              && ((Number) proof.get("captionCueIndex")).intValue()
                  == used.path("captionCueIndex").asInt()
              && json.valueToTree(proof.get("crop")).equals(used.path("crop"))
              && !used.path("commercialEvidenceClaimed").asBoolean(true)
              && used.path("syntheticScenario").asBoolean(false);
      if (!matches) blockers.add("Captura aplicada diverge da fonte homologada: trecho " + i);
      else result.add(proof);
    }
  }

  /**
   * Confere cada arte aplicada com a biblioteca aprovada do produto, sem confiar só na declaração.
   */
  private void verifyDelivery(
      VideoProject project,
      JsonNode lineage,
      JsonNode finalMetadata,
      List<Map<String, Object>> result,
      List<String> blockers) {
    JsonNode requested = lineage.path("requestedDeliveryExamples");
    JsonNode applied = finalMetadata.at("/product_reference_overlay/deliveryExamples/shots");
    if (!requested.isArray()
        || requested.size() < 2
        || !applied.isArray()
        || requested.size() != applied.size()) {
      blockers.add("Amostras de entrega aplicadas estão ausentes ou incompletas.");
      return;
    }
    List<DeliveryExampleDto> approved = deliveryExamples.approved(project.getProductId());
    for (int i = 0; i < requested.size(); i++) {
      JsonNode request = requested.get(i), used = applied.get(i);
      var source =
          approved.stream()
              .filter(a -> a.visualAssetId() == request.path("visualAssetId").asLong())
              .findFirst()
              .orElse(null);
      if (source == null
          || !hash(source.sha256())
          || !source.sha256().equals(used.path("sha256").asText())
          || source.visualAssetId() != used.path("visualAssetId").asLong()
          || request.path("captionCueIndex").asInt() != used.path("captionCueIndex").asInt()
          || source.commercialPlanId() == null
          || source.commercialPlanId() <= 0
          || source.origin() == null
          || source.origin().isBlank()
          || source.rightsStatement() == null
          || source.rightsStatement().isBlank()) {
        blockers.add("Arte aplicada não comprova fonte aprovada do produto: trecho " + i);
        continue;
      }
      var item = new LinkedHashMap<String, Object>();
      item.put("visualAssetId", source.visualAssetId());
      item.put("commercialPlanId", source.commercialPlanId());
      item.put("url", source.url());
      item.put("sha256", source.sha256());
      item.put("origin", source.origin());
      item.put("rightsStatement", source.rightsStatement());
      item.put("captionCueIndex", used.path("captionCueIndex").asInt());
      result.add(item);
    }
  }

  /** Exige o projeto e o contrato exatos do produto e do experimento do criativo. */
  private boolean owned(Creative creative, VideoProject project, JsonNode lineage) {
    return project != null
        && project.getId() != null
        && project.getProductId() != null
        && project.getExperimentId() != null
        && creative.getExperiment().getProduct() != null
        && Objects.equals(project.getProductId(), creative.getExperiment().getProduct().getId())
        && Objects.equals(project.getExperimentId(), creative.getExperiment().getId())
        && lineage.path("productId").asLong() == project.getProductId()
        && lineage.path("experimentId").asLong() == project.getExperimentId()
        && Objects.equals(lineage.path("tenantId").asText(), project.getTenantId())
        && project.getCampaignKey() != null
        && project.getCampaignKey().equals(lineage.path("campaign_key").asText());
  }

  /** Confere o contexto persistido do job antes de usar seus artefatos como evidência. */
  private boolean sameContext(SalesVideoJob job, VideoProject project)
      throws JsonProcessingException {
    JsonNode context = read(job.getMetadataJson());
    return Objects.equals(job.getTenantId(), project.getTenantId())
        && context.path("videoProjectId").asLong() == project.getId()
        && context.path("productId").asLong() == project.getProductId()
        && context.path("experimentId").asLong() == project.getExperimentId()
        && (project.getCampaignKey().equals(context.path("campaign_key").asText())
            || !context.hasNonNull("campaign_key")
                && "EDITORIAL_MOTION".equals(job.getProviderName())
                && sameFrozenProof(context.at("/post_production/product_proof"), project));
  }

  /** Usa a versão congelada no contrato editorial original quando não existe campaign_key. */
  private boolean sameFrozenProof(JsonNode proof, VideoProject project) {
    return "PDE_PRIVATE_VIDEO_PROOF_V1".equals(proof.path("contractVersion").asText())
        && proof.path("projectId").asLong() == project.getId()
        && proof.path("productId").asLong() == project.getProductId()
        && proof.path("experimentId").asLong() == project.getExperimentId()
        && project.getTenantId().equals(proof.path("tenantId").asText())
        && project.getCampaignKey().equals(proof.path("productVersion").asText())
        && hash(proof.path("sha256").asText());
  }

  /** Expõe prova estruturada e motivos, reservando a decisão comercial ao revisor independente. */
  private CreativeMediaGovernanceEvidenceDto evidence(
      ExperimentVideoAsset video,
      VideoProject project,
      JsonNode lineage,
      MediaArtifact finalArtifact,
      MediaArtifact sourceArtifact,
      List<Map<String, Object>> proofs,
      List<Map<String, Object>> delivery,
      EditorialMediaEvidenceDto.AudioEvidence audio,
      List<String> blockers) {
    var details =
        new EditorialMediaEvidenceDto(
            project == null ? null : project.getId(),
            project == null ? null : project.getProductId(),
            project == null ? null : project.getExperimentId(),
            project == null ? null : project.getCampaignKey(),
            List.copyOf(proofs),
            List.copyOf(delivery),
            audio,
            List.copyOf(blockers));
    return new CreativeMediaGovernanceEvidenceDto(
        CONTRACT,
        blockers.isEmpty() ? "VERIFIED" : "INCOMPLETE",
        video.getId(),
        video.getSalesVideoJob() == null ? null : video.getSalesVideoJob().getId(),
        STRATEGY,
        "video-management-service/src/main/java/com/marketinghub/videomanagement/service/provider/EditorialMotionVideoProvider.java",
        List.of(),
        List.of(),
        finalArtifact,
        sourceArtifact,
        null,
        project == null ? null : project.getReferencePerformanceUri(),
        null,
        "Provas homologadas e artes aprovadas identificadas abaixo; direitos da voz limitados à política citada.",
        false,
        REFERENCE_MODE,
        audio != null && audio.disclosureVerified(),
        true,
        audio == null ? null : audio.license(),
        video.getReviewedBy(),
        video.getReviewedAt(),
        details);
  }

  /** Lê metadados do ativo sem afirmar que ausência comprova origem. */
  private JsonNode metadata(com.marketinghub.media.Asset asset) throws JsonProcessingException {
    return asset == null ? json.createObjectNode() : read(asset.getPayload()).path("metadata");
  }

  /** Interpreta somente a auditoria persistida; JSON inválido permanece bloqueante no chamador. */
  private JsonNode read(String value) throws JsonProcessingException {
    return value == null || value.isBlank() ? json.createObjectNode() : json.readTree(value);
  }

  /** Reconhece identidade SHA-256 completa em formato canônico. */
  private boolean hash(String value) {
    return value != null && value.matches("[a-f0-9]{64}");
  }
}
