package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a avaliação estética contextual e auditável da Psique v4. */
class PsiqueVisualCompositionContractTest {
  private static final List<String> VISUAL_SCHEMAS =
      List.of(
          "src/main/resources/prompts/customer-agent/behavioral-v4/evaluation-schema.json",
          "src/main/resources/prompts/customer-agent/v3/digital-observation-schema.json",
          "src/main/resources/prompts/bpm/v3/creative-customer-review-schema.json",
          "src/main/resources/prompts/bpm/v3/landing-customer-review-schema.json",
          "src/main/resources/prompts/bpm/v3/pde-experience-review-schema.json",
          "src/main/resources/prompts/bpm/v3/pde-commercial-homologation-customer-review-schema.json");
  private final ObjectMapper mapper = new ObjectMapper();

  /** Aceita paleta contida e ausência humana quando ambas servem ao arquétipo observado. */
  @Test
  void acceptsContextualVisualCompositionWithoutPeopleQuota() throws Exception {
    JsonNode experience = visualExperience(4, false, "NONE", "LOW", false);

    PsiqueVisualCompositionContract.validate(experience);
    PsiqueVisualCompositionContract.requireApprovalThreshold(experience);
  }

  /** Aceita pessoas quando sua função demonstra uso e permanece coerente com a promessa. */
  @Test
  void acceptsFunctionalHumanPresence() throws Exception {
    JsonNode experience = visualExperience(4, true, "PRODUCT_IN_USE", "NONE", false);

    PsiqueVisualCompositionContract.validate(experience);
  }

  /** Rejeita pixels declarados sem a composição estética que deveria explicá-los. */
  @Test
  void rejectsVisualEvidenceWithoutComposition() throws Exception {
    JsonNode experience = visualExperience(4, false, "NONE", "LOW", false);
    ((com.fasterxml.jackson.databind.node.ObjectNode) experience).remove("visualComposition");

    assertThatThrownBy(() -> PsiqueVisualCompositionContract.validate(experience))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Composição visual");
  }

  /** Rejeita função humana inventada quando nenhum rosto ou pessoa foi observado. */
  @Test
  void rejectsHumanRoleWithoutObservedPeople() throws Exception {
    JsonNode experience = visualExperience(4, false, "SOCIAL_PROOF", "LOW", false);

    assertThatThrownBy(() -> PsiqueVisualCompositionContract.validate(experience))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Presença humana");
  }

  /** Rejeita aprovação quando uma dimensão visual continua abaixo do mínimo funcional. */
  @Test
  void rejectsApprovalWithWeakVisualDimension() throws Exception {
    JsonNode experience = visualExperience(2, true, "CUSTOMER_IDENTIFICATION", "NONE", false);

    PsiqueVisualCompositionContract.validate(experience);
    assertThatThrownBy(() -> PsiqueVisualCompositionContract.requireApprovalThreshold(experience))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("imageTextBalance");
  }

  /** Rejeita aprovação quando o próprio parecer reconhece déficit visual crítico. */
  @Test
  void rejectsApprovalWithCriticalVisualDeficit() throws Exception {
    JsonNode experience = visualExperience(4, true, "PRODUCT_IN_USE", "NONE", true);

    PsiqueVisualCompositionContract.validate(experience);
    assertThatThrownBy(() -> PsiqueVisualCompositionContract.requireApprovalThreshold(experience))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("déficit visual crítico");
  }

  /** Mantém todos os schemas v4 estritos, tipados e com as mesmas dimensões estéticas. */
  @Test
  void keepsEveryVisualSchemaStrictAndProviderCompatible() throws Exception {
    for (String resource : VISUAL_SCHEMAS) {
      JsonNode schema = mapper.readTree(Path.of(resource).toFile());
      assertStrictObjects(schema, resource);
      assertTypedConstants(schema, resource);
      assertThat(schema.toString())
          .as("Schema visual deve expor o contrato: %s", resource)
          .contains(
              "visualComposition",
              "imageTextBalance",
              "mediaVariety",
              "visualRhythm",
              "colorStrategy",
              "typographicHierarchy",
              "densityAndBreathingRoom",
              "noveltyFamiliarity",
              "humanConnection")
          .doesNotContain("\"oneOf\"", "\"anyOf\"", "\"allOf\"", "\"uniqueItems\"");
    }
  }

