package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: comprovar retomada de Psique sem perder parecer nem repetir inferência. */
class CustomerBpmTaskOutboxTest {
  @TempDir Path directory;
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  /** Reenvia o mesmo callback depois de reinício e só o remove após confirmação do backend. */
  @Test
  void preservesCallbackAcrossRestartWithoutStartingAnotherModel() throws Exception {
    AtomicInteger attempts = new AtomicInteger();
    AtomicReference<String> received = new AtomicReference<>();
    HttpServer server = callbackServer(attempts, received, true);
    Path state = directory.resolve("callback-restart");
    Map<String, Object> task = task(448L);
    Map<String, Object> body =
        Map.of(
            "resultJson", "{\"decision\":\"APPROVED\"}",
            "evidenceJson", "{\"proof\":true}");
    CustomerBpmTaskOutbox outbox = new CustomerBpmTaskOutbox(state, json);
    outbox.save(new CustomerBpmTaskOutbox.Pending(task, audit(), List.of(), true, "result", body));
    try {
      consumer(server, state).processOne();

      assertThat(attempts).hasValue(1);
      assertThat(outbox.read()).isNotNull();
      assertThat(outbox.read().callback()).isEqualTo(body);

      consumer(server, state).processOne();

      assertThat(attempts).hasValue(2);
      assertThat(json.readTree(received.get())).isEqualTo(json.valueToTree(body));
      assertThat(outbox.read()).isNull();
    } finally {
      server.stop(0);
    }
  }

  /**
   * Converte resultado rejeitado repetidamente em bloqueio contratualmente válido sem nova
   * inferência.
   */
  @Test
  void blocksPersistentlyRejectedResultWithoutStartingAnotherModel() throws Exception {
    AtomicInteger attempts = new AtomicInteger();
    AtomicReference<String> received = new AtomicReference<>();
    List<String> paths = new CopyOnWriteArrayList<>();
    HttpServer server = resultRejectionServer(attempts, received, paths);
    Path state = directory.resolve("permanent-result-rejection");
    Map<String, Object> body =
        Map.of(
            "resultJson",
            "{\"decision\":\"APPROVED\"}",
            "evidenceJson",
            "{\"proof\":true}",
            "modelUsages",
            List.of(
                Map.of(
                    "modelCode",
                    "gpt-5.6-sol",
                    "inputTokens",
                    1000,
                    "cachedInputTokens",
                    100,
                    "outputTokens",
                    40)),
            "executionAudit",
            audit());
    new CustomerBpmTaskOutbox(state, json)
        .save(
            new CustomerBpmTaskOutbox.Pending(
                task(448L), audit(), List.of(), true, "result", body));
    try {
      for (int i = 0; i < 4; i++) consumer(server, state).processOne();

      JsonNode failure = json.readTree(received.get());
      assertThat(attempts).hasValue(4);
      assertThat(paths.subList(0, 3)).allMatch(path -> path.endsWith("/result"));
      assertThat(paths.get(3)).endsWith("/failure");
      assertThat(failure.path("error").asText())
          .contains("CALLBACK_RESULT_REJECTED_AFTER_RETRIES", "sem nova inferência");
      assertThat(failure.path("resultJson").asText()).isEqualTo(body.get("resultJson"));
      assertThat(failure.path("modelUsages").path(0).path("inputTokens").asLong()).isEqualTo(1000L);
      assertThat(failure.path("blockerGuidance").path("category").asText())
          .isEqualTo("TECHNICAL_FAILURE");
      assertThat(failure.path("blockerGuidance").path("recommendedAction").asText())
          .contains("corrija a integração");
      assertThat(failure.path("blockerGuidance").path("helpLinks").isArray()).isTrue();
      assertThat(failure.path("blockerGuidance").path("helpLinks")).isNotEmpty();
      assertThat(new CustomerBpmTaskOutbox(state, json).read()).isNull();
    } finally {
      server.stop(0);
    }
  }

