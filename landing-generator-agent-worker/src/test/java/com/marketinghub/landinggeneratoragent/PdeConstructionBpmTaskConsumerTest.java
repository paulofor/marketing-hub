package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: proteger os contratos BPM de construção do PDE executados por Dédalo. */
class PdeConstructionBpmTaskConsumerTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Persiste a auditoria integral antes de iniciar a chamada faturável do modelo. */
  @Test
  void recordsExecutionAuditBeforeModelInvocation() throws Exception {
    AtomicReference<String> method = new AtomicReference<>();
    AtomicReference<String> body = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/api/internal/agent-tasks/landing-generator/stage-executions/534/execution-audit",
        exchange -> {
          method.set(exchange.getRequestMethod());
          body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          exchange.sendResponseHeaders(204, -1);
          exchange.close();
        });
    server.start();
    try {
      LandingGeneratorAgentProperties properties = new LandingGeneratorAgentProperties();
      properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
      PdeConstructionBpmTaskConsumer consumer =
          new PdeConstructionBpmTaskConsumer(
              properties,
              json,
              mock(AutomaticExecutionControl.class),
              mock(CodexTelemetryReporter.class));

      consumer.recordExecutionStart(
          Map.of("taskId", 534L),
          "Núcleo de Dédalo.\n\nConstruir a jornada.",
          "Núcleo de Dédalo.",
          "Construir a jornada.");

      JsonNode audit = json.readTree(body.get());
      assertThat(method.get()).isEqualTo("PUT");
      assertThat(audit.path("executionMode").asText()).isEqualTo("MODEL");
      assertThat(audit.path("reasoningEffort").asText()).isEqualTo("max");
      assertThat(audit.path("promptSent").asText())
          .isEqualTo("Núcleo de Dédalo.\n\nConstruir a jornada.");
      assertThat(audit.path("agentPromptPart").asText()).isEqualTo("Núcleo de Dédalo.");
      assertThat(audit.path("activityPromptPart").asText()).isEqualTo("Construir a jornada.");
    } finally {
      server.stop(0);
    }
  }

  /** Reconhece o parecer preservado que deve ser reenviado antes de qualquer nova inferência. */
  @Test
  void restoresCompleteCallbackWithoutModelInput() throws Exception {
    String result = validProductArchitectureResult();
    var callback =
        PdeConstructionBpmTaskConsumer.preservedCallback(
            json,
            Map.of("retryResultJson", result, "retryEvidenceJson", "{\"proof\":true}"),
            new PdeConstructionBpmTaskConsumer.BpmContract(
                "pde-commercial-plan-offer",
                "productArchitecture",
                "prompt",
                "schema",
                "v6",
                "APPROVE"));

    assertThat(callback).isNotNull();
    assertThat(callback.result().path("decision").asText()).isEqualTo("APPROVE");
    assertThat(callback.resultJson()).isEqualTo(result);
    assertThat(callback.evidenceJson()).isEqualTo("{\"proof\":true}");
  }

  /** Falha antes do modelo quando o backend entrega somente parte do callback preservado. */
  @Test
  void rejectsPartialPreservedCallbackBeforeModel() {
    var contract =
        new PdeConstructionBpmTaskConsumer.BpmContract(
            "pde-commercial-plan-offer",
            "productArchitecture",
            "prompt",
            "schema",
            "v6",
            "APPROVE");

    assertThatThrownBy(
            () ->
                PdeConstructionBpmTaskConsumer.preservedCallback(
                    json, Map.of("retryResultJson", validProductArchitectureResult()), contract))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("nova inferência");
  }

  /** Preserva resultado, prova, auditoria e consumo quando apenas o callback inicial falha. */
  @Test
  void preservesCompletedInferenceWhenResultCallbackFails() throws Exception {
    PdeConstructionBpmTaskConsumer consumer = consumer();
    Map<String, Object> task = productArchitectureTask();
    JsonNode result = json.readTree(validProductArchitectureResult());
    var execution =
        new PdeConstructionBpmTaskConsumer.BpmExecution(
            result,
            new PdeConstructionBpmTaskConsumer.TokenUsage(120L, 20L, 40L),
            "prompt completo",
            "núcleo",
            "atividade");

    Map<String, Object> failure =
        consumer.failureBody(
            task, new IllegalStateException("500 Internal Server Error"), execution);

    assertThat(failure.get("resultJson")).isEqualTo(json.writeValueAsString(result));
    assertThat(failure.get("evidenceJson")).asString().contains("product-discovery-cycle:71");
    assertThat(failure).containsKeys("modelUsages", "executionAudit");
  }

  /** Torna a segunda falha do mesmo callback terminal sem somar novo uso de modelo. */
  @Test
  void blocksSecondCallbackFailureWithoutRepeatedUsage() throws Exception {
    PdeConstructionBpmTaskConsumer consumer = consumer();
    Map<String, Object> task = new HashMap<>(productArchitectureTask());
    task.put("retryResultJson", validProductArchitectureResult());
    task.put("retryEvidenceJson", "{\"proof\":true}");

    Map<String, Object> failure =
        consumer.failureBody(task, new IllegalStateException("500 Internal Server Error"), null);

    assertThat(failure.get("error")).asString().startsWith("AUTO_RETRY_CALLBACK_ONCE|");
    assertThat(failure)
        .containsEntry("resultJson", validProductArchitectureResult())
        .containsEntry("evidenceJson", "{\"proof\":true}")
        .doesNotContainKeys("modelUsages", "executionAudit");
  }

  /** Resolve prompts e schemas específicos sem misturar contratos entre atividades. */
  @Test
  void resolvesVersionedResourcesByActivity() {
    assertThat(
            PdeConstructionBpmTaskConsumer.promptResourceFor(
                "pde-commercial-plan-offer", "productArchitecture"))
        .isEqualTo("prompts/pde-commercial-plan/v6/product-architecture.md");
    assertThat(
            PdeConstructionBpmTaskConsumer.promptResourceFor(
                "pde-construction-approval", "journey"))
        .isEqualTo("prompts/pde-construction/v2/journey.md");
    assertThat(
            PdeConstructionBpmTaskConsumer.schemaResourceFor("pde-construction-approval", "access"))
        .isEqualTo("prompts/pde-construction/v2/access-schema.json");
    assertThat(
            PdeConstructionBpmTaskConsumer.promptResourceFor(
                "pde-construction-approval", "prototypeCorrection"))
        .isEqualTo("prompts/pde-construction/v3/prototype-correction.md");
    assertThat(
            PdeConstructionBpmTaskConsumer.schemaResourceFor(
                "pde-construction-approval", "prototypeCorrection"))
        .isEqualTo("prompts/pde-construction/v3/prototype-correction-schema.json");
    assertThat(
            PdeConstructionBpmTaskConsumer.promptResourceFor(
                "venda-entrega-satisfacao-cliente", "materialization"))
        .isEqualTo("prompts/pde-delivery/v1/personalization.md");
    assertThat(
            PdeConstructionBpmTaskConsumer.promptResourceFor(
                "pde-tasting-proof-of-value", "materialization"))
        .isEqualTo("prompts/pde-tasting/v1/materialization.md");
  }

  /** Cobre todas as atividades vigentes de Dédalo e recusa comunicação pertencente a Íris. */
  @Test
  void supportsOnlyCurrentProductContracts() {
    assertThat(
            PdeConstructionBpmTaskConsumer.supportsContract(
                "pde-commercial-plan-offer", "productArchitecture"))
        .isTrue();
    assertThat(
            PdeConstructionBpmTaskConsumer.supportsContract("pde-construction-approval", "journey"))
        .isTrue();
    assertThat(
            PdeConstructionBpmTaskConsumer.supportsContract(
                "pde-construction-approval", "deliverables"))
        .isTrue();
    assertThat(
            PdeConstructionBpmTaskConsumer.supportsContract("pde-construction-approval", "access"))
        .isTrue();
    assertThat(
            PdeConstructionBpmTaskConsumer.supportsContract(
                "pde-construction-approval", "prototypeCorrection"))
        .isTrue();
    assertThat(
            PdeConstructionBpmTaskConsumer.supportsContract(
                "pde-tasting-proof-of-value", "materialization"))
        .isTrue();
    assertThat(
            PdeConstructionBpmTaskConsumer.supportsContract(
                "venda-entrega-satisfacao-cliente", "materialization"))
        .isTrue();
    assertThat(
            PdeConstructionBpmTaskConsumer.supportsContract(
                "creative-production-approval", "nonAudiovisual"))
        .isFalse();
    assertThat(PdeConstructionBpmTaskConsumer.supportsContract("landing-page-generation", "html"))
        .isFalse();
  }

  /** Prioriza a personalização de venda reconciliada antes de construção e degustação. */
  @Test
  void prioritizesPaidDeliveryInPollingOrder() {
    assertThat(PdeConstructionBpmTaskConsumer.contractKeysInPollingOrder())
        .startsWith("venda-entrega-satisfacao-cliente/materialization")
        .containsExactlyInAnyOrder(
            "opala-commercial-preparation-v1/entry",
            "opala-commercial-preparation-v1/creative",
            "opala-commercial-preparation-v1/checkout",
            "opala-commercial-preparation-v1/targeting",
            "venda-entrega-satisfacao-cliente/materialization",
            "pde-construction-approval/prototypeCorrection",
            "pde-construction-approval/journey",
            "pde-construction-approval/deliverables",
            "pde-construction-approval/access",
            "pde-commercial-plan-offer/productArchitecture",
            "pde-tasting-proof-of-value/materialization");
  }

  /** Garante que todos os schemas executáveis existem e são estritos em cada objeto. */
  @Test
  void keepsEveryCurrentProductSchemaStrict() throws Exception {
    List<String> schemas =
        List.of(
            PdeConstructionBpmTaskConsumer.schemaResourceFor(
                "pde-commercial-plan-offer", "productArchitecture"),
            PdeConstructionBpmTaskConsumer.schemaResourceFor(
                "pde-construction-approval", "prototypeCorrection"),
            PdeConstructionBpmTaskConsumer.schemaResourceFor(
                "pde-construction-approval", "journey"),
            PdeConstructionBpmTaskConsumer.schemaResourceFor(
                "pde-construction-approval", "deliverables"),
            PdeConstructionBpmTaskConsumer.schemaResourceFor("pde-construction-approval", "access"),
            PdeConstructionBpmTaskConsumer.schemaResourceFor(
                "pde-tasting-proof-of-value", "materialization"),
            PdeConstructionBpmTaskConsumer.schemaResourceFor(
                "venda-entrega-satisfacao-cliente", "materialization"));

    for (String schema : schemas) {
      String rawSchema =
          new ClassPathResource(schema).getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
      JsonNode root = json.readTree(rawSchema);
      assertStrictObjects(root, schema);
      assertThat(rawSchema)
          .as("schema %s deve ser aceito pelo Structured Outputs", schema)
          .doesNotContain("\"uniqueItems\"", "\"anyOf\"", "\"oneOf\"", "\"allOf\"");
    }
  }

  /** Impede que o prompt genérico recupere produto fixo ou antecipe comunicação comercial. */
  @Test
  void keepsJourneyGenericAndRestrictedToPrivatePrototype() throws Exception {
    String prompt = read("prompts/pde-construction/v2/journey.md");

    assertThat(prompt)
        .contains(
            "experiência digital",
            "SIMULATED_NO_CHARGE",
            "TASK_CONTEXT.taskTarget.pdeContext",
            "researchIntelligence",
            "Não proponha recrutamento",
            "ADHERENT",
            "RECOVERY",
            "SAFETY")
        .doesNotContain("Kit WhatsApp", "15 respostas", "pós-compra");
  }

  /** Aceita o contexto privado completo e segregado antes de chamar o modelo. */
  @Test
  void acceptsCompletePrivatePdeContext() throws Exception {
    Map<String, Object> task =
        json.readValue(
            """
            {"taskTarget":{"experienceVersion":"private-validation-v1","pdeContext":{
              "contractVersion":"PDE_HARNESS_PLAN_V1",
              "experienceVersion":"private-validation-v1",
              "marketStrategy":{"buyer":"Pessoa compradora definida",
                "problem":"Dor recorrente definida","desiredOutcome":"Resultado pronto",
                "valueMechanism":"Transformação orientada por IA"},
              "economics":{"commercialSpendAuthorized":false},
              "harness":{"privatePrototype":{"simpleInput":"Entrada simples",
                "readyResult":"Resultado pessoal pronto","maxValueTimeMinutes":10,
                "instrumentationEvents":["EXPERIENCE_STARTED","VALUE_MOMENT",
                  "READY_RESULT_USED","PREFERRED_OVER_FREE","CHECKOUT_STARTED"],
                "checkoutMode":"SIMULATED_NO_CHARGE"}},
              "privateValidationPlan":{"purchaseScene":{"trigger":"Momento de compra"},
                "humanValueDelivery":{"minimumCustomerInput":"Contexto mínimo",
                  "readyMadeOutcome":"Entrega utilizável"}},
              "publicationBoundary":"Construção privada sem publicação ou gasto"
            }}}
            """,
            Map.class);

    PdeConstructionBpmTaskConsumer.validateTaskContext(
        task,
        new PdeConstructionBpmTaskConsumer.BpmContract(
            "pde-construction-approval", "journey", "prompt", "schema", "v2", "READY"),
        json);
  }

  /** Aceita o contrato multiagente sem plano de leitura ou participante humano. */
  @Test
  void acceptsCompleteAgentValidationContext() throws Exception {
    Map<String, Object> task =
        json.readValue(
            """
            {"taskTarget":{"experienceVersion":"agent-validation-v1","pdeContext":{
              "contractVersion":"PDE_HARNESS_PLAN_V1",
              "experienceVersion":"agent-validation-v1",
              "marketStrategy":{"buyer":"Pessoa compradora definida",
                "problem":"Dor recorrente definida","desiredOutcome":"Resultado pronto",
                "valueMechanism":"Transformação orientada por IA"},
              "economics":{"commercialSpendAuthorized":false},
              "harness":{"privatePrototype":{"simpleInput":"Entrada simples",
                "readyResult":"Resultado pessoal pronto","maxValueTimeMinutes":10,
                "instrumentationEvents":["EXPERIENCE_STARTED","VALUE_MOMENT",
                  "READY_RESULT_USED","PREFERRED_OVER_FREE","CHECKOUT_STARTED"],
                "checkoutMode":"SIMULATED_NO_CHARGE"}},
              "agentValidationPlan":{"contractVersion":"PDE_AGENT_VALIDATION_V1",
                "purchaseScene":{"trigger":"Ocasião concreta"},
                "customerValueDelivery":{"minimumCustomerInput":"Contexto mínimo",
                  "readyMadeOutcome":"Entrega utilizável"}},
              "publicationBoundary":"Homologação multiagente sem publicação ou gasto"
            }}}
            """,
            Map.class);

    PdeConstructionBpmTaskConsumer.validateTaskContext(
        task,
        new PdeConstructionBpmTaskConsumer.BpmContract(
            "pde-construction-approval", "journey", "prompt", "schema", "v2", "READY"),
        json);
  }

  /** Recusa o handoff incompleto antes que ele consuma tokens do modelo. */
  @Test
  void rejectsMissingPrivatePdeContextBeforeModel() {
    Map<String, Object> task =
        Map.of("taskTarget", Map.of("experienceVersion", "private-validation-v1"));

    assertThatThrownBy(
            () ->
                PdeConstructionBpmTaskConsumer.validateTaskContext(
                    task,
                    new PdeConstructionBpmTaskConsumer.BpmContract(
                        "pde-construction-approval", "journey", "prompt", "schema", "v2", "READY"),
                    json))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Contrato PDE de homologação");
  }

  /** Aceita a candidata Opala quando alvo, ciclo, checkout e acesso apontam à mesma versão. */
  @Test
  void acceptsExactOpalaCandidateBeforeModel() throws Exception {
    Map<String, Object> task = opalaTask(407L, 922L, "pde-v18", "checkout-futuro");

    PdeConstructionBpmTaskConsumer.validateTaskContext(
        task,
        new PdeConstructionBpmTaskConsumer.BpmContract(
            "opala-commercial-preparation-v1", "checkout", "prompt", "schema", "v1", "READY"),
        json);
  }

  /** Recusa a predecessora mesmo quando ela reutiliza o mesmo checkout da candidata. */
  @Test
  void rejectsPredecessorOpalaTargetBeforeModel() throws Exception {
    Map<String, Object> task = opalaTask(4L, 92L, "musa-v12", "owm6x");
    @SuppressWarnings("unchecked")
    Map<String, Object> target = (Map<String, Object>) task.get("taskTarget");
    target.put("experienceVersion", "musa-v7");

    assertThatThrownBy(
            () ->
                PdeConstructionBpmTaskConsumer.validateTaskContext(
                    task,
                    new PdeConstructionBpmTaskConsumer.BpmContract(
                        "opala-commercial-preparation-v1",
                        "checkout",
                        "prompt",
                        "schema",
                        "v1",
                        "READY"),
                    json))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Contrato Opala mistura");
  }

  /** Monta uma tarefa sintética sem efeitos externos para exercitar identidades futuras. */
  @SuppressWarnings("unchecked")
  private Map<String, Object> opalaTask(
      long productId, long experimentId, String version, String checkoutReference)
      throws Exception {
    String checkoutUrl = "https://go.pepper.com.br/" + checkoutReference;
    String contract =
        """
        {"experienceVersion":"%s",
         "commercialBinding":{"experimentId":%d,"priceBrl":67,"billingModel":"ONE_TIME"},
         "commercialCheckout":{"provider":"PEPPER","checkoutUrl":"%s",
           "offerReference":"%s","priceBrl":67,"currency":"BRL","billingModel":"ONE_TIME"},
         "commercialAccess":{"experienceVersion":"%s","accessDays":90,"renewal":false,
           "activationTrigger":"PAYMENT_APPROVED","scope":"PAID_CONTENT"}}
        """
            .formatted(version, experimentId, checkoutUrl, checkoutReference, version);
    String value =
        """
        {"taskTarget":{"experimentId":%d,"productId":%d,"experienceVersion":"%s",
          "publicUrl":"https://candidate.example/%s","commercialCheckoutProvider":"PEPPER",
          "commercialCheckoutReference":"%s","commercialCheckoutUrl":"%s","unitPriceBrl":67,
          "pdeContext":%s},
         "processContextJson":""}
        """
            .formatted(
                experimentId,
                productId,
                version,
                version,
                checkoutReference,
                checkoutUrl,
                contract);
    Map<String, Object> task = json.readValue(value, Map.class);
    Map<String, Object> process =
        json.readValue(
            """
            {"opalaCommercial":{"productId":%d,"experimentId":%d,"cycleId":71,
              "productVersion":"%s","destinationUrl":"https://candidate.example/%s",
              "priceBrl":67,"checkoutUrl":"%s","productContract":%s},
             "learningSalesCycle":{"cycleId":71,"productId":%d,"experimentId":%d,
              "productVersion":"%s"}}
            """
                .formatted(
                    productId,
                    experimentId,
                    version,
                    version,
                    checkoutUrl,
                    contract,
                    productId,
                    experimentId,
                    version),
            Map.class);
    task.put("processContextJson", json.writeValueAsString(process));
    return task;
  }

  /** Mantém as três atividades privadas presas ao contrato e sem prova humana antecipada. */
  @Test
  void keepsEveryPrivateConstructionPromptBoundToCanonicalContext() throws Exception {
    for (String resource :
        List.of(
            "prompts/pde-construction/v2/journey.md",
            "prompts/pde-construction/v2/deliverables.md",
            "prompts/pde-construction/v2/access.md")) {
      assertThat(read(resource))
          .contains("TASK_CONTEXT.taskTarget.pdeContext", "researchIntelligence")
          .containsIgnoringCase("recrutamento")
          .containsIgnoringCase("opinião solicitada")
          .doesNotContainIgnoringCase("duas pessoas");
    }
  }

  /** Preserva causa, instrução ao usuário e retorno ao harness no prompt de correção. */
  @Test
  void keepsCorrectionPromptBoundToRejectedTaskAndNewVersion() throws Exception {
    assertThat(read("prompts/pde-construction/v3/prototype-correction.md"))
        .contains(
            "blockedActivities",
            "processContextJson.validationPolicy",
            "mode=AGENT_VALIDATION",
            "não recrutamento nem duas leituras humanas",
            "FUNCTIONAL_ADJUSTMENT",
            "TECHNICAL_FAILURE",
            "learningSalesCycle",
            "exatamente três alternativas",
            "userInstructions",
            "technicalHomologation",
            "versão em `taskTarget.experienceVersion`")
        .containsIgnoringCase("não publique");
  }

  /**
   * Aceita a origem técnica auditada, mas não declara pronta uma implementação ainda inexistente.
   */
  @Test
  void acceptsTechnicalHomologationRecoveryWithoutInventingExecutablePrototype() throws Exception {
    var task = technicalCorrectionTask("technicalHomologation");
    var result =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(readyCorrectionResult("vega-v7", "vega-v8"));
    ((com.fasterxml.jackson.databind.node.ObjectNode) result.path("correctionPlan"))
        .put("rejectedActivityId", "technicalHomologation");
    var contract = correctionContract();
    assertThatThrownBy(
            () ->
                PdeConstructionBpmTaskConsumer.validateCorrectionContext(
                    task, result, contract, json))
        .hasMessageContaining("protótipo executável aceito");

    result.put("decision", "BLOCKED");
    PdeConstructionBpmTaskConsumer.validate(result, contract);
    PdeConstructionBpmTaskConsumer.validateCorrectionContext(task, result, contract, json);

    var preparedTask = new java.util.HashMap<String, Object>(task);
    String privateUrl = "https://prototype.example.test/vega-private";
    preparedTask.put(
        "taskTarget",
        Map.of(
            "experienceVersion",
            "vega-v8",
            "publicUrl",
            privateUrl,
            "pdeContext",
            Map.of(
                "privatePrototypeAcceptance",
                Map.of(
                    "status",
                    "READY",
                    "prototypeVersion",
                    "vega-v8",
                    "privateAccessUrl",
                    privateUrl))));
    result.put("decision", "READY");
    PdeConstructionBpmTaskConsumer.validateCorrectionContext(preparedTask, result, contract, json);
  }

  /** Recusa falhas técnicas de outras atividades como origem de correção de protótipo. */
  @Test
  void rejectsTechnicalRecoveryFromAnotherActivity() throws Exception {
    var result = json.readTree(readyCorrectionResult("vega-v7", "vega-v8"));
    assertThatThrownBy(
            () ->
                PdeConstructionBpmTaskConsumer.validateCorrectionContext(
                    technicalCorrectionTask("access"), result, correctionContract(), json))
        .hasMessageContaining("falha técnica de homologação");
  }

  /** Exige a origem mais recente mesmo quando uma falha técnica sucede o parecer funcional. */
  @Test
  void rejectsStaleFunctionalSourceAfterTechnicalFailure() throws Exception {
    var task =
        Map.<String, Object>of(
            "taskId",
            380,
            "taskTarget",
            Map.of("experienceVersion", "vega-v8"),
            "processContextJson",
            """
        {"blockedActivities":[
          {"taskId":350,"activityId":"psiqueAdherent","category":"FUNCTIONAL_ADJUSTMENT"},
          {"taskId":377,"activityId":"technicalHomologation","category":"TECHNICAL_FAILURE"}]}
        """);
    var result = json.readTree(readyCorrectionResult("vega-v7", "vega-v8"));
    assertThatThrownBy(
            () ->
                PdeConstructionBpmTaskConsumer.validateCorrectionContext(
                    task, result, correctionContract(), json))
        .hasMessageContaining("vigente");
  }

  /** Mantém a correção funcional válida quando a falha técnica posterior já foi superada. */
  @Test
  void ignoresTechnicalFailureAfterCompletedHomologation() throws Exception {
    var task =
        Map.<String, Object>of(
            "taskId",
            381,
            "taskTarget",
            Map.of("experienceVersion", "vega-v8"),
            "processContextJson",
            """
        {"blockedActivities":[
          {"taskId":350,"activityId":"psiqueAdherent","category":"FUNCTIONAL_ADJUSTMENT"},
          {"taskId":377,"activityId":"technicalHomologation","category":"TECHNICAL_FAILURE"}],
         "completedActivities":[{"taskId":380,"activityId":"technicalHomologation"}]}
        """);
    var result = json.readTree(readyCorrectionResult("vega-v7", "vega-v8"));
    PdeConstructionBpmTaskConsumer.validateCorrectionContext(
        task, result, correctionContract(), json);
  }

  /** Monta contexto mínimo de uma falha técnica de homologação sem chamar integrações externas. */
  private Map<String, Object> technicalCorrectionTask(String activityId) throws Exception {
    var context = json.createObjectNode();
    context
        .putArray("blockedActivities")
        .addObject()
        .put("taskId", 350)
        .put("activityId", activityId)
        .put("category", "TECHNICAL_FAILURE");
    return Map.of(
        "taskId",
        378,
        "taskTarget",
        Map.of("experienceVersion", "vega-v8"),
        "processContextJson",
        json.writeValueAsString(context));
  }

  /** Usa o mesmo contrato de correção que o consumidor resolve para a fila canônica. */
  private PdeConstructionBpmTaskConsumer.BpmContract correctionContract() {
    return new PdeConstructionBpmTaskConsumer.BpmContract(
        "pde-construction-approval",
        "prototypeCorrection",
        "prompt",
        "schema",
        "pde-construction-v3",
        "READY");
  }

  /** Aceita a correção somente quando ela referencia o parecer e uma nova versão real. */
  @Test
  void validatesCorrectionBoundToFunctionalRejection() throws Exception {
    Map<String, Object> task =
        Map.of(
            "taskId",
            351,
            "taskTarget",
            Map.of("experienceVersion", "mira-private-v2"),
            "processContextJson",
            """
            {"blockedActivities":[{"taskId":350,"activityId":"psiqueAdherent",
              "category":"FUNCTIONAL_ADJUSTMENT","result":{"prototypeVersion":"mira-private-v1"}}]}
            """);
    JsonNode result = json.readTree(readyCorrectionResult("mira-private-v1", "mira-private-v2"));
    var contract =
        new PdeConstructionBpmTaskConsumer.BpmContract(
            "pde-construction-approval",
            "prototypeCorrection",
            "prompt",
            "schema",
            "pde-construction-v3",
            "READY");

    PdeConstructionBpmTaskConsumer.validate(result, contract);
    PdeConstructionBpmTaskConsumer.validateCorrectionContext(task, result, contract, json);
  }

  /** Recusa aprovação que tenta manter o identificador da versão funcional rejeitada. */
  @Test
  void rejectsCorrectionWithoutNewPrototypeVersion() throws Exception {
    Map<String, Object> task =
        Map.of(
            "taskId",
            351,
            "taskTarget",
            Map.of("experienceVersion", "mira-private-v1"),
            "processContextJson",
            """
            {"blockedActivities":[{"taskId":350,"activityId":"psiqueAdherent",
              "category":"FUNCTIONAL_ADJUSTMENT","result":{"prototypeVersion":"mira-private-v1"}}]}
            """);
    JsonNode result = json.readTree(readyCorrectionResult("mira-private-v1", "mira-private-v1"));
    var contract =
        new PdeConstructionBpmTaskConsumer.BpmContract(
            "pde-construction-approval",
            "prototypeCorrection",
            "prompt",
            "schema",
            "pde-construction-v3",
            "READY");

    assertThatThrownBy(
            () ->
                PdeConstructionBpmTaskConsumer.validateCorrectionContext(
                    task, result, contract, json))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("versão nova");
  }

  /** Recusa um parecer antigo quando já existe rejeição funcional posterior no mesmo fluxo. */
  @Test
  void rejectsCorrectionBoundToStaleFunctionalRejection() throws Exception {
    Map<String, Object> task =
        Map.of(
            "taskId",
            353,
            "taskTarget",
            Map.of("experienceVersion", "mira-private-v2"),
            "processContextJson",
            """
            {"blockedActivities":[
              {"taskId":350,"activityId":"psiqueAdherent","category":"FUNCTIONAL_ADJUSTMENT",
               "result":{"prototypeVersion":"mira-private-v1"}},
              {"taskId":352,"activityId":"psiqueRecovery","category":"FUNCTIONAL_ADJUSTMENT",
               "result":{"prototypeVersion":"mira-private-v1"}}]}
            """);
    JsonNode result = json.readTree(readyCorrectionResult("mira-private-v1", "mira-private-v2"));
    var contract =
        new PdeConstructionBpmTaskConsumer.BpmContract(
            "pde-construction-approval",
            "prototypeCorrection",
            "prompt",
            "schema",
            "pde-construction-v3",
            "READY");

    assertThatThrownBy(
            () ->
                PdeConstructionBpmTaskConsumer.validateCorrectionContext(
                    task, result, contract, json))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("rejeição funcional vigente");
  }

  /** Aceita somente jornada com decisão comparada, cinco etapas e critérios verificáveis. */
  @Test
  void validatesCompleteJourney() throws Exception {
    var result =
        json.readTree(
            """
            {"decision":"READY","rationale":"Contrato completo e coerente.",
             "selectedApproach":"Formulário guiado com entrega assistida completa.",
             "alternatives":[{},{},{}],"acceptanceCriteria":["a"],
             "experienceContract":{"stages":[{},{},{}],"maxValueTimeMinutes":8,
               "instrumentationEvents":["EXPERIENCE_STARTED","VALUE_MOMENT",
                 "READY_RESULT_USED","PREFERRED_OVER_FREE","CHECKOUT_STARTED"],
               "checkoutMode":"SIMULATED_NO_CHARGE"}}
            """);

    PdeConstructionBpmTaskConsumer.validate(result, "pde-construction-approval", "journey");
  }

  /** Exige que a arquitetura comercial comece por um protótipo privado instrumentado. */
  @Test
  void validatesPrivatePrototypeArchitecture() throws Exception {
    var result =
        json.readTree(
            """
            {
              "decision":"APPROVE",
              "rationale":"Harness mínimo preserva a estratégia e mede integridade técnica.",
              "selectedApproach":"Experiência guiada com resultado pessoal pronto em dez minutos.",
              "alternatives":[{},{},{}],
              "productArchitecture":{
                "staticResultFixtures":{"contractVersion":"PDE_STATIC_RESULT_FIXTURES_V1",
                  "required":true,"mode":"DETERMINISTIC_HOMOLOGATION_ONLY",
                  "artifactType":"STATIC_IMAGE","count":3,"width":1024,"height":1024,
                  "generator":"DETERMINISTIC_TEST_ADAPTER","providerCallsAuthorized":0,
                  "externalSideEffects":false,"commercialEvidenceEligible":false,
                  "purpose":"Validar os cartões estáticos no harness interno."},
                "privatePrototype":{
                  "scope":"Uma decisão completa em ambiente privado.",
                  "simpleInput":"Contexto informado em linguagem comum.",
                  "readyResult":"Resultado pessoal pronto para uso.",
                  "maxValueTimeMinutes":10,
                  "instrumentationEvents":[
                    "EXPERIENCE_STARTED","VALUE_MOMENT","READY_RESULT_USED",
                    "PREFERRED_OVER_FREE","CHECKOUT_STARTED"
                  ],
                  "checkoutMode":"SIMULATED_NO_CHARGE",
                  "excludedFromPrototype":["Pagamento real","Publicação"]
                }
              }
            }
            """);

    PdeConstructionBpmTaskConsumer.validate(
        result, "pde-commercial-plan-offer", "productArchitecture");
  }

  /** Bloqueia uma fixture estática que tenta combinar ausência com geração determinística. */
  @Test
  void rejectsInconsistentStaticResultFixtureContract() throws Exception {
    var result =
        json.readTree(
            """
            {"decision":"APPROVE","rationale":"Contrato visual inconsistente.",
             "selectedApproach":"Harness determinístico com contrato visual auditável.",
             "alternatives":[{},{},{}],"productArchitecture":{
               "staticResultFixtures":{"contractVersion":"PDE_STATIC_RESULT_FIXTURES_V1",
                 "required":false,"mode":"DETERMINISTIC_HOMOLOGATION_ONLY",
                 "artifactType":"STATIC_IMAGE","count":3,"width":1024,"height":1024,
                 "generator":"DETERMINISTIC_TEST_ADAPTER","providerCallsAuthorized":0,
                 "externalSideEffects":false,"commercialEvidenceEligible":false,
                 "purpose":"Contrato propositalmente inválido."},
               "privatePrototype":{"scope":"Uma decisão sintética completa.",
                 "simpleInput":"Contexto em linguagem comum.",
                 "readyResult":"Resultado pessoal pronto.","maxValueTimeMinutes":8,
                 "instrumentationEvents":["EXPERIENCE_STARTED","VALUE_MOMENT",
                   "READY_RESULT_USED","PREFERRED_OVER_FREE","CHECKOUT_STARTED"],
                 "checkoutMode":"SIMULATED_NO_CHARGE","excludedFromPrototype":["Cobrança"]}}}
            """);

    assertThatThrownBy(
            () ->
                PdeConstructionBpmTaskConsumer.validate(
                    result, "pde-commercial-plan-offer", "productArchitecture"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fixtures estáticas");
  }

  /** Separa imagens estáticas de homologação do audiovisual governado por Apolo. */
  @Test
  void keepsStaticResultFixturesOutsidePaidAudiovisualPipeline() throws Exception {
    String prompt =
        new ClassPathResource("prompts/pde-commercial-plan/v6/product-architecture.md")
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    JsonNode schema =
        json.readTree(
            new ClassPathResource("prompts/pde-commercial-plan/v6/product-architecture-schema.json")
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));

    assertThat(prompt)
        .contains(
            "READY_FOR_AGENT_VALIDATION",
            "Não proponha recrutamento",
            "Imagens estáticas do próprio resultado",
            "audiovisualRequired=false",
            "DETERMINISTIC_HOMOLOGATION_ONLY",
            "comprovam somente prontidão técnica")
        .doesNotContain("READY_FOR_PRIVATE_VALIDATION", "duas leituras", "experiência humana");
    JsonNode fixtures =
        schema
            .path("properties")
            .path("productArchitecture")
            .path("properties")
            .path("staticResultFixtures");
    assertThat(fixtures.path("type").asText()).isEqualTo("object");
    assertThat(fixtures.path("properties").path("providerCallsAuthorized").path("const").asInt())
        .isZero();
    assertThat(
            fixtures
                .path("properties")
                .path("commercialEvidenceEligible")
                .path("const")
                .asBoolean())
        .isFalse();
  }

  /** Rejeita arquitetura que omite a prova privada e tenta avançar direto ao produto completo. */
  @Test
  void rejectsArchitectureWithoutPrivatePrototype() throws Exception {
    var result =
        json.readTree(
            """
            {
              "decision":"APPROVE",
              "rationale":"Contrato tenta avançar sem validar valor.",
              "selectedApproach":"Aplicação completa antes de qualquer leitura privada.",
              "alternatives":[{},{},{}],
              "productArchitecture":{}
            }
            """);

    assertThatThrownBy(
            () ->
                PdeConstructionBpmTaskConsumer.validate(
                    result, "pde-commercial-plan-offer", "productArchitecture"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Protótipo privado");
  }

  /** Bloqueia pacote que não contém todos os grupos mínimos de entregáveis. */
  @Test
  void rejectsIncompleteDeliveryPackage() throws Exception {
    var result =
        json.readTree(
            """
            {"decision":"READY","rationale":"Pacote parcial.",
             "selectedApproach":"Materiais editáveis com orientação guiada completa.",
             "alternatives":[{},{},{}],"acceptanceCriteria":["a"],
             "deliveryPackage":{"version":"pde-private-prototype-v1","assets":[{},{}],
               "instrumentationEvents":["EXPERIENCE_STARTED","VALUE_MOMENT",
                 "READY_RESULT_USED","PREFERRED_OVER_FREE","CHECKOUT_STARTED"]}}
            """);

    assertThatThrownBy(
            () ->
                PdeConstructionBpmTaskConsumer.validate(
                    result, "pde-construction-approval", "deliverables"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Pacote");
  }

  /** Impede decisão de abordagem truncada pelo contrato estruturado. */
  @Test
  void rejectsTruncatedSelectedApproach() throws Exception {
    var result =
        json.readTree(
            """
            {"decision":"READY","rationale":"A jornada atende aos gates.",
             "selectedApproach":"sem","alternatives":[{},{},{}],
             "acceptanceCriteria":["critério"],
             "experienceContract":{"stages":[{},{},{}],"maxValueTimeMinutes":8,
               "instrumentationEvents":["EXPERIENCE_STARTED","VALUE_MOMENT",
                 "READY_RESULT_USED","PREFERRED_OVER_FREE","CHECKOUT_STARTED"],
               "checkoutMode":"SIMULATED_NO_CHARGE"}}
            """);

    assertThatThrownBy(
            () ->
                PdeConstructionBpmTaskConsumer.validate(
                    result, "pde-construction-approval", "journey"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("decisão comparada");
  }

  /** Aceita arquitetura aprovada quando o protótipo privado está limitado e instrumentado. */
  @Test
  void validatesProductArchitecture() throws Exception {
    var result =
        json.readTree(
            """
            {"decision":"APPROVE","rationale":"Arquitetura coerente com os contratos.",
             "selectedApproach":"Experiência assistida com primeiro valor verificável.",
             "alternatives":[{},{},{}],"productArchitecture":{"format":"PDE",
               "staticResultFixtures":{"contractVersion":"PDE_STATIC_RESULT_FIXTURES_V1",
                 "required":false,"mode":"NOT_REQUIRED","artifactType":"NONE",
                 "count":0,"width":0,"height":0,"generator":"NONE",
                 "providerCallsAuthorized":0,"externalSideEffects":false,
                 "commercialEvidenceEligible":false,"purpose":"Sem fixture nesta arquitetura."},
               "privatePrototype":{"maxValueTimeMinutes":8,
                 "scope":"Uma decisão privada completa.",
                 "simpleInput":"Contexto em linguagem comum.",
                 "readyResult":"Resultado pessoal pronto.",
                 "instrumentationEvents":["EXPERIENCE_STARTED","VALUE_MOMENT",
                   "READY_RESULT_USED","PREFERRED_OVER_FREE","CHECKOUT_STARTED"],
                 "checkoutMode":"SIMULATED_NO_CHARGE",
                 "excludedFromPrototype":["Pagamento real"]}}}
            """);

    PdeConstructionBpmTaskConsumer.validate(
        result, "pde-commercial-plan-offer", "productArchitecture");
  }

  /** Exige artefato funcional, eventos e isolamento antes de liberar uma degustação. */
  @Test
  void rejectsTastingWithoutFunctionalArtifact() throws Exception {
    var result =
        json.readTree(
            """
            {"decision":"READY","rationale":"A descrição ainda não materializa valor.",
             "selectedApproach":"Microexperiência limitada ao primeiro resultado real.",
             "alternatives":[{},{},{}],"acceptanceCriteria":["valor real"],
             "tastingExperience":{"steps":[{},{},{}]},
             "functionalArtifact":{"content":""},
             "instrumentationEvents":["TASTING_STARTED"],"testIsolation":"qa=true"}
            """);

    assertThatThrownBy(
            () ->
                PdeConstructionBpmTaskConsumer.validate(
                    result, "pde-tasting-proof-of-value", "materialization"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Microexperiência");
  }

  /** Exige referência da venda e entregáveis concretos na personalização pós-compra. */
  @Test
  void validatesPaidPersonalizationPackage() throws Exception {
    var result =
        json.readTree(
            """
            {"decision":"READY","rationale":"Venda e versão contratada foram preservadas.",
             "selectedApproach":"Personalização mínima com revisão humana e acesso rastreável.",
             "alternatives":[{},{},{}],"acceptanceCriteria":["entrega íntegra"],
             "personalizationPackage":{"contractReference":"sale:42","deliverables":[{}]},
             "qualityChecks":[{}],"accessHandoff":{}}
            """);

    PdeConstructionBpmTaskConsumer.validate(
        result, "venda-entrega-satisfacao-cliente", "materialization");
  }

  /** Encerra o processo Codex filho antes do lançador quando a atividade ultrapassa o timeout. */
  @Test
  void terminatesWholeProcessTree() {
    Process process = mock(Process.class);
    ProcessHandle child = mock(ProcessHandle.class);
    when(process.descendants()).thenReturn(Stream.of(child));

    PdeConstructionBpmTaskConsumer.terminateTree(process);

    verify(child).destroyForcibly();
    verify(process).destroyForcibly();
  }

  /** Verifica recursivamente o subconjunto estrito exigido pelos contratos de saída. */
  private void assertStrictObjects(JsonNode node, String resource) {
    if (node.isObject() && "object".equals(node.path("type").asText())) {
      assertThat(node.path("additionalProperties").asBoolean(true))
          .as("additionalProperties em %s", resource)
          .isFalse();
      Set<String> propertyNames =
          StreamSupport.stream(
                  java.util.Spliterators.spliteratorUnknownSize(
                      node.path("properties").fieldNames(), java.util.Spliterator.ORDERED),
                  false)
              .collect(Collectors.toSet());
      Set<String> required =
          StreamSupport.stream(node.path("required").spliterator(), false)
              .map(JsonNode::asText)
              .collect(Collectors.toSet());
      assertThat(required).as("required em %s", resource).isEqualTo(propertyNames);
    }
    node.forEach(child -> assertStrictObjects(child, resource));
  }

  /** Monta a saída completa de correção usada nos testes de vínculo e versionamento. */
  private String readyCorrectionResult(String previousVersion, String correctedVersion) {
    return """
        {
          "decision":"READY",
          "rationale":"A causa funcional foi corrigida sem alterar o mecanismo de valor.",
          "alternatives":[{},{},{}],
          "selectedApproach":"Manter o resultado funcional visível e separar o aviso de homologação.",
          "correctionPlan":{
            "sourceTaskId":350,
            "rejectedActivityId":"psiqueAdherent",
            "previousPrototypeVersion":"%s",
            "correctedPrototypeVersion":"%s",
            "rootCause":"A conclusão substituía a rotina útil por uma mensagem administrativa.",
            "userInstructions":["Publique a nova versão e execute novamente a homologação técnica."],
            "changes":["A rotina continua visível após o cenário sintético ser concluído."],
            "nextActivityId":"technicalHomologation",
            "verification":{
              "routineRemainsVisible":true,
              "valueAndLimitsRemainVisible":true,
              "nextActionVisible":true,
              "newVersionPublished":true,
              "technicalRevalidationRequired":true,
              "noExternalSideEffects":true
            }
          },
          "acceptanceCriteria":["rotina visível"],
          "requiredChanges":[]
        }
        """
        .formatted(previousVersion, correctedVersion);
  }

  /** Cria o consumidor sem acesso externo para validar envelopes de falha e replay. */
  private PdeConstructionBpmTaskConsumer consumer() {
    LandingGeneratorAgentProperties properties = new LandingGeneratorAgentProperties();
    properties.setBackendUrl("http://localhost:1");
    return new PdeConstructionBpmTaskConsumer(
        properties,
        json,
        mock(AutomaticExecutionControl.class),
        mock(CodexTelemetryReporter.class));
  }

  /** Monta a identidade mínima da tarefa real sem fixar o conteúdo comercial do produto. */
  private Map<String, Object> productArchitectureTask() {
    return Map.of(
        "taskId",
        493L,
        "processCode",
        "pde-commercial-plan-offer",
        "activityId",
        "productArchitecture",
        "sourceReference",
        "product-discovery-cycle:71");
  }

  /** Retorna uma arquitetura genérica completa usada para comprovar replay sem inferência. */
  private String validProductArchitectureResult() {
    return """
        {"decision":"APPROVE","rationale":"Arquitetura privada, limitada e auditável.",
         "selectedApproach":"Experiência guiada com resultado pessoal pronto e verificável.",
         "alternatives":[{},{},{}],"productArchitecture":{"format":"PDE",
           "staticResultFixtures":{"contractVersion":"PDE_STATIC_RESULT_FIXTURES_V1",
             "required":false,"mode":"NOT_REQUIRED","artifactType":"NONE",
             "count":0,"width":0,"height":0,"generator":"NONE",
             "providerCallsAuthorized":0,"externalSideEffects":false,
             "commercialEvidenceEligible":false,"purpose":"Sem fixture nesta arquitetura."},
           "privatePrototype":{"scope":"Uma decisão privada completa.",
             "simpleInput":"Contexto em linguagem comum.",
             "readyResult":"Resultado pessoal pronto.","maxValueTimeMinutes":8,
             "instrumentationEvents":["EXPERIENCE_STARTED","VALUE_MOMENT",
               "READY_RESULT_USED","PREFERRED_OVER_FREE","CHECKOUT_STARTED"],
             "checkoutMode":"SIMULATED_NO_CHARGE",
             "excludedFromPrototype":["Pagamento real"]}}}
        """;
  }

  /** Lê um prompt do classpath com a mesma codificação usada em produção. */
  private String read(String resource) throws IOException {
    return new ClassPathResource(resource)
        .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
  }
}
