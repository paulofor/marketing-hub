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
      String narrationText,
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
      double durationSeconds,
      String text,
      SpeechReceipt originalReceipt) {}

  /** Expõe somente campos permitidos do recibo que originou o trecho, sem credenciais ou custo. */
  public record SpeechReceipt(
      String operation,
      String provider,
      String endpoint,
      String status,
      SpeechRequest request,
      SpeechResponse response) {}

  /** Conserva o texto e os parâmetros originais da voz nativa para comparação independente. */
  public record SpeechRequest(
      String model,
      String voice,
      String input,
      String instructions,
      String responseFormat,
      Double speed) {}

  /** Identifica a resposta binária auditada sem serializar áudio ou qualquer cabeçalho secreto. */
  public record SpeechResponse(
      String representation,
      String fileName,
      String contentType,
      long bytes,
      String sha256,
      String requestId) {}
}