  /** Cria uma experiência visual completa para exercitar coerência e gates do contrato. */
  private JsonNode visualExperience(
      int score,
      boolean peopleObserved,
      String functionalRole,
      String absenceImpact,
      boolean criticalDeficit)
      throws Exception {
    return mapper.readTree(
        """
        {
          "evidenceAvailable":true,
          "availableModalities":["VISUAL"],
          "pleasureByModality":[
            {"modality":"VISUAL","pleasureScore":4,"evidence":"Pixels da página foram inspecionados"}
          ],
          "processingFluency":4,
          "sensoryCongruence":4,
          "overloadRisk":1,
          "embodiedAnticipation":"Imagino avançar pela jornada sem esforço",
          "dominantCue":"Demonstração visual do resultado",
          "evidenceBoundary":"Captura mobile fornecida na própria execução",
          "visualComposition":{
            "applicable":true,
            "archetype":"PERSUASIVE_LANDING",
            "imageTextBalance":{"score":%d,"evidence":"Texto e imagem alternam função ao longo da página"},
            "mediaVariety":{"score":4,"evidence":"Demonstração, prova e contexto têm funções diferentes"},
            "visualRhythm":{"score":4,"evidence":"Seções densas alternam com respiro e ação clara"},
            "colorStrategy":{"score":4,"evidence":"A cor principal orienta título, prova e ação sem ruído"},
            "typographicHierarchy":{"score":4,"evidence":"Título, apoio, corpo e ação possuem pesos distinguíveis"},
            "densityAndBreathingRoom":{"score":4,"evidence":"Blocos permanecem escaneáveis no viewport mobile"},
            "noveltyFamiliarity":{"score":4,"evidence":"Padrões familiares sustentam uma demonstração nova"},
            "humanConnection":{
              "peopleObserved":%s,
              "functionalRole":"%s",
              "appropriatenessScore":4,
              "absenceImpact":"%s",
              "evidence":"A presença ou ausência humana foi julgada pela função na promessa"
            },
            "strongestPattern":"A hierarquia conduz da promessa até a demonstração e a ação",
            "criticalDeficitPresent":%s,
            "criticalDeficit":"%s"
          }
        }
        """
            .formatted(
                score,
                peopleObserved,
                functionalRole,
                absenceImpact,
                criticalDeficit,
                criticalDeficit
                    ? "A composição contém um déficit visual que impede aprovação"
                    : "Nenhum déficit visual crítico foi observado na evidência"));
  }

  /** Confirma fechamento e obrigatoriedade de todos os campos em objetos do schema. */
  private void assertStrictObjects(JsonNode node, String resource) {
    if (node.isObject() && "object".equals(node.path("type").asText())) {
      assertThat(node.path("additionalProperties").asBoolean())
          .as("Objeto deve ser fechado em %s", resource)
          .isFalse();
      if (node.path("properties").isObject()) {
        assertThat(node.path("required").isArray())
            .as("Objeto deve declarar required em %s", resource)
            .isTrue();
        Set<String> required = new HashSet<>();
        node.path("required").forEach(field -> required.add(field.asText()));
        node.path("properties")
            .fieldNames()
            .forEachRemaining(
                property ->
                    assertThat(required)
                        .as("Campo %s deve ser obrigatório em %s", property, resource)
                        .contains(property));
      }
    }
    node.elements().forEachRemaining(child -> assertStrictObjects(child, resource));
  }

  /** Exige tipo explícito em toda constante enviada ao Structured Outputs. */
  private void assertTypedConstants(JsonNode node, String resource) {
    if (node.isObject() && node.has("const")) {
      assertThat(node.hasNonNull("type"))
          .as("Constante deve declarar tipo explícito em %s: %s", resource, node)
          .isTrue();
    }
    node.elements().forEachRemaining(child -> assertTypedConstants(child, resource));
  }
}
