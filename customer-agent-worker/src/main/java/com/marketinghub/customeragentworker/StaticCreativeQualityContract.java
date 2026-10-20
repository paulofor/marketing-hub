package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;

/** Responsabilidade: impedir aprovação estética baseada apenas na validade técnica de uma peça. */
final class StaticCreativeQualityContract {
  private static final Set<String> KINDS =
      Set.of(
          "TECHNICAL_PLACEHOLDER",
          "FUNCTIONAL_CAPTURE",
          "EDITORIAL_ILLUSTRATION",
          "PRODUCT_PHOTOGRAPHY",
          "OTHER");

  /** Impede instanciação do contrato sem estado. */
  private StaticCreativeQualityContract() {}

  /** Exige notas visuais explícitas nas imagens finais e preserva a revisão própria dos vídeos. */
  static void validate(
      JsonNode result, List<BpmVisualEvidenceBackendClient.UploadedVisualEvidence> images) {
    for (var image : images) {
      if (!"CREATIVE_RENDER".equals(image.evidenceType())) continue;
      JsonNode entry = null;
      for (JsonNode audit : result.path("renderedAssetAudit"))
        if (audit.path("artifactId").asLong() == image.id()) entry = audit;
      if (entry == null || !image.sha256().equals(entry.path("sha256").asText()))
        throw new IllegalArgumentException(
            "Qualidade estática sem identidade dos pixels recebidos.");
      JsonNode quality = entry.path("staticQuality");
      if (!KINDS.contains(quality.path("sourceKind").asText())
          || quality.path("assessment").asText().isBlank())
        throw new IllegalArgumentException(
            "Qualidade estática sem fonte e avaliação fundamentada.");
      boolean approved = "APPROVED".equals(result.path("decision").asText());
      if (approved && "TECHNICAL_PLACEHOLDER".equals(quality.path("sourceKind").asText()))
        throw new IllegalArgumentException(
            "Fixture técnica não pode receber aprovação de qualidade visual.");
      for (String dimension : List.of("craftsmanship", "desireConnection", "productClarity")) {
        JsonNode score = quality.path(dimension);
        if (!score.isIntegralNumber()
            || !score.canConvertToInt()
            || score.asInt() < 0
            || score.asInt() > 5
            || (approved && score.asInt() < 3))
          throw new IllegalArgumentException("Qualidade estática insuficiente: " + dimension);
      }
    }
  }
}
