package com.marketinghub.creative.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.dto.CreativeMediaGovernanceEvidenceDto;
import com.marketinghub.creative.dto.CreativeMediaGovernanceEvidenceDto.MediaArtifact;
import com.marketinghub.creative.dto.CreativeMediaGovernanceEvidenceDto.MediaReference;
import com.marketinghub.creative.dto.CreativeMediaGovernanceEvidenceDto.ProviderLicense;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.media.Asset;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProviderModelRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.SalesVideoProviderModel;
import com.marketinghub.salesvideo.VideoProject;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsabilidade: resolver a prova auditável da mídia exata usada por um criativo de vídeo. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreativeMediaGovernanceEvidenceService {
  static final String CONTRACT_VERSION = "CREATIVE_MEDIA_GOVERNANCE_V1";
  static final String RUNWAY_COMMERCIAL_USE_POLICY_URL =
      "https://help.runwayml.com/hc/en-us/articles/21668707517587-Can-I-use-the-content-I-made-in-Runway-for-commercial-purposes";

  private final ExperimentVideoAssetRepository videoAssets;
  private final VideoProjectRepository videoProjects;
  private final AssetRepository assets;
  private final SalesVideoProviderModelRepository providerModels;
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
    String sourceProvider = text(lineage, "sourceProviderName");
    SalesVideoProviderModel providerModel =
        sourceProvider == null
            ? null
            : providerModels.findByProviderName(sourceProvider).orElse(null);
    MediaArtifact finalArtifact = artifact(finalAsset, mediaUrl, video.getProvider(), null);
    MediaArtifact generatedSource = artifact(sourceAsset, null, sourceProvider, sourceAssetId);
    MediaReference presenterReference =
        reference(presenterAsset, projectMatches ? project.getCharacterPerformanceUri() : null);
    ProviderLicense providerLicense = providerLicense(providerModel, sourceProvider);
    String consentEvidence = text(governance, "presenterConsentEvidence");
    String rightsEvidence = text(governance, "referenceRightsEvidence");
    boolean verified =
        video.getStatus() == ExperimentVideoStatus.READY
            && video.getReviewStatus() == ExperimentVideoReviewStatus.APPROVED
            && projectMatches
            && hasSha256(finalArtifact)
            && hasSha256(generatedSource)
            && presenterReference != null
            && StringUtils.hasText(presenterReference.generationReference())
            && StringUtils.hasText(presenterReference.generationPrompt())
            && StringUtils.hasText(consentEvidence)
            && StringUtils.hasText(rightsEvidence)
            && providerLicense != null
            && providerLicense.commercialLicenseVerified();
    return new CreativeMediaGovernanceEvidenceDto(
        CONTRACT_VERSION,
        verified ? "VERIFIED" : "INCOMPLETE",
        video.getId(),
        video.getSalesVideoJob() == null ? null : video.getSalesVideoJob().getId(),
        text(lineage, "generation_strategy"),
        finalArtifact,
        generatedSource,
        presenterReference,
        projectMatches ? trimToNull(project.getReferencePerformanceUri()) : null,
        consentEvidence,
        rightsEvidence,
        governance.path("productIsDigitalExperience").asBoolean(false),
        providerLicense,
        trimToNull(video.getReviewedBy()),
        video.getReviewedAt());
  }

  /** Converte o payload persistido de um asset em identidade imutável de arquivo. */
  private MediaArtifact artifact(
      Asset asset, String fallbackUrl, String fallbackProvider, Long fallbackSourceAssetId)
      throws JsonProcessingException {
    if (asset == null) {
      return new MediaArtifact(
          fallbackSourceAssetId, fallbackUrl, null, fallbackProvider, null, null);
    }
    JsonNode metadata = readObject(asset.getPayload()).path("metadata");
    return new MediaArtifact(
        asset.getId(),
        firstText(asset.getUrl(), fallbackUrl),
        text(metadata, "sha256"),
        firstText(
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
        mediaUrl == null ? null : new MediaArtifact(null, mediaUrl, null, null, null, null),
        null,
        null,
        null,
        null,
        null,
        false,
        null,
        null,
        null);
  }
}
