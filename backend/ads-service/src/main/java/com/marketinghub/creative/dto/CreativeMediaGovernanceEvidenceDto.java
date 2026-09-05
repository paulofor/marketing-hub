package com.marketinghub.creative.dto;

import java.time.Instant;

/**
 * Responsabilidade: transportar a linhagem e os direitos da mídia exata submetida à revisão
 * comercial.
 */
public record CreativeMediaGovernanceEvidenceDto(
    String contractVersion,
    String verificationStatus,
    Long experimentVideoAssetId,
    Long salesVideoJobId,
    String generationStrategy,
    MediaArtifact finalArtifact,
    MediaArtifact generatedSourceArtifact,
    MediaReference presenterReference,
    String productReferenceUrl,
    String presenterConsentEvidence,
    String referenceRightsEvidence,
    boolean productIsDigitalExperience,
    ProviderLicense providerLicense,
    String approvedBy,
    Instant approvedAt) {

  /** Responsabilidade: identificar um arquivo de vídeo por origem, tarefa e conteúdo imutável. */
  public record MediaArtifact(
      Long assetId,
      String url,
      String sha256,
      String provider,
      String providerTaskId,
      Long sourceAssetId) {}

  /** Responsabilidade: comprovar a origem sintética da referência de apresentadora. */
  public record MediaReference(
      Long assetId,
      String url,
      String provider,
      String model,
      String generationReference,
      String generationPrompt) {}

  /** Responsabilidade: identificar a curadoria e a fonte oficial da licença do provedor. */
  public record ProviderLicense(
      String catalogCode,
      String providerName,
      boolean commercialLicenseVerified,
      String evidenceUrl,
      Instant catalogVerifiedAt) {}
}
