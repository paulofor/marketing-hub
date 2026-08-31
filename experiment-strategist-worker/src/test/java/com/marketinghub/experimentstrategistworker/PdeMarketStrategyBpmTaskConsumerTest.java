package com.marketinghub.experimentstrategistworker;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a seleção única e factual do contrato autônomo de Atena. */
class PdeMarketStrategyBpmTaskConsumerTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  /** Aceita aprovação que seleciona exatamente um dossiê e preserva contrato versionado. */
  @Test
  void acceptsSingleSelectedDossier() throws Exception {
    assertThatCode(
            () ->
                PdeMarketStrategyBpmTaskConsumer.validate(
                    objectMapper.readTree(validResult("APPROVE", "301", "501"))))
        .doesNotThrowAnyException();
  }

  /** Rejeita aprovação sem vínculo exato à candidata factual de Argos. */
  @Test
  void rejectsApprovalWithoutSelectedDossier() throws Exception {
    var result = objectMapper.readTree(validResult("APPROVE", "null", "null"));

    assertThatThrownBy(() -> PdeMarketStrategyBpmTaskConsumer.validate(result))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("candidata factual");
  }

  /** Permite ajuste sem fabricar seleção enquanto a evidência não sustenta avanço. */
  @Test
  void acceptsAdjustmentWithoutSelection() throws Exception {
    assertThatCode(
            () ->
                PdeMarketStrategyBpmTaskConsumer.validate(
                    objectMapper.readTree(validResult("ADJUST", "null", "null"))))
        .doesNotThrowAnyException();
  }

  /** Preserva tarefas comerciais legadas que não possuem candidata de ciclo autônomo. */
  @Test
  void acceptsApprovalWithoutDiscoverySelectionOutsideAutonomousCycle() throws Exception {
    assertThatCode(
            () ->
                PdeMarketStrategyBpmTaskConsumer.validate(
                    objectMapper.readTree(validResult("APPROVE", "null", "null")), "product:77"))
        .doesNotThrowAnyException();
  }

  /** Confirma que o schema v6 permanece estrito em todos os objetos aninhados. */
  @Test
  void keepsVersionedSchemaStrictAndComplete() throws Exception {
    JsonNode schema =
        objectMapper.readTree(
            Files.readString(
                Path.of(
                    "src/main/resources/prompts/pde-commercial-plan/v6/market-strategy-schema.json")));

    assertStrictObjects(schema);
  }

  /** Percorre objetos do schema e exige propriedades fechadas e integralmente obrigatórias. */
  private void assertStrictObjects(JsonNode node) {
    if (node.isObject()) {
      if ("object".equals(node.path("type").asText())) {
        org.assertj.core.api.Assertions.assertThat(
                node.path("additionalProperties").asBoolean(true))
            .as("todo objeto do schema deve rejeitar propriedades desconhecidas")
            .isFalse();
        Set<String> properties = new HashSet<>();
        node.path("properties").fieldNames().forEachRemaining(properties::add);
        Set<String> required = new HashSet<>();
        node.path("required").forEach(item -> required.add(item.asText()));
        org.assertj.core.api.Assertions.assertThat(required)
            .as("todas as propriedades do objeto devem ser obrigatórias")
            .containsExactlyInAnyOrderElementsOf(properties);
      }
      node.elements().forEachRemaining(this::assertStrictObjects);
    } else if (node.isArray()) {
      node.elements().forEachRemaining(this::assertStrictObjects);
    }
  }

  /** Monta um parecer mínimo que respeita o schema versionado da atividade. */
  private String validResult(String decision, String dossierId, String opportunityId) {
    return """
        {
          "decision":"%s",
          "selectedDossierId":%s,
          "selectedOpportunityId":%s,
          "alternatives":[{},{},{}],
          "selectedAlternative":"Alternativa factual",
          "marketStrategicContract":{"contractVersion":"MARKET_STRATEGY_V2"},
          "rationale":"Decisão sustentada pelo dossiê e suas fontes."
        }
        """
        .formatted(decision, dossierId, opportunityId);
  }
}
