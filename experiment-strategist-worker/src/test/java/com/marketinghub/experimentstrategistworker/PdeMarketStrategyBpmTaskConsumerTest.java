package com.marketinghub.experimentstrategistworker;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

  /** Aceita a criação de identidade catalogada para um produto novo do ciclo autônomo. */
  @Test
  void acceptsProductIdentityForNewDiscoveryProduct() throws Exception {
    ObjectNode result = identityResult("CREATE", "Alcyone", "AI_PRODUCT", "Safira");

    assertThatCode(
            () ->
                PdeMarketStrategyBpmTaskConsumer.validate(
                    result, "product-discovery-cycle:71", true, discoveryTask()))
        .doesNotThrowAnyException();
  }

  /** Preserva a política quando uma evidência do dossiê também contém a palavra Contexto. */
  @Test
  void readsIdentityPolicyFromTheFirstContextMarker() throws Exception {
    ObjectNode result = identityResult("CREATE", "Alcyone", "AI_PRODUCT", "Safira");
    ObjectNode task = (ObjectNode) discoveryTask();
    String description = task.path("description").asText();
    task.put(
        "description",
        description.substring(0, description.length() - 1)
            + ",\"evidence\":\"Contexto: trecho factual preservado\"}");

    assertThatCode(
            () ->
                PdeMarketStrategyBpmTaskConsumer.validate(
                    result, "product-discovery-cycle:71", true, task))
        .doesNotThrowAnyException();
  }

  /** Rejeita o rótulo provisório que causou a recorrência no cadastro automático. */
  @Test
  void rejectsProvisionalInternalProductName() throws Exception {
    ObjectNode result =
        identityResult("CREATE", "Decisão de look · PDE planejado #46", "AI_PRODUCT", "Safira");

    assertThatThrownBy(
            () ->
                PdeMarketStrategyBpmTaskConsumer.validate(
                    result, "product-discovery-cycle:71", true, discoveryTask()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("nome interno estável");
  }

  /** Rejeita codinome já usado antes de liberar Plutus e Dédalo. */
  @Test
  void rejectsReservedProductInternalName() throws Exception {
    ObjectNode result = identityResult("CREATE", "Míra", "AI_PRODUCT", "Safira");

    assertThatThrownBy(
            () ->
                PdeMarketStrategyBpmTaskConsumer.validate(
                    result, "product-discovery-cycle:71", true, discoveryTask()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("já ocupado");
  }

  /** Rejeita código e mineral que não formem a mesma entrada ativa do catálogo. */
  @Test
  void rejectsProductTypeOutsideActiveCatalog() throws Exception {
    ObjectNode result = identityResult("CREATE", "Alcyone", "PDE", "Safira");

    assertThatThrownBy(
            () ->
                PdeMarketStrategyBpmTaskConsumer.validate(
                    result, "product-discovery-cycle:71", true, discoveryTask()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fora do catálogo ativo");
  }

  /** Impede Atena de renomear ou reclassificar um produto já existente em um sucessor. */
  @Test
  void preservesExistingProductIdentity() throws Exception {
    ObjectNode result = identityResult("PRESERVE", "Mira", "AI_PRODUCT", "Safira");
    JsonNode task =
        objectMapper.readTree(
            """
            {
              "taskTarget":{
                "productInternalName":"Mira",
                "pdeContext":{"product":{
                  "productTypeCode":"AI_PRODUCT",
                  "productTypeInternalName":"Safira"
                }}
              }
            }
            """);

    assertThatCode(
            () -> PdeMarketStrategyBpmTaskConsumer.validate(result, "experiment:93", true, task))
        .doesNotThrowAnyException();

    result.withObject("/productIdentity").put("internalName", "Alcyone");
    assertThatThrownBy(
            () -> PdeMarketStrategyBpmTaskConsumer.validate(result, "experiment:93", true, task))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("alterar a identidade");
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

  /** Confirma que o schema v9 exige identidade fechada sem relaxar o contrato anterior. */
  @Test
  void keepsProductIdentitySchemaStrictAndComplete() throws Exception {
    String rawSchema =
        Files.readString(
            Path.of(
                "src/main/resources/prompts/pde-commercial-plan/v9/market-strategy-schema.json"));
    JsonNode schema = objectMapper.readTree(rawSchema);

    assertStrictObjects(schema);
    org.assertj.core.api.Assertions.assertThat(schema.path("required"))
        .anyMatch(item -> "productIdentity".equals(item.asText()));
    org.assertj.core.api.Assertions.assertThat(
            schema.path("properties").path("productIdentity").path("required"))
        .hasSize(6);
  }

  /** Exige no Processo 2 v10 o plano multiagente sem participante ou prova comercial fabricada. */
  @Test
  void acceptsAgentValidationContractWithoutHumanPilot() throws Exception {
    ObjectNode result = agentValidationResult();

    assertThatCode(
            () ->
                PdeMarketStrategyBpmTaskConsumer.validate(
                    result, "product-discovery-cycle:71", true, agentDiscoveryTask()))
        .doesNotThrowAnyException();

    result
        .withObject("/marketStrategicContract/agentValidationPlan")
        .put("humanEvidenceClaimed", true);
    assertThatThrownBy(
            () ->
                PdeMarketStrategyBpmTaskConsumer.validate(
                    result, "product-discovery-cycle:71", true, agentDiscoveryTask()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("homologação multiagente");
  }

  /** Impede transformar três pareceres de experiência em três repetições da matriz técnica. */
  @Test
  void rejectsInflatedMatrixRunsAndMissingReviewContract() throws Exception {
    for (int count : new int[] {0, 3}) {
      ObjectNode result = agentValidationResult();
      result
          .withObject("/marketStrategicContract/agentValidationPlan")
          .put("technicalMatrixRuns", count);
      assertThatThrownBy(
              () ->
                  PdeMarketStrategyBpmTaskConsumer.validate(
                      result, "product-discovery-cycle:71", true, agentDiscoveryTask()))
          .hasMessageContaining("homologação multiagente");
    }
    ObjectNode missing = agentValidationResult();
    missing
        .withObject("/marketStrategicContract/agentValidationPlan")
        .remove("independentExperienceReviewCount");
    assertThatThrownBy(
            () ->
                PdeMarketStrategyBpmTaskConsumer.validate(
                    missing, "product-discovery-cycle:71", true, agentDiscoveryTask()))
        .hasMessageContaining("homologação multiagente");
  }

  /** Mantém o schema e o prompt vigente fechados contra o retorno do piloto humano legado. */
  @Test
  void keepsAgentValidationResourcesStrictAndHumanIndependent() throws Exception {
    String rawSchema =
        Files.readString(
            Path.of(
                "src/main/resources/prompts/pde-commercial-plan/v10/market-strategy-schema.json"));
    String prompt =
        Files.readString(
            Path.of("src/main/resources/prompts/pde-commercial-plan/v11/market-strategy.md"));
    JsonNode schema = objectMapper.readTree(rawSchema);

    assertStrictObjects(schema);
    org.assertj.core.api.Assertions.assertThat(rawSchema)
        .contains("MARKET_STRATEGY_V4", "READY_FOR_AGENT_VALIDATION", "agentValidationPlan")
        .doesNotContain(
            "privateValidationPlan",
            "minimumIndependentReadings",
            "minimumEligibleParticipantsPerReading",
            "\"uniqueItems\"");
    org.assertj.core.api.Assertions.assertThat(prompt)
        .contains("Nunca proponha entrevista", "somente o mercado comprova demanda")
        .doesNotContain("duas leituras", "READY_FOR_PRIVATE_VALIDATION");
  }

  /** Reproduz a deriva de Vega e aceita somente a entrada descrita pela implementação recebida. */
  @Test
  void preservesImplementedInputBeforeHandingOffToDedalo() throws Exception {
    ObjectNode result = agentValidationResult();
    ObjectNode task =
        implementedInputTask(
            91004L, 91013L, 91106L, "Ocasião e roupa disponível; observação opcional.");
    var delivery =
        result.withObject("/marketStrategicContract/agentValidationPlan/customerValueDelivery");
    delivery.put(
        "minimumCustomerInput",
        "Quatro escolhas categoriais: situação, mensagem percebida, sinal desejado e recurso existente.");
    assertThatThrownBy(
            () ->
                PdeMarketStrategyBpmTaskConsumer.validate(result, "experiment:91106", false, task))
        .hasMessageContaining("entrada implementada");
    delivery.put("minimumCustomerInput", "Ocasião e roupa disponível; observação opcional.");
    assertThatCode(
            () ->
                PdeMarketStrategyBpmTaskConsumer.validate(result, "experiment:91106", false, task))
        .doesNotThrowAnyException();
  }

  /** Confere outro produto com entrada diferente e recusa trocar a identidade do contrato. */
  @Test
  void preservesOtherProductInputAndRejectsCrossedIdentity() throws Exception {
    ObjectNode result = agentValidationResult();
    ObjectNode task =
        implementedInputTask(
            92010L, 92009L, 92102L, "Objetivo e produtos com instruções do rótulo.");
    result
        .withObject("/marketStrategicContract/agentValidationPlan/customerValueDelivery")
        .put("minimumCustomerInput", "Objetivo e produtos com instruções do rótulo.");
    assertThatCode(
            () ->
                PdeMarketStrategyBpmTaskConsumer.validate(result, "experiment:92102", false, task))
        .doesNotThrowAnyException();
    ObjectNode context =
        (ObjectNode) objectMapper.readTree(task.path("processContextJson").asText());
    context.withObject("/learningSalesCycle/implementedInput").put("productId", 91004L);
    task.put("processContextJson", context.toString());
    assertThatThrownBy(
            () ->
                PdeMarketStrategyBpmTaskConsumer.validate(result, "experiment:92102", false, task))
        .hasMessageContaining("não corresponde");
  }

  /**
   * Monta contrato de transporte sintético com produto, ciclo, experimento e versão inequívocos.
   */
  private ObjectNode implementedInputTask(
      Long productId, Long cycleId, Long experimentId, String summary) throws Exception {
    ObjectNode task = (ObjectNode) agentDiscoveryTask();
    task.put("sourceReference", "experiment:" + experimentId);
    task.putObject("taskTarget").put("productId", productId);
    ObjectNode context = objectMapper.createObjectNode();
    var cycle = context.putObject("learningSalesCycle");
    cycle
        .put("productId", productId)
        .put("cycleId", cycleId)
        .put("experimentId", experimentId)
        .put("productVersion", "fixture-current-v1");
    cycle
        .putObject("implementedInput")
        .put("contractVersion", "PDE_IMPLEMENTED_INPUT_V1")
        .put("productId", productId)
        .put("cycleId", cycleId)
        .put("experimentId", experimentId)
        .put("prototypeVersion", "fixture-current-v1")
        .put("sourceReference", "Fixture do contrato executável local")
        .put("minimumCustomerInput", summary)
        .putArray("requiredFields")
        .add("fixtureInput");
    task.put("processContextJson", context.toString());
    return task;
  }

  /** Compartilha uma estratégia v4 completa com os testes de contrato e entrega multiagente. */
  ObjectNode agentValidationResult() throws Exception {
    ObjectNode result = identityResult("CREATE", "Alcyone", "AI_PRODUCT", "Safira");
    ObjectNode contract = (ObjectNode) result.path("marketStrategicContract");
    contract.put("contractVersion", "MARKET_STRATEGY_V4");
    contract.put("status", "READY_FOR_AGENT_VALIDATION");
    contract.remove("privateValidationPlan");
    ObjectNode plan = contract.putObject("agentValidationPlan");
    plan.put("contractVersion", "PDE_AGENT_VALIDATION_V1");
    plan.put("technicalMatrixRuns", 1);
    plan.put("independentExperienceReviewCount", 3);
    plan.put("hypothesis", "Três combinações prontas reduzem tentativa e erro.");
    plan.put("prototypeObjective", "Entregar três combinações em até dez minutos.");
    plan.set(
        "purchaseScene",
        objectMapper.readTree(
            """
            {"trigger":"Ocasião confirmada","deadline":"Antes do evento",
             "costOfError":"Perder tempo e confiança","budgetEvidence":"Compara orientação paga",
             "failedAttempt":"Tentou montar sozinha","currentPaidBehavior":"Compra orientação"}
            """));
    plan.put("strongestFreeAlternative", "Montagem manual com IA genérica.");
    plan.put("prototypeAdvantage", "Resultado pessoal pronto sem prompting.");
    plan.set(
        "customerValueDelivery",
        objectMapper.readTree(
            """
            {"territories":["RECOGNITION","EFFORT_RELIEF"],
             "desiredTransformation":"Escolher com segurança e menos esforço",
             "evidenceSourceIds":["source-1","source-2"],
             "evidencePathways":["CURRENT_LANGUAGE","PAID_BEHAVIOR"],
             "readyMadeOutcome":"Três combinações prontas","minimumCustomerInput":"Ocasião e preferências",
             "requiresPromptEngineering":false,"requiresManualAssembly":false,
             "usableWithoutAiKnowledge":true,"customerStepsToValue":3,
             "timeToUsableResultMinutes":10,"automationBoundary":"Cliente decide o uso final"}
            """));
    plan.put("trafficClass", "AGENT_VALIDATION");
    plan.put("internalMarker", "mh_internal_test");
    plan.putArray("requiredScenarios").add("ADHERENT").add("RECOVERY").add("SAFETY");
    plan.putArray("requiredDevices").add("DESKTOP_1440").add("IPHONE_15_PRO").add("PIXEL_7");
    plan.put("maxReadyResultSeconds", 600);
    plan.put("humanEvidenceClaimed", false);
    plan.put("commercialEvidenceClaimed", false);
    plan.put("paymentEnabled", false);
    plan.put("publicationAuthorized", false);
    plan.put("campaignAuthorized", false);
    plan.put("mediaSpendAuthorizedBrl", 0);
    plan.put("sourceMaxAgeDays", 30);
    plan.put("continueCriteria", "Todos os gates automatizados aprovados.");
    plan.put("adjustCriteria", "Corrigir somente a etapa bloqueada.");
    plan.put("stopCriteria", "Mecanismo inviável ou inseguro.");
    plan.put("sourceRefreshRequired", false);
    plan.put("sourceRefreshAction", "Nenhuma atualização pendente.");
    plan.put("publicationBoundary", "Sem contato, publicação, cobrança, campanha ou gasto.");
    return result;
  }

  /** Acrescenta a versão 10 à mesma política de identidade entregue pelo backend. */
  private JsonNode agentDiscoveryTask() throws Exception {
    ObjectNode task = (ObjectNode) discoveryTask();
    task.put("processVersion", 10);
    return task;
  }

  /** Acrescenta uma identidade estruturada a um parecer válido para os cenários da versão 9. */
  private ObjectNode identityResult(
      String mode, String internalName, String typeCode, String typeInternalName) throws Exception {
    ObjectNode result = (ObjectNode) objectMapper.readTree(validResult("APPROVE", "301", "501"));
    ObjectNode identity = result.putObject("productIdentity");
    identity.put("contractVersion", "PRODUCT_IDENTITY_V1");
    identity.put("mode", mode);
    identity.put("internalName", internalName);
    identity.put("productTypeCode", typeCode);
    identity.put("productTypeInternalName", typeInternalName);
    identity.put(
        "classificationRationale",
        "A personalização por IA constitui o mecanismo de valor; a web é apenas formato.");
    return result;
  }

  /** Monta a política de nomes e tipos anexada pelo backend à tarefa autônoma. */
  private JsonNode discoveryTask() throws Exception {
    return objectMapper.readTree(
        """
        {
          "description":"Escolha uma identidade. Contexto: {\\\"productIdentityPolicy\\\":{\\\"contractVersion\\\":\\\"PRODUCT_IDENTITY_V1\\\",\\\"internalNameUniverse\\\":\\\"STAR\\\",\\\"reservedInternalNames\\\":[\\\"Mira\\\"],\\\"activeProductTypes\\\":[{\\\"code\\\":\\\"AI_PRODUCT\\\",\\\"internalName\\\":\\\"Safira\\\"}]}}"
        }
        """);
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
  static String validResult(String decision, String dossierId, String opportunityId) {
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
