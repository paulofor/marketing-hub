package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Set;

/**
 * Responsabilidade: validar a coerência determinística da experiência sensorial emitida por Psique.
 */
final class PsiqueSensoryContract {
  private static final Set<String> MODALITIES =
      Set.of("VISUAL", "AUDIO", "MOTION", "TACTILE_IMAGERY");

  /** Impede instanciação do validador utilitário. */
  private PsiqueSensoryContract() {}

  /** Exige evidência, modalidades e escalas coerentes sem transformar ausência em nota fictícia. */
  static void validate(JsonNode experience) {
    if (!experience.isObject()
        || !experience.path("evidenceAvailable").isBoolean()
        || !experience.path("availableModalities").isArray()
        || !experience.path("pleasureByModality").isArray()
        || !score(experience.path("processingFluency"))
        || !score(experience.path("sensoryCongruence"))
        || !score(experience.path("overloadRisk"))
        || experience.path("embodiedAnticipation").asText().isBlank()
        || experience.path("dominantCue").asText().isBlank()
        || experience.path("evidenceBoundary").asText().isBlank()) {
      throw new IllegalArgumentException("Experiência sensorial de Psique incompleta.");
    }

    boolean evidenceAvailable = experience.path("evidenceAvailable").asBoolean();
    Set<String> available = new HashSet<>();
    experience
        .path("availableModalities")
        .forEach(
            modality -> {
              String value = modality.asText();
              if (!MODALITIES.contains(value) || !available.add(value)) {
                throw new IllegalArgumentException("Modalidade sensorial inválida ou duplicada.");
              }
            });

    Set<String> assessed = new HashSet<>();
    experience
        .path("pleasureByModality")
        .forEach(
            item -> {
              String modality = item.path("modality").asText();
              if (!item.isObject()
                  || !available.contains(modality)
                  || !assessed.add(modality)
                  || !score(item.path("pleasureScore"))
                  || item.path("evidence").asText().isBlank()) {
                throw new IllegalArgumentException(
                    "Prazer sensorial sem modalidade e evidência coerentes.");
              }
            });

    if (evidenceAvailable && (available.isEmpty() || !assessed.equals(available))) {
      throw new IllegalArgumentException(
          "Evidência sensorial disponível sem avaliação por modalidade.");
    }
    if (!evidenceAvailable
        && (!available.isEmpty()
            || !assessed.isEmpty()
            || experience.path("processingFluency").asInt() != 0
            || experience.path("sensoryCongruence").asInt() != 0
            || experience.path("overloadRisk").asInt() != 0)) {
      throw new IllegalArgumentException(
          "Ausência sensorial não pode receber modalidades ou notas.");
    }
  }

  /** Confirma que uma nota inteira pertence à escala explícita de zero a cinco. */
  private static boolean score(JsonNode value) {
    return value.isIntegralNumber() && value.asInt() >= 0 && value.asInt() <= 5;
  }
}