  /** Bloqueia execução interrompida sem saída e preserva os tokens já informados. */
  @Test
  void blocksInterruptedModelWithoutStartingAnotherInference() throws Exception {
    AtomicInteger attempts = new AtomicInteger();
    AtomicReference<String> received = new AtomicReference<>();
    HttpServer server = callbackServer(attempts, received, false);
    Path state = directory.resolve("interrupted-model");
    CustomerBpmTaskOutbox outbox = new CustomerBpmTaskOutbox(state, json);
    outbox.save(
        new CustomerBpmTaskOutbox.Pending(task(448L), audit(), List.of(), true, null, null));
    Files.writeString(
        outbox.events(),
        "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":1200,\"cached_input_tokens\":200,\"output_tokens\":30}}\n");
    try {
      consumer(server, state).processOne();

      JsonNode failure = json.readTree(received.get());
      assertThat(attempts).hasValue(1);
      assertThat(failure.path("error").asText())
          .contains("sem nova inferência", "cobrança duplicada");
      assertThat(failure.path("modelUsages").path(0).path("inputTokens").asLong()).isEqualTo(1200L);
      assertThat(failure.path("executionAudit").path("promptSent").asText())
          .isEqualTo("núcleo\n\natividade");
      assertThat(outbox.read()).isNull();
    } finally {
      server.stop(0);
    }
  }

  /** Entrega a resposta final gravada antes do reinício sem executar novamente o modelo. */
  @Test
  void recoversCompletedModelOutputAfterRestart() throws Exception {
    AtomicInteger attempts = new AtomicInteger();
    AtomicReference<String> received = new AtomicReference<>();
    HttpServer server = callbackServer(attempts, received, false);
    Path state = directory.resolve("completed-output");
    CustomerBpmTaskOutbox outbox = new CustomerBpmTaskOutbox(state, json);
    Map<String, Object> task = agentScenarioTask(449L);
    var visualEvidence = List.of(visualEvidence());
    outbox.save(new CustomerBpmTaskOutbox.Pending(task, audit(), visualEvidence, true, null, null));
    String raw = agentScenarioResult();
    Files.writeString(outbox.output(), raw);
    Files.writeString(
        outbox.events(),
        "{\"type\":\"turn.completed\",\"usage\":{\"input_tokens\":2500,\"cached_input_tokens\":500,\"output_tokens\":400}}\n");
    try {
      consumer(server, state).processOne();

      JsonNode callback = json.readTree(received.get());
      assertThat(attempts).hasValue(1);
      assertThat(callback.path("resultJson").asText()).isEqualTo(raw);
      assertThat(callback.path("modelUsages").path(0).path("outputTokens").asLong())
          .isEqualTo(400L);
      assertThat(callback.path("evidenceJson").asText()).contains("visualEvidenceIds");
      assertThat(outbox.read()).isNull();
    } finally {
      server.stop(0);
    }
  }

