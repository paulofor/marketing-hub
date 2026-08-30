package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;

/** Responsabilidade: validar a composição estética estruturada emitida pela Psique visual v4. */
final class PsiqueVisualCompositionContract {
  private static final List<String> SCORED_DIMENSIONS =
      List.of(
          "imageTextBalance",
          "mediaVariety",
          "visualRhythm",
          "colorStrategy",
          "typographicHierarchy",
          "densityAndBreathingRoom",
          "noveltyFamiliarity");
  private static final Set<String> ARCHETYPES =
      Set.of(
          "EDITORIAL_PORTAL",
          "PERSUASIVE_LANDING",
          "TRANSACTIONAL_DISCOVERY",
          "PRODUCT_SHOWCASE",
          "APPLICATION",
          "SINGLE_CREATIVE",
          "OTHER",
          "NOT_APPLICABLE");
  private static final Set<String> HUMAN_ROLES =
      Set.of(
          "NONE",
          "CUSTOMER_IDENTIFICATION",
          "PRODUCT_IN_USE",
          "SOCIAL_PROOF",
          "EXPERT_AUTHORITY",
          "ASPIRATIONAL_LIFESTYLE",
          "DECORATIVE",
          "MIXED");
  private static final Set<String> ABSENCE_IMPACTS = Set.of("NONE", "LOW", "MEDIUM", "HIGH");

  /** Impede instanciação do validador utilitário. */
  private PsiqueVisualCompositionContract() {}

  /** Exige que evidência visual e composição estética permaneçam semanticamente coerentes. */
  static void validate(JsonNode sensoryExperience) {
    PsiqueSensoryContract.validate(sensoryExperience);
    JsonNode composition = sensoryExperience.path("visualComposition");
    if (!composition.isObject()
        || !composition.path("applicable").isBoolean()
        || !ARCHETYPES.contains(composition.path("archetype").asText())
        || composition.path("strongestPattern").asText().isBlank()
        || !composition.path("criticalDeficitPresent").isBoolean()
        || composition.path("criticalDeficit").asText().isBlank()) {
      throw new IllegalArgumentException("Composição visual de Psique incompleta.");
    }
    for (String dimension : SCORED_DIMENSIONS) validateDimension(composition, dimension);
    validateHumanConnection(composition.path("humanConnection"));

    boolean visualAvailable = contains(sensoryExperience.path("availableModalities"), "VISUAL");
    boolean applicable = composition.path("applicable").asBoolean();
    if (visualAvailable != applicable) {
      throw new IllegalArgumentException(
          "Composição visual deve acompanhar exatamente a modalidade VISUAL disponível.");
    }
    if (applicable && "NOT_APPLICABLE".equals(composition.path("archetype").asText())) {
      throw new IllegalArgumentException("Composição aplicável exige arquétipo visual concreto.");
    }
    if (!applicable) validateNotApplicable(composition);
  }

  /**
   * Impede aprovação produtiva quando alguma dimensão estética aplicável permanece insuficiente.
   */
  static void requireApprovalThreshold(JsonNode sensoryExperience) {
    JsonNode composition = sensoryExperience.path("visualComposition");
    if (!composition.path("applicable").asBoolean(false)) return;
    for (String dimension : SCORED_DIMENSIONS) {
      if (composition.path(dimension).path("score").asInt(-1) < 3) {
        throw new IllegalArgumentException(
            "Parecer de Psique aprovou com dimensão estética abaixo do mínimo: " + dimension);
      }
    }
    if (composition.path("humanConnection").path("appropriatenessScore").asInt(-1) < 3
        || composition.path("criticalDeficitPresent").asBoolean(true)) {
      throw new IllegalArgumentException(
          "Parecer de Psique aprovou com conexão humana inadequada ou déficit visual crítico.");
    }
  }

  /** Valida uma dimensão estética pela escala de zero a cinco e por sua prova textual. */
  private static void validateDimension(JsonNode composition, String dimension) {
    JsonNode value = composition.path(dimension);
    if (!value.isObject()
        || !score(value.path("score"))
        || value.path("evidence").asText().isBlank()) {
      throw new IllegalArgumentException("Dimensão estética incompleta: " + dimension);
    }
  }

  /** Mantém presença, função e impacto de ausência humana no mesmo significado. */
  private static void validateHumanConnection(JsonNode human) {
    if (!human.isObject()
        || !human.path("peopleObserved").isBoolean()
        || !HUMAN_ROLES.contains(human.path("functionalRole").asText())
        || !score(human.path("appropriatenessScore"))
        || !ABSENCE_IMPACTS.contains(human.path("absenceImpact").asText())
        || human.path("evidence").asText().isBlank()) {
      throw new IllegalArgumentException("Conexão humana visual de Psique incompleta.");
    }
    boolean peopleObserved = human.path("peopleObserved").asBoolean();
    boolean noRole = "NONE".equals(human.path("functionalRole").asText());
    if (peopleObserved == noRole) {
      throw new IllegalArgumentException("Presença humana e função visual são incoerentes.");
    }
    if (!peopleObserved
        && "HIGH".equals(human.path("absenceImpact").asText())
        && human.path("appropriatenessScore").asInt() > 2) {
      throw new IllegalArgumentException(
          "Ausência humana de alto impacto não pode receber adequação de aprovação.");
    }
  }

  /** Exige zeros e marcadores neutros quando não existem pixels para avaliação estética. */
  private static void validateNotApplicable(JsonNode composition) {
    if (!"NOT_APPLICABLE".equals(composition.path("archetype").asText())
        || composition.path("criticalDeficitPresent").asBoolean()) {
      throw new IllegalArgumentException("Composição não aplicável possui classificação visual.");
    }
    for (String dimension : SCORED_DIMENSIONS) {
      if (composition.path(dimension).path("score").asInt(-1) != 0) {
        throw new IllegalArgumentException("Composição não aplicável não pode receber notas.");
      }
    }
    JsonNode human = composition.path("humanConnection");
    if (human.path("peopleObserved").asBoolean()
        || !"NONE".equals(human.path("functionalRole").asText())
        || human.path("appropriatenessScore").asInt(-1) != 0
        || !"NONE".equals(human.path("absenceImpact").asText())) {
      throw new IllegalArgumentException(
          "Composição não aplicável não pode inferir conexão humana.");
    }
  }

  /** Confirma presença de um valor textual em uma lista JSON. */
  private static boolean contains(JsonNode values, String expected) {
    if (!values.isArray()) return false;
    for (JsonNode value : values) {
      if (expected.equals(value.asText())) return true;
    }
    return false;
  }

  /** Confirma que uma nota inteira pertence à escala explícita de zero a cinco. */
  private static boolean score(JsonNode value) {
    return value.isIntegralNumber() && value.asInt() >= 0 && value.asInt() <= 5;
  }
}
