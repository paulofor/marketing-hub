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

  /** Rejeita aprovação que tenta pular da pesquisa diretamente para operação comercial. */
  @Test
  void rejectsApprovalAsReadyForOperation() throws Exception {
    var result = objectMapper.readTree(validResult("APPROVE", "301", "501")).deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) result.path("marketStrategicContract"))
        .put("status", "READY_FOR_OPERATION");

    assertThatThrownBy(() -> PdeMarketStrategyBpmTaskConsumer.validate(result))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("contrato versionado");
  }

  /** Rejeita aprovação sem os cinco sinais e as duas leituras predeclaradas. */
  @Test
  void rejectsApprovalWithoutCompletePrivateValidationPlan() throws Exception {
    var result = objectMapper.readTree(validResult("APPROVE", "301", "501"));
    ((com.fasterxml.jackson.databind.node.ObjectNode)
            result.path("marketStrategicContract").path("privateValidationPlan"))
        .put("minimumIndependentReadings", 1);

    assertThatThrownBy(() -> PdeMarketStrategyBpmTaskConsumer.validate(result))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("duas leituras privadas");
  }

  /** Rejeita sinal repetido que tentaria ocultar a ausência de um critério privado. */
  @Test
  void rejectsApprovalWithDuplicatedPrivateSignal() throws Exception {
    var result = objectMapper.readTree(validResult("APPROVE", "301", "501"));
    var signals =
        (com.fasterxml.jackson.databind.node.ArrayNode)
            result
                .path("marketStrategicContract")
                .path("privateValidationPlan")
                .path("requiredSignals");
    signals.set(4, com.fasterxml.jackson.databind.node.TextNode.valueOf("EXPERIENCE_STARTED"));

    assertThatThrownBy(() -> PdeMarketStrategyBpmTaskConsumer.validate(result))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("duas leituras privadas");
  }

  /** Confirma que o schema v7 permanece estrito em todos os objetos aninhados. */
  @Test
  void keepsVersionedSchemaStrictAndComplete() throws Exception {
    String rawSchema =
        Files.readString(
            Path.of(
                "src/main/resources/prompts/pde-commercial-plan/v7/market-strategy-schema.json"));
    JsonNode schema = objectMapper.readTree(rawSchema);

    assertStrictObjects(schema);
    org.assertj.core.api.Assertions.assertThat(rawSchema)
        .as("o schema enviado ao Codex não pode usar palavras rejeitadas pelo Structured Outputs")
        .doesNotContain("\"uniqueItems\"", "\"anyOf\"", "\"oneOf\"", "\"allOf\"");
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
    String status =
        "APPROVE".equals(decision) ? "READY_FOR_PRIVATE_VALIDATION" : "INSUFFICIENT_EVIDENCE";
    return """
        {
          "decision":"%s",
          "selectedDossierId":%s,
          "selectedOpportunityId":%s,
          "alternatives":[{},{},{}],
          "selectedAlternative":"Alternativa factual",
          "marketStrategicContract":{
            "contractVersion":"MARKET_STRATEGY_V3",
            "status":"%s",
            "privateValidationPlan":{
              "prototypeObjective":"Demonstrar resultado pronto em até dez minutos.",
              "purchaseScene":{
                "trigger":"Compromisso confirmado.",
                "deadline":"Antes de sair hoje.",
                "costOfError":"Perder confiança e tempo.",
                "budgetEvidence":"Já compara alternativas pagas.",
                "failedAttempt":"Tentou montar manualmente.",
                "currentPaidBehavior":"Compra orientação especializada."
              },
              "strongestFreeAlternative":"Pesquisar e montar manualmente com uma IA genérica.",
              "prototypeAdvantage":"Entregar resultado pessoal pronto sem prompting.",
              "humanValueDelivery":{
                "territories":["RECOGNITION","EFFORT_RELIEF"],
                "desiredTransformation":"Sentir segurança com menos esforço.",
                "evidenceSourceIds":["source-1","source-2"],
                "evidencePathways":["CURRENT_LANGUAGE","PAID_BEHAVIOR"],
                "readyMadeOutcome":"Resultado pessoal pronto para uso.",
                "minimumCustomerInput":"Uma escolha curta em linguagem comum.",
                "requiresPromptEngineering":false,
                "requiresManualAssembly":false,
                "usableWithoutAiKnowledge":true,
                "customerStepsToValue":3,
                "timeToUsableResultMinutes":8,
                "automationBoundary":"A pessoa revisa antes de aplicar."
              },
              "minimumIndependentReadings":2,
              "minimumEligibleParticipantsPerReading":1,
              "requiredSignals":[
                "EXPERIENCE_STARTED","VALUE_MOMENT","READY_RESULT_USED",
                "PREFERRED_OVER_FREE","CHECKOUT_STARTED"
              ],
              "minimumExperienceStartRate":1,
              "minimumValueMomentRate":1,
              "minimumReadyResultUseRate":1,
              "minimumPrototypePreferenceRate":1,
              "minimumCheckoutStartRate":1,
              "sourceMaxAgeDays":30,
              "sourceRefreshRequired":false,
              "sourceRefreshAction":"Nenhuma atualização necessária.",
              "publicationBoundary":"Uso privado sem contato, publicação, pagamento ou gasto."
            }
          },
          "rationale":"Decisão sustentada pelo dossiê e suas fontes."
        }
        """
        .formatted(decision, dossierId, opportunityId, status);
  }
}
