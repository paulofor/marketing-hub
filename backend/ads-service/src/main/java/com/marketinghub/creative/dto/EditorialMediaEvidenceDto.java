package com.marketinghub.creative.dto;

import java.util.List;
import java.util.Map;

/** Transporta fontes verificadas da montagem editorial sem inventar apresentador ou licença. */
public record EditorialMediaEvidenceDto(
    Long projectId,
    Long productId,
    Long experimentId,
    String productVersion,
    List<Map<String, Object>> productProofSources,
    List<Map<String, Object>> deliverySources,
    AudioEvidence audio,
    List<String> blockers) {

  /**
   * Identifica a voz incorporada, sua origem e a política verificada, sem alegar custo conciliado.
   */
  public record AudioEvidence(
      String provider,
      String model,
      String voice,
      boolean builtInVoice,
      boolean disclosureVerified,
      String costStatus,
      CreativeMediaGovernanceEvidenceDto.ProviderLicense license,
      List<AudioSegment> segments,
      List<String> blockers) {}

  /** Vincula cada trecho ao binário recebido e ao request original, sem expor credenciais. */
  public record AudioSegment(
      Long assetId,
      Long sourceJobId,
      int segmentIndex,
      String url,
      String sha256,
      String requestId,
      String inputTextSha256,
      double durationSeconds) {}
}
