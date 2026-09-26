package com.marketinghub.creative.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.dto.CreativeMediaGovernanceEvidenceDto;
import com.marketinghub.creative.dto.CreativeMediaGovernanceEvidenceDto.ApprovedCreativeSource;
import com.marketinghub.creative.dto.CreativeMediaGovernanceEvidenceDto.MediaArtifact;
import com.marketinghub.creative.dto.CreativeMediaGovernanceEvidenceDto.MediaReference;
import com.marketinghub.creative.dto.CreativeMediaGovernanceEvidenceDto.ProviderLicense;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.media.Asset;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProviderModelRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.SalesVideoProviderModel;
import com.marketinghub.salesvideo.VideoProject;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsabilidade: resolver a prova auditável da mídia exata usada por um criativo de vídeo. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreativeMediaGovernanceEvidenceService {
  static final String CONTRACT_VERSION = "CREATIVE_MEDIA_GOVERNANCE_V2";
  static final String VERSIONED_MONTAGE_CONTRACT_VERSION = "CREATIVE_MEDIA_GOVERNANCE_V3";
  static final String EXPLICIT_REFERENCE = "EXPLICIT_REFERENCE";
  static final String PROMPT_ONLY_SYNTHETIC = "PROMPT_ONLY_SYNTHETIC";
  static final String UNRESOLVED_REFERENCE = "UNRESOLVED";
  static final String APPROVED_PRODUCT_ASSETS = "APPROVED_PRODUCT_ASSETS";
  private static final Set<String> PROMPT_ONLY_INPUT_FIELDS =
      Set.of("promptText", "duration", "aspectRatio", "resolution", "audio");
  static final String RUNWAY_COMMERCIAL_USE_POLICY_URL =
      "https://help.runwayml.com/hc/en-us/articles/21668707517587-Can-I-use-the-content-I-made-in-Runway-for-commercial-purposes";

  private final ExperimentVideoAssetRepository videoAssets;
  private final VideoProjectRepository videoProjects;
  private final AssetRepository assets;
  private final SalesVideoProviderModelRepository providerModels;
  private final CreativeRepository creatives;
  private final ObjectMapper objectMapper;

  /** Monta evidência estruturada sem alterar o criativo e sinaliza qualquer lacuna ao revisor. */
  public CreativeMediaGovernanceEvidenceDto resolve(Creative creative) {
    if (!"VIDEO".equalsIgnoreCase(creative.getFormat())) {
      return null;
    }
    Long experimentId = creative.getExperiment().getId();
    String mediaUrl = trimToNull(creative.getVideoUrl());
    if (mediaUrl == null) {
      return unavailable("MEDIA_URL_MISSING", null);
    }
    ExperimentVideoAsset video =
        videoAssets
            .findFirstByExperimentIdAndAssetUrlOrderByIdDesc(experimentId, mediaUrl)
            .orElse(null);
    if (video == null) {
      return unavailable("NO_APPROVED_ASSET_MATCH", mediaUrl);
    }
    try {
      return resolvedEvidence(creative, video, mediaUrl);
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao interpretar auditoria da mídia do criativo; creativeId={} experimentId={} experimentVideoAssetId={} mediaUrl={}",
          creative.getId(),
          experimentId,
          video.getId(),
          mediaUrl,
          ex);
      return unavailable("INVALID_AUDIT_PAYLOAD", mediaUrl);
    }
  }

  /** Monta a projeção completa a partir do vídeo aprovado e dos artefatos que o originaram. */
  private CreativeMediaGovernanceEvidenceDto resolvedEvidence(
      Creative creative, ExperimentVideoAsset video, String mediaUrl)
      throws JsonProcessingException {
    JsonNode request = readObject(video.getRequestJson());
    if ("experiment.userAdVideoUpload.v2".equals(text(request, "artifactType"))) {
      return resolvedVersionedUpload(creative, video, mediaUrl, request);
    }
    JsonNode lineage = nestedObject(request, "postProductionMetadataJson");
    JsonNode governance = lineage.path("referenceGovernance");
    Long projectId = positiveLong(lineage.path("videoProjectId"));
    VideoProject project =
        projectId == null ? null : videoProjects.findById(projectId).orElse(null);
    boolean projectMatches =
        project != null
            && Objects.equals(project.getExperimentId(), creative.getExperiment().getId());
    Asset finalAsset = video.getAsset();
    Long sourceAssetId = positiveLong(lineage.path("sourceAssetId"));
    Asset sourceAsset = sourceAssetId == null ? null : assets.findById(sourceAssetId).orElse(null);
    Asset presenterAsset =
        projectMatches ? findAssetByUrl(project.getCharacterPerformanceUri()) : null;
    JsonNode sourceMetadata = assetMetadata(sourceAsset);
    JsonNode sourceProviderMetadata = sourceMetadata.path("provider_metadata");
    String sourceProviderFromAsset = text(sourceProviderMetadata, "provider");
    String sourceProvider = firstText(sourceProviderFromAsset, text(lineage, "sourceProviderName"));
    SalesVideoProviderModel providerModel =
        resolveProviderModel(sourceProvider, sourceProviderFromAsset, sourceProviderMetadata);
    MediaArtifact finalArtifact = artifact(finalAsset, mediaUrl, video.getProvider(), null);
    MediaArtifact generatedSource = artifact(sourceAsset, null, sourceProvider, sourceAssetId);
    MediaReference presenterReference =
        reference(presenterAsset, projectMatches ? project.getCharacterPerformanceUri() : null);
    ProviderLicense providerLicense = providerLicense(providerModel, sourceProvider);
    String consentEvidence = text(governance, "presenterConsentEvidence");
    String rightsEvidence = text(governance, "referenceRightsEvidence");
    boolean presenterIsSynthetic = governance.path("presenterIsSynthetic").asBoolean(false);
    String presenterReferenceMode =
        presenterReference != null
            ? EXPLICIT_REFERENCE
            : promptOnlySynthetic(lineage, presenterIsSynthetic)
                ? PROMPT_ONLY_SYNTHETIC
                : UNRESOLVED_REFERENCE;
    boolean syntheticMediaDisclosureVerified = syntheticDisclosureVerified(finalAsset);
    boolean referenceEvidenceVerified =
        EXPLICIT_REFERENCE.equals(presenterReferenceMode)
            ? completePresenterReference(presenterReference)
                && StringUtils.hasText(consentEvidence)
                && StringUtils.hasText(rightsEvidence)
            : PROMPT_ONLY_SYNTHETIC.equals(presenterReferenceMode)
                && syntheticMediaDisclosureVerified;
    boolean verified =
        video.getStatus() == ExperimentVideoStatus.READY
            && video.getReviewStatus() == ExperimentVideoReviewStatus.APPROVED
            && projectMatches
            && hasSha256(finalArtifact)
            && hasSha256(generatedSource)
            && StringUtils.hasText(finalArtifact.providerTaskId())
            && StringUtils.hasText(generatedSource.providerTaskId())
            && referenceEvidenceVerified
            && providerCommerciallyApproved(providerModel);
    return new CreativeMediaGovernanceEvidenceDto(
        CONTRACT_VERSION,
        verified ? "VERIFIED" : "INCOMPLETE",
        video.getId(),
        video.getSalesVideoJob() == null ? null : video.getSalesVideoJob().getId(),
        text(lineage, "generation_strategy"),
        null,
        List.of(),
        finalArtifact,
        generatedSource,
        presenterReference,
        projectMatches ? trimToNull(project.getReferencePerformanceUri()) : null,
        consentEvidence,
        rightsEvidence,
        presenterIsSynthetic,
        presenterReferenceMode,
        syntheticMediaDisclosureVerified,
        governance.path("productIsDigitalExperience").asBoolean(false),
        providerLicense,
        trimToNull(video.getReviewedBy()),
        video.getReviewedAt());
  }

  /**
   * Resolve uma montagem local somente a partir de fontes já aprovadas e do arquivo final exato.
   */
  private CreativeMediaGovernanceEvidenceDto resolvedVersionedUpload(
      Creative creative, ExperimentVideoAsset video, String mediaUrl, JsonNode request)
      throws JsonProcessingException {
    Asset finalAsset = video.getAsset();
    MediaArtifact finalArtifact = artifact(finalAsset, mediaUrl, video.getProvider(), null);
    String productionReference = text(request, "productionReference");
    ResolvedApprovedSources sources =
        resolveApprovedSources(request.path("approvedSourceCreatives"), creative);
    String generationStrategy = text(request, "generationStrategy");
    boolean verified =
        video.getStatus() == ExperimentVideoStatus.READY
            && video.getReviewStatus() == ExperimentVideoReviewStatus.APPROVED
            && "VERSIONED_FFMPEG_MONTAGE_V1".equals(video.getModel())
            && "VERSIONED_APPROVED_CREATIVE_MONTAGE".equals(generationStrategy)
            && hasSha256(finalArtifact)
            && isVersionedProductionReference(productionReference)
            && sources.complete();
    return new CreativeMediaGovernanceEvidenceDto(
        VERSIONED_MONTAGE_CONTRACT_VERSION,
        verified ? "VERIFIED" : "INCOMPLETE",
        video.getId(),
        null,
        generationStrategy,
        productionReference,
        sources.sources(),
        finalArtifact,
        null,
        null,
        null,
        null,
        "APPROVED_SOURCE_CREATIVES",
        false,
        APPROVED_PRODUCT_ASSETS,
        false,
        false,
        null,
        trimToNull(video.getReviewedBy()),
        video.getReviewedAt());
  }

  /** Confere novamente no banco cada criativo registrado no snapshot do upload. */
  private ResolvedApprovedSources resolveApprovedSources(JsonNode nodes, Creative target) {
    if (!nodes.isArray() || nodes.isEmpty() || nodes.size() > 10) {
      return new ResolvedApprovedSources(List.of(), false);
    }
    Long permittedExperimentId =
        target.getExperiment().getSourceExperiment() == null
            ? target.getExperiment().getId()
            : target.getExperiment().getSourceExperiment().getId();
    List<ApprovedCreativeSource> resolved = new java.util.ArrayList<>();
    Set<Long> seenCreativeIds = new java.util.HashSet<>();
    boolean complete = true;
    for (JsonNode node : nodes) {
      Long creativeId = positiveLong(node.path("creativeId"));
      Long experimentId = positiveLong(node.path("experimentId"));
      Instant snapshotReviewedAt = reviewedInstant(node.path("reviewedAt"));
      ApprovedCreativeSource snapshot =
          new ApprovedCreativeSource(
              creativeId,
              experimentId,
              text(node, "format"),
              text(node, "mediaUrl"),
              text(node, "status"),
              text(node, "agentReviewStatus"),
              snapshotReviewedAt == null ? null : snapshotReviewedAt.toString());
      resolved.add(snapshot);
      Creative source =
          creativeId == null ? null : creatives.findByIdWithExperiment(creativeId).orElse(null);
      String actualMediaUrl =
          source == null
              ? null
              : "VIDEO".equalsIgnoreCase(source.getFormat())
                  ? trimToNull(source.getVideoUrl())
                  : trimToNull(source.getImageUrl());
      complete =
          complete
              && creativeId != null
              && seenCreativeIds.add(creativeId)
              && source != null
              && Objects.equals(experimentId, permittedExperimentId)
              && source.getExperiment() != null
              && Objects.equals(source.getExperiment().getId(), permittedExperimentId)
              && Objects.equals(trimToNull(source.getFormat()), snapshot.format())
              && Objects.equals(actualMediaUrl, snapshot.mediaUrl())
              && source.getStatus() != null
              && Objects.equals(source.getStatus().name(), snapshot.status())
              && "READY".equals(snapshot.status())
              && source.getAgentReviewStatus() != null
              && Objects.equals(source.getAgentReviewStatus().name(), snapshot.agentReviewStatus())
              && "APPROVED".equals(snapshot.agentReviewStatus())
              && source.getReviewedAt() != null
              && Objects.equals(source.getReviewedAt(), snapshotReviewedAt);
    }
    return new ResolvedApprovedSources(List.copyOf(resolved), complete);
  }

  /** Aceita apenas um caminho versionável do repositório, sem URL ou travessia de diretório. */
  private boolean isVersionedProductionReference(String value) {
    return value != null
        && !value.startsWith("/")
        && !value.contains("..")
        && !value.contains("://")
        && value.matches("[A-Za-z0-9._/-]+\\.(sh|js|mjs|ts|py)");
  }

  /** Responsabilidade: agrupar as fontes resolvidas e o resultado da verificação cruzada. */
  private record ResolvedApprovedSources(List<ApprovedCreativeSource> sources, boolean complete) {}

  /** Converte o payload persistido de um asset em identidade imutável de arquivo. */
  private MediaArtifact artifact(
      Asset asset, String fallbackUrl, String fallbackProvider, Long fallbackSourceAssetId)
      throws JsonProcessingException {
    if (asset == null) {
      return new MediaArtifact(
          fallbackSourceAssetId, fallbackUrl, null, fallbackProvider, null, null);
    }
    JsonNode metadata = assetMetadata(asset);
    JsonNode providerMetadata = metadata.path("provider_metadata");
    return new MediaArtifact(
        asset.getId(),
        firstText(asset.getUrl(), fallbackUrl),
        text(metadata, "sha256"),
        firstText(
            text(providerMetadata, "provider"),
            text(metadata, "provider"),
            asset.getProvider() == null ? null : asset.getProvider().name(),
            fallbackProvider),
        text(metadata, "provider_job_id"),
        positiveLong(metadata.path("source_asset_id")));
  }

  /** Converte o asset de referência em prova de geração sintética auditável. */
  private MediaReference reference(Asset asset, String fallbackUrl) throws JsonProcessingException {
    if (asset == null) {
      return null;
    }
    JsonNode payload = readObject(asset.getPayload());
    return new MediaReference(
        asset.getId(),
        firstText(asset.getUrl(), fallbackUrl),
        asset.getProvider() == null ? null : asset.getProvider().name(),
        trimToNull(asset.getModel()),
        firstText(text(payload, "generation_job_id"), asset.getExternalId()),
        trimToNull(asset.getPrompt()));
  }

  /** Resolve a fonte oficial de licença somente a partir da curadoria vigente do provedor. */
  private ProviderLicense providerLicense(
      SalesVideoProviderModel providerModel, String sourceProvider) {
    if (providerModel == null) {
      return null;
    }
    String evidenceUrl =
        sourceProvider != null && sourceProvider.startsWith("RUNWAY")
            ? RUNWAY_COMMERCIAL_USE_POLICY_URL
            : trimToNull(providerModel.getDocumentationUrl());
    return new ProviderLicense(
        providerModel.getCode(),
        providerModel.getProviderName(),
        providerModel.isCommercialLicenseVerified(),
        evidenceUrl,
        providerModel.getUpdatedAt());
  }

  /** Resolve o modelo exato escolhido pelo Router sem atribuir licença a um modelo diferente. */
  private SalesVideoProviderModel resolveProviderModel(
      String sourceProvider, String sourceProviderFromAsset, JsonNode sourceProviderMetadata) {
    if (sourceProvider == null) {
      return null;
    }
    SalesVideoProviderModel model = providerModels.findByProviderName(sourceProvider).orElse(null);
    if (model == null || sourceProviderFromAsset == null) {
      return model;
    }
    String externalModel = text(sourceProviderMetadata, "model");
    return StringUtils.hasText(externalModel)
            && externalModel.equalsIgnoreCase(model.getExternalModelId())
        ? model
        : null;
  }

  /** Confirma que a curadoria vigente libera tecnicamente o provedor para uso comercial. */
  private boolean providerCommerciallyApproved(SalesVideoProviderModel providerModel) {
    return providerModel != null
        && "ACTIVE".equalsIgnoreCase(providerModel.getLifecycleStatus())
        && providerModel.isAdapterVerified()
        && providerModel.isQualityGateVerified()
        && providerModel.isCommercialLicenseVerified();
  }

  /** Confirma a cadeia completa quando uma imagem ou performance externa guiou o vídeo. */
  private boolean completePresenterReference(MediaReference presenterReference) {
    return presenterReference != null
        && StringUtils.hasText(presenterReference.generationReference())
        && StringUtils.hasText(presenterReference.generationPrompt());
  }

  /**
   * Reconhece geração sintética por texto somente quando nenhuma entrada aceita referência externa.
   */
  private boolean promptOnlySynthetic(JsonNode lineage, boolean presenterIsSynthetic)
      throws JsonProcessingException {
    if (!presenterIsSynthetic
        || !"PROVIDER_CLIPS_WITH_POST_PRODUCTION_CUTS"
            .equals(text(lineage, "generation_strategy"))) {
      return false;
    }
    JsonNode requests = embeddedJson(lineage.path("runwayRouterRequestsJson"));
    if (!requests.isArray() || requests.isEmpty()) {
      return false;
    }
    for (JsonNode request : requests) {
      JsonNode input = request.path("input");
      if (!input.isObject() || !StringUtils.hasText(text(input, "promptText"))) {
        return false;
      }
      var fields = input.fieldNames();
      while (fields.hasNext()) {
        if (!PROMPT_ONLY_INPUT_FIELDS.contains(fields.next())) {
          return false;
        }
      }
    }
    return true;
  }

  /** Exige disclosure inseparável da peça quando não existe pessoa ou imagem de referência. */
  private boolean syntheticDisclosureVerified(Asset finalAsset) throws JsonProcessingException {
    JsonNode disclosure =
        assetMetadata(finalAsset).path("provider_metadata").path("synthetic_media_disclosure");
    return disclosure.path("presenter_synthetic").asBoolean(false)
        && disclosure.path("required").asBoolean(false)
        && StringUtils.hasText(text(disclosure, "text"));
  }

  /** Lê o objeto metadata do ativo sem confundir ausência com evidência vazia. */
  private JsonNode assetMetadata(Asset asset) throws JsonProcessingException {
    return asset == null
        ? objectMapper.createObjectNode()
        : readObject(asset.getPayload()).path("metadata");
  }

  /** Interpreta JSON já estruturado ou serializado como texto no snapshot auditável. */
  private JsonNode embeddedJson(JsonNode value) throws JsonProcessingException {
    return value.isTextual() ? objectMapper.readTree(value.asText()) : value;
  }

  /** Localiza uma referência pela URL canônica sem buscar todos os assets em memória. */
  private Asset findAssetByUrl(String url) {
    String normalized = trimToNull(url);
    if (normalized == null) {
      return null;
    }
    return assets.findByUrlIn(List.of(normalized)).stream().findFirst().orElse(null);
  }

  /** Lê um objeto JSON persistido e usa objeto vazio quando o campo opcional não existe. */
  private JsonNode readObject(String value) throws JsonProcessingException {
    if (!StringUtils.hasText(value)) {
      return objectMapper.createObjectNode();
    }
    return objectMapper.readTree(value);
  }

  /** Lê um objeto JSON armazenado como texto dentro do snapshot externo. */
  private JsonNode nestedObject(JsonNode parent, String field) throws JsonProcessingException {
    JsonNode value = parent.path(field);
    if (value.isObject()) {
      return value;
    }
    return StringUtils.hasText(value.asText()) ? objectMapper.readTree(value.asText()) : parent;
  }

  /** Retorna texto normalizado do campo sem converter valores ausentes em string vazia. */
  private String text(JsonNode parent, String field) {
    JsonNode value = parent.path(field);
    return value.isTextual() ? trimToNull(value.asText()) : null;
  }

  /** Normaliza datas ISO e snapshots numéricos legados do Jackson para o mesmo instante. */
  private Instant reviewedInstant(JsonNode value) {
    try {
      if (value.isTextual()) {
        String normalized = trimToNull(value.asText());
        return normalized == null ? null : Instant.parse(normalized);
      }
      if (value.isNumber()) {
        BigDecimal[] parts = value.decimalValue().divideAndRemainder(BigDecimal.ONE);
        long seconds = parts[0].longValueExact();
        int nanos = parts[1].movePointRight(9).intValueExact();
        return Instant.ofEpochSecond(seconds, nanos);
      }
      return null;
    } catch (ArithmeticException | DateTimeException ex) {
      return null;
    }
  }

  /** Retorna o primeiro texto não vazio dentre as fontes auditáveis informadas. */
  private String firstText(String... values) {
    for (String value : values) {
      String normalized = trimToNull(value);
      if (normalized != null) {
        return normalized;
      }
    }
    return null;
  }

  /** Converte somente números inteiros positivos em identificadores válidos. */
  private Long positiveLong(JsonNode value) {
    return value.canConvertToLong() && value.asLong() > 0 ? value.asLong() : null;
  }

  /** Confirma que o artefato possui SHA-256 completo em minúsculas. */
  private boolean hasSha256(MediaArtifact artifact) {
    return artifact != null
        && artifact.sha256() != null
        && artifact.sha256().matches("[0-9a-f]{64}");
  }

  /** Normaliza texto opcional preservando conteúdo não vazio. */
  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  /** Produz um contrato bloqueante quando a prova exata não pode ser resolvida. */
  private CreativeMediaGovernanceEvidenceDto unavailable(String status, String mediaUrl) {
    return new CreativeMediaGovernanceEvidenceDto(
        CONTRACT_VERSION,
        status,
        null,
        null,
        null,
        null,
        List.of(),
        mediaUrl == null ? null : new MediaArtifact(null, mediaUrl, null, null, null, null),
        null,
        null,
        null,
        null,
        null,
        false,
        UNRESOLVED_REFERENCE,
        false,
        false,
        null,
        null,
        null);
  }
}
