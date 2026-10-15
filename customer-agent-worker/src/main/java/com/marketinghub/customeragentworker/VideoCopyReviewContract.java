package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Locale;

/**
 * Vincula a avaliação persuasiva de Psique à narração real, sem inferir qualidade por
 * palavras-chave.
 */
final class VideoCopyReviewContract {
  private static final List<String> CRITERIA =
      List.of("recognition", "humanConnection", "benefit", "simplicity", "proofBridge", "cta");

  /** Impede instanciar o contrato sem estado. */
  private VideoCopyReviewContract() {}

  /** Exige avaliação textual independente antes de aceitar a aprovação visual do vídeo. */
  static void validate(JsonNode media, JsonNode review, boolean approved) {
    JsonNode narration = media.path("narration");
    String source = narration.isTextual() ? normalize(narration.asText()) : "";
    if (!"VIDEO_COPY_REVIEW_V1".equals(review.path("contractVersion").asText())
        || !List.of("PERSUASIVE", "DESCRIPTIVE", "UNVERIFIABLE")
            .contains(review.path("mode").asText()))
      throw new IllegalArgumentException("Parecer sem contrato de copy audiovisual verificável.");
    if (approved && (source.isBlank() || !"PERSUASIVE".equals(review.path("mode").asText())))
      throw new IllegalArgumentException(
          "Copy descritiva ou sem narração comprovada impede aprovação.");
    for (String criterion : CRITERIA)
      validateCriterion(criterion, review.path(criterion), source, approved);
    JsonNode continuity = review.path("paidContinuity");
    if (!continuity.path("applicable").isBoolean())
      throw new IllegalArgumentException("Copy sem decisão sobre continuidade paga.");
    if (continuity.path("applicable").asBoolean()) {
      validateCriterion("paidContinuity", continuity, source, approved);
    } else if (!continuity.path("assessment").isTextual()
        || continuity.path("assessment").asText().isBlank()
        || !continuity.path("quote").isTextual()
        || !continuity.path("quote").asText().isBlank()
        || !continuity.path("score").isIntegralNumber()
        || continuity.path("score").asInt(-1) != 0) {
      throw new IllegalArgumentException(
          "Continuidade não aplicável exige justificativa e escore zero.");
    }
  }

  /** Confere escore, justificativa e citação literal; não permite inventar frases para aprovar. */
  private static void validateCriterion(
      String name, JsonNode value, String source, boolean approved) {
    JsonNode score = value.path("score");
    String quote = normalize(value.path("quote").asText());
    if (!value.path("quote").isTextual()
        || !value.path("assessment").isTextual()
        || !score.isIntegralNumber()
        || score.asInt() < 0
        || score.asInt() > 5
        || value.path("assessment").asText().isBlank())
      throw new IllegalArgumentException("Critério de copy incompleto: " + name);
    if ((!quote.isBlank() && !source.contains(quote)) || (score.asInt() >= 3 && quote.isBlank()))
      throw new IllegalArgumentException("Citação de copy ausente ou fora da narração: " + name);
    if (approved && score.asInt() < 3)
      throw new IllegalArgumentException("Critério de copy insuficiente: " + name);
  }

  /** Tolera caixa e espaços de formatação sem alterar palavras da evidência recebida. */
  private static String normalize(String value) {
    return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
  }
}
