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

/** Responsabilidade: proteger o contrato sensorial v3 de Psique contra deriva entre fluxos. */
class PsiqueSensoryContractTest {
  private static final List<String> SENSORY_SCHEMAS =
      List.of(
          "src/main/resources/prompts/customer-agent/behavioral-v3/evaluation-schema.json",
          "src/main/resources/prompts/customer-agent/v2/digital-observation-schema.json",
          "src/main/resources/prompts/opportunity-review/v2/review-schema.json",
          "src/main/resources/prompts/bpm/v2/creative-customer-review-schema.json",
          "src/main/resources/prompts/bpm/v2/landing-customer-review-schema.json",
          "src/main/resources/prompts/bpm/v2/pde-experience-review-schema.json",
          "src/main/resources/prompts/bpm/v2/pde-commercial-homologation-customer-review-schema.json");
  private final ObjectMapper mapper = new ObjectMapper();

  /** Aceita avaliação visual completa com escalas e evidência ligadas à mesma modalidade. */
  @Test
  void acceptsEvidenceBoundVisualExperience() throws Exception {
    PsiqueSensoryContract.validate(
        mapper.readTree(
            """
            {
              "evidenceAvailable":true,
              "availableModalities":["VISUAL","MOTION"],
              "pleasureByModality":[
                {"modality":"VISUAL","pleasureScore":4,"evidence":"Contraste e hierarquia claros"},
                {"modality":"MOTION","pleasureScore":3,"evidence":"Transição curta sem distração"}
              ],
              "processingFluency":5,
              "sensoryCongruence":4,
              "overloadRisk":1,
              "embodiedAnticipation":"Imagino tocar e avançar pelo CTA",
              "dominantCue":"Demonstração do resultado",
              "evidenceBoundary":"Screenshot e captura de movimento fornecidos"
            }
            """));
  }

  /** Aceita ausência explícita sem converter falta de estímulo em avaliação estética. */
  @Test
  void acceptsExplicitlyUnavailableSensoryEvidence() throws Exception {
    PsiqueSensoryContract.validate(
        mapper.readTree(
            """
            {
              "evidenceAvailable":false,
              "availableModalities":[],
              "pleasureByModality":[],
              "processingFluency":0,
              "sensoryCongruence":0,
              "overloadRisk":0,
              "embodiedAnticipation":"Não observável",
              "dominantCue":"Não observado",
              "evidenceBoundary":"Dossiê sem ativo sensorial"
            }
            """));
  }

  /** Rejeita notas inventadas quando nenhuma modalidade foi observada. */
  @Test
  void rejectsScoresWithoutSensoryEvidence() throws Exception {
    JsonNode result =
        mapper.readTree(
            """
            {
              "evidenceAvailable":false,
              "availableModalities":[],
              "pleasureByModality":[],
              "processingFluency":4,
              "sensoryCongruence":4,
              "overloadRisk":1,
              "embodiedAnticipation":"Parece agradável",
              "dominantCue":"Suposto visual",
              "evidenceBoundary":"Sem screenshot"
            }
            """);

    assertThatThrownBy(() -> PsiqueSensoryContract.validate(result))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Ausência sensorial");
  }

  /** Exige que cada modalidade declarada possua uma avaliação de prazer correspondente. */
  @Test
  void rejectsUnassessedModality() throws Exception {
    JsonNode result =
        mapper.readTree(
            """
            {
              "evidenceAvailable":true,
              "availableModalities":["VISUAL","AUDIO"],
              "pleasureByModality":[{"modality":"VISUAL","pleasureScore":4,"evidence":"Imagem clara"}],
              "processingFluency":4,
              "sensoryCongruence":4,
              "overloadRisk":1,
              "embodiedAnticipation":"Uso imaginado",
              "dominantCue":"Imagem",
              "evidenceBoundary":"Imagem e áudio fornecidos"
            }
            """);

    assertThatThrownBy(() -> PsiqueSensoryContract.validate(result))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("avaliação por modalidade");
  }

  /** Mantém os schemas sensoriais estritos e compatíveis com Structured Outputs do Codex. */
  @Test
  void keepsEverySensorySchemaStrictAndProviderCompatible() throws Exception {
    for (String resource : SENSORY_SCHEMAS) {
      JsonNode schema = mapper.readTree(Path.of(resource).toFile());
      assertStrictObjects(schema, resource);
      String serialized = schema.toString();
      assertThat(serialized)
          .as("Schema sensorial deve expor o contrato: %s", resource)
          .contains("sensoryExperience", "pleasureByModality", "evidenceBoundary")
          .doesNotContain("\"oneOf\"", "\"anyOf\"", "\"allOf\"", "\"uniqueItems\"");
    }
  }

  /** Confirma fechamento e obrigatoriedade de todos os campos em cada objeto do schema. */
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
}