  /** Cria backend local que falha apenas na primeira entrega quando solicitado. */
  private HttpServer callbackServer(
      AtomicInteger attempts, AtomicReference<String> received, boolean failFirst)
      throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/internal/agent-tasks/",
        exchange -> {
          int attempt = attempts.incrementAndGet();
          String body =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          if (failFirst && attempt == 1) {
            exchange.sendResponseHeaders(500, -1);
          } else {
            received.set(body);
            exchange.sendResponseHeaders(204, -1);
          }
          exchange.close();
        });
    server.start();
    return server;
  }

  /** Rejeita três callbacks de resultado e aceita somente o bloqueio técnico subsequente. */
  private HttpServer resultRejectionServer(
      AtomicInteger attempts, AtomicReference<String> received, List<String> paths)
      throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/internal/agent-tasks/",
        exchange -> {
          attempts.incrementAndGet();
          paths.add(exchange.getRequestURI().getPath());
          String body =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          if (exchange.getRequestURI().getPath().endsWith("/result")) {
            exchange.sendResponseHeaders(500, -1);
          } else {
            received.set(body);
            exchange.sendResponseHeaders(204, -1);
          }
          exchange.close();
        });
    server.start();
    return server;
  }

  /** Monta o consumidor apontado ao backend e ao mesmo volume da tentativa anterior. */
  private CustomerBpmTaskConsumer consumer(HttpServer server, Path state) {
    return new CustomerBpmTaskConsumer(
        "http://127.0.0.1:" + server.getAddress().getPort(),
        directory.resolve("modelo-que-nao-deve-ser-chamado").toString(),
        "gpt-5.6-sol",
        "max",
        directory.toString(),
        directory.toString(),
        state.toString(),
        json,
        null,
        null,
        null,
        new CodexProcessSupervisor(
            Duration.ofSeconds(2), Duration.ofSeconds(4), Duration.ofMillis(50)));
  }

  /** Monta a tarefa comercial mínima usada na retomada da #448. */
  private Map<String, Object> task(long id) {
    return Map.of(
        "taskId",
        id,
        "processCode",
        "opala-commercial-preparation-v1",
        "activityId",
        "humanExperienceReview",
        "sourceReference",
        "experiment:92",
        "processContextJson",
        "{}");
  }

  /** Monta a tarefa sintética cujo resultado possui validação determinística autocontida. */
  private Map<String, Object> agentScenarioTask(long id) {
    return Map.of(
        "taskId",
        id,
        "processCode",
        "pde-construction-approval",
        "activityId",
        "psiqueAdherent",
        "sourceReference",
        "product:10@agent-validation-v1",
        "taskTarget",
        Map.of(
            "productId", 10L, "productSlug", "produto-teste", "experienceVersion", "private-v1"));
  }

  /** Preserva o envelope integral já enviado ao modelo antes da interrupção. */
  private Map<String, Object> audit() {
    return Map.of(
        "executionMode",
        "MODEL",
        "modelCode",
        "gpt-5.6-sol",
        "reasoningEffort",
        "max",
        "promptSent",
        "núcleo\n\natividade",
        "agentPromptPart",
        "núcleo",
        "activityPromptPart",
        "atividade",
        "accessedUrls",
        List.of());
  }

  /** Representa a captura já persistida no backend antes do reinício. */
  private BpmVisualEvidenceBackendClient.UploadedVisualEvidence visualEvidence() {
    return new BpmVisualEvidenceBackendClient.UploadedVisualEvidence(
        910901L,
        "capture-1",
        "scenario-screen",
        "FULL_PAGE",
        "Cenário completo",
        "IPHONE_15_PRO",
        1,
        null,
        393,
        852,
        852,
        0,
        "https://produto.example/privado",
        "https://produto.example/privado",
        "/api/agent-tasks/449/visual-evidence/910901/content",
        1200L,
        "a".repeat(64),
        Instant.parse("2026-09-18T02:00:00Z"),
        null);
  }

  /** Retorna parecer sintético completo para validar recuperação do arquivo final. */
  private String agentScenarioResult() {
    return """
        {"contractVersion":"PDE_PSIQUE_AGENT_SCENARIO_V1","decision":"APPROVED","scenarioCode":"ADHERENT","sourceReference":"product:10@agent-validation-v1","productId":10,"productSlug":"produto-teste","prototypeVersion":"private-v1","trafficClass":"AGENT_VALIDATION","internalMarker":"mh_internal_test","syntheticEvaluation":true,"humanEvidenceClaimed":false,"commercialEvidenceClaimed":false,"sideEffects":{"paymentEnabled":false,"published":false,"campaignCreated":false,"mediaSpendBrl":0},"experienceAssessment":{"evidenceBoundary":"Cenário sintético persistido."},"checks":{"sameProductAndVersion":true,"isolatedFreshSession":true,"functionalOutcomeMatchesScenario":true,"lowEffortNoPrompting":true,"accessibilityAndResponsive":true,"privacyPreserved":true,"internalTrafficSegregated":true,"safeLimits":true,"noExternalSideEffects":true},"visualAudit":{"captureSessionId":"capture-1","evidenceIds":[910901],"visualHierarchy":"Clara","legibility":"Legível","affectiveResponse":"Segura","trustCues":"Limites visíveis"},"evidence":["Screenshot 910901"],"requiredChanges":[],"rootCause":"Percurso confirmado sem efeito externo."}
        """
        .trim();
  }
}
