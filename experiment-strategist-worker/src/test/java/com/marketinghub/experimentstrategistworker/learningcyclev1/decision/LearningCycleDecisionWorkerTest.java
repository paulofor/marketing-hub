package com.marketinghub.experimentstrategistworker.learningcyclev1.decision;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experimentstrategistworker.AutomaticExecutionControl;
import com.marketinghub.experimentstrategistworker.WorkerProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Responsabilidade: verificar o consumo real da fila de Atena sem usar modelo ou rede produtivos.
 */
class LearningCycleDecisionWorkerTest {
  @TempDir Path temporary;
  private final ObjectMapper json = new ObjectMapper();

  /** Mantém consulta, request antes do modelo e callback estritamente no contrato do ciclo. */
  @Test
  void consumesPendingAndAuditsBeforeRunning() throws Exception {
    List<String> calls = new ArrayList<>();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          String route = exchange.getRequestURI().getPath();
          calls.add(exchange.getRequestMethod() + " " + route);
          byte[] answer;
          if (route.endsWith("/pending"))
            answer =
                json.writeValueAsBytes(
                    List.of(
                        Map.of(
                            "proposalId",
                            7,
                            "cycleId",
                            1,
                            "leaseToken",
                            "lease",
                            "context",
                            Map.of("cycleId", 1))));
          else {
            var body = json.readTree(exchange.getRequestBody());
            assertThat(body.path("leaseToken").asText()).isEqualTo("lease");
            if (route.endsWith("/request")) {
              assertThat(body.path("prompt").asText()).contains("Atena");
              assertThat(body.path("serviceTierReason").asText()).isNotBlank();
            }
            if (route.endsWith("/result"))
              assertThat(body.path("rawResponse").asText()).isEqualTo("raw-model-answer");
            answer = "{\"status\":\"READY\"}".getBytes();
          }
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, answer.length);
          exchange.getResponseBody().write(answer);
          exchange.close();
        });
    server.start();
    try {
      var properties = properties();
      properties.setBackendUrl("http://127.0.0.1:" + server.getAddress().getPort());
      var runner = mock(LearningCycleDecisionRunner.class);
      when(runner.prepare(any()))
          .thenReturn(
              new LearningCycleDecisionRunner.Prepared(
                  "Atena: request", json.createObjectNode(), "fixture"));
      when(runner.run(any()))
          .thenAnswer(
              call -> {
                assertThat(calls).hasSize(2);
                return new LearningCycleDecisionRunner.Result("raw-model-answer", null, 2L, 3L);
              });
      var control = mock(AutomaticExecutionControl.class);
      when(control.allowsAutomaticExecution()).thenReturn(true);
      new LearningCycleDecisionConsumer(properties, runner, control).processOne();
      assertThat(calls)
          .containsExactly(
              "GET " + LearningCycleDecisionConsumer.ENDPOINT + "/pending",
              "PUT " + LearningCycleDecisionConsumer.ENDPOINT + "/7/request",
              "POST " + LearningCycleDecisionConsumer.ENDPOINT + "/7/result");
    } finally {
      server.stop(0);
    }
  }

  /** STOP impede consulta e consumo, preservando o controle operacional do módulo. */
  @Test
  void stopDoesNotConsumeModel() {
    var runner = mock(LearningCycleDecisionRunner.class);
    var control = mock(AutomaticExecutionControl.class);
    new LearningCycleDecisionConsumer(properties(), runner, control).processOne();
    verifyNoInteractions(runner);
  }

  /**
   * Prompt e schema versionados preservam fonte, hipótese, três alternativas e aprovação humana.
   */
  @Test
  void preparesCompleteVersionedContract() throws Exception {
    var runner = new LearningCycleDecisionRunner(properties(), json);
    var prepared = runner.prepare(json.createObjectNode().put("cycleId", 901));
    assertThat(prepared.prompt())
        .contains("901", "três alternativas", "aprovação humana", "não provam motivo")
        .doesNotContain("{{CYCLE_CONTEXT}}");
    assertThat(prepared.schema().path("additionalProperties").asBoolean(true)).isFalse();
    assertThat(prepared.schema().path("properties").path("alternatives").path("minItems").asInt())
        .isEqualTo(3);
    assertThat(runner.command(temporary, temporary.resolve("s"), temporary.resolve("a")))
        .contains(
            "--ignore-user-config",
            "--ephemeral",
            "read-only",
            "features.shell_tool=false",
            "features.apps=false",
            "features.multi_agent=false",
            "features.hooks=false",
            "mcp_servers={}",
            "web_search=\"disabled\"",
            "--output-schema");
  }

  /** Saída malformada permanece bruta para bloqueio e auditoria no backend. */
  @Test
  void preservesMalformedOutputForBackendValidation() throws Exception {
    var properties = properties();
    properties.setCodexCommand(
        executable(
                """
        #!/bin/sh
        while [ "$#" -gt 0 ]; do
          if [ "$1" = "--output-last-message" ]; then shift; target="$1"; fi
          shift
        done
        cat >/dev/null
        printf '{invalid model response' > "$target"
        printf '%s\\n' '{"type":"turn.completed","usage":{"input_tokens":11,"output_tokens":7}}'
        """)
            .toString());
    var runner = new LearningCycleDecisionRunner(properties, json);
    var result = runner.run(runner.prepare(json.createObjectNode()));
    assertThat(result.rawResponse()).isEqualTo("{invalid model response");
    assertThat(result.inputTokens()).isEqualTo(11L);
    assertThat(result.outputTokens()).isEqualTo(7L);
    assertThat(result.error()).isNull();
  }

  /** Timeout interrompe o processo; ausência de consumo não é reportada como custo zero. */
  @Test
  void boundsTimeoutAndKeepsUnknownUsage() throws Exception {
    var properties = properties();
    properties.setCodexTimeout(Duration.ofMillis(80));
    properties.setCodexCommand(executable("#!/bin/sh\ncat >/dev/null\nsleep 20\n").toString());
    var runner = new LearningCycleDecisionRunner(properties, json);
    var result = runner.run(runner.prepare(json.createObjectNode()));
    assertThat(result.error()).contains("tempo");
    assertThat(result.inputTokens()).isNull();
  }

  /** Código de falha do processo é devolvido sem tratar erro de integração como sucesso. */
  @Test
  void recordsProcessExitFailure() throws Exception {
    var properties = properties();
    properties.setCodexCommand(executable("#!/bin/sh\ncat >/dev/null\nexit 7\n").toString());
    var runner = new LearningCycleDecisionRunner(properties, json);
    assertThat(runner.run(runner.prepare(json.createObjectNode())).error()).contains("código 7");
  }

  /** Declara somente configuração local e modelo fictício. */
  private WorkerProperties properties() {
    var value = new WorkerProperties();
    value.setBackendUrl("http://127.0.0.1:1");
    value.setModel("fixture-no-paid-model");
    value.setCodexCommand("never-call-real-model");
    return value;
  }

  /** Cria um test double executável isolado no diretório temporário do teste. */
  private Path executable(String source) throws Exception {
    var file = temporary.resolve("fake-codex-" + System.nanoTime());
    Files.writeString(file, source);
    assertThat(file.toFile().setExecutable(true)).isTrue();
    return file;
  }
}
