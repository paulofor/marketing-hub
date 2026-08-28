package com.marketinghub.pde.harness.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.pde.harness.v1.support.PdeHarnessTestSupport;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Homologa o fluxo completo do facade Java com dados sintéticos e App Server simulado. */
class PdeHarnessSdkTest {
  private final ObjectMapper mapper = new ObjectMapper();
  @TempDir Path temporaryDirectory;

  /** Consolida thread, turno, saída, eventos, uso e hashes no caminho feliz. */
  @Test
  void executesCompleteAgentTurn() {
    PdeHarnessConfiguration configuration = configuration(null, Duration.ofSeconds(2));
    List<PdeHarnessEvent> observed = new CopyOnWriteArrayList<>();
    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration)) {
      PdeAgentRunResult result = sdk.execute(request("cliente-a", "missao-1", null), observed::add);

      assertEquals(PdeRunStatus.COMPLETED, result.status());
      assertEquals("{\"message\":\"Resposta para cliente-a\"}", result.output());
      assertEquals("Resposta para cliente-a", result.structuredOutput().path("message").asText());
      assertTrue(result.threadId().startsWith("thread-"));
      assertTrue(result.turnId().startsWith("turn-"));
      assertNotNull(result.tokenUsage());
      assertEquals(12, result.tokenUsage().path("inputTokens").asInt());
      assertFalse(result.events().isEmpty());
      assertEquals(result.events().size(), observed.size());
      assertEquals(64, result.promptSha256().length());
      assertEquals(64, result.outputSchemaSha256().length());
      assertEquals("gpt-test", result.model());
      assertEquals("prompt-v1", result.promptVersion());
      assertEquals("schema-v1", result.outputSchemaVersion());
      ((ObjectNode) result.structuredOutput()).put("message", "mutação");
      ((ObjectNode) result.tokenUsage()).put("inputTokens", 999);
      assertEquals("Resposta para cliente-a", result.structuredOutput().path("message").asText());
      assertEquals(12, result.tokenUsage().path("inputTokens").asInt());
    }
  }

  /** Retoma a thread persistida sem criar uma identidade nova para a conversa. */
  @Test
  void resumesPersistedThread() {
    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration(null, Duration.ofSeconds(2)))) {
      PdeAgentRunResult first = sdk.execute(request("cliente-a", "missao-1", null));
      PdeAgentRunResult resumed = sdk.execute(request("cliente-a", "missao-2", first.threadId()));

      assertEquals(first.threadId(), resumed.threadId());
      assertNotEquals(first.turnId(), resumed.turnId());
    }
  }

  /** Mantém threads, workspaces e saídas distintos entre clientes sintéticos. */
  @Test
  void segregatesTwoCustomerExecutions() {
    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration(null, Duration.ofSeconds(2)))) {
      PdeAgentRunResult customerA = sdk.execute(request("cliente-a", "missao-1", null));
      PdeAgentRunResult customerB = sdk.execute(request("cliente-b", "missao-1", null));

      assertNotEquals(customerA.threadId(), customerB.threadId());
      assertNotEquals(customerA.context().workspace(), customerB.context().workspace());
      assertTrue(customerA.output().contains("cliente-a"));
      assertFalse(customerA.output().contains("cliente-b"));
      assertTrue(customerB.output().contains("cliente-b"));
      assertFalse(customerB.output().contains("cliente-a"));
    }
  }

  /** Mantém correlação correta quando dois clientes executam turnos ao mesmo tempo. */
  @Test
  void segregatesConcurrentCustomerExecutions() throws Exception {
    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration(null, Duration.ofSeconds(2)));
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      CompletableFuture<PdeAgentRunResult> customerA =
          CompletableFuture.supplyAsync(
              () -> sdk.execute(request("cliente-a", "missao-concorrente", null)), executor);
      CompletableFuture<PdeAgentRunResult> customerB =
          CompletableFuture.supplyAsync(
              () -> sdk.execute(request("cliente-b", "missao-concorrente", null)), executor);

      PdeAgentRunResult resultA = customerA.get();
      PdeAgentRunResult resultB = customerB.get();
      assertNotEquals(resultA.threadId(), resultB.threadId());
      assertTrue(resultA.output().contains("cliente-a"));
      assertFalse(resultA.output().contains("cliente-b"));
      assertTrue(resultB.output().contains("cliente-b"));
      assertFalse(resultB.output().contains("cliente-a"));
    }
  }

  /** Rejeita workspace externo à raiz antes de iniciar um turno de modelo. */
  @Test
  void rejectsWorkspaceOutsideConfiguredRoot() {
    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration(null, Duration.ofSeconds(2)))) {
      PdeRunContext context =
          new PdeRunContext(
              "produto-teste",
              "v1",
              "cliente-a",
              "missao-1",
              temporaryDirectory.resolve("fora-da-raiz"));
      PdeAgentRunRequest request =
          PdeAgentRunRequest.newThread(
              context,
              "gpt-test",
              "cliente-a",
              "prompt-v1",
              PdeHarnessTestSupport.validOutputSchema(),
              "schema-v1",
              false);

      PdeHarnessException error =
          assertThrows(PdeHarnessException.class, () -> sdk.execute(request));
      assertEquals(PdeHarnessFailureCategory.CONFIGURATION, error.category());
    }
  }

  /** Valida schema antes de tentar iniciar um comando externo inexistente. */
  @Test
  void rejectsInvalidSchemaBeforeStartingAppServer() throws Exception {
    Path workspaceRoot = temporaryDirectory.resolve("workspaces-invalid-schema");
    PdeHarnessConfiguration configuration =
        new PdeHarnessConfiguration(
            temporaryDirectory.resolve("codex-inexistente").toString(),
            List.of("app-server"),
            temporaryDirectory.resolve("codex-invalid-schema"),
            workspaceRoot,
            Duration.ofSeconds(1),
            Duration.ofSeconds(1),
            "0.149.0",
            "test",
            "Test",
            "test",
            Map.of(),
            false);
    JsonNode invalidSchema =
        mapper.readTree("{\"type\":\"array\",\"items\":{\"type\":\"string\"}}");
    PdeAgentRunRequest request =
        PdeAgentRunRequest.newThread(
            new PdeRunContext(
                "produto-teste",
                "v1",
                "cliente-a",
                "missao-schema",
                workspaceRoot.resolve("cliente-a")),
            "gpt-test",
            "prompt",
            "prompt-v1",
            invalidSchema,
            "schema-invalido",
            false);

    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration)) {
      PdeHarnessException error =
          assertThrows(PdeHarnessException.class, () -> sdk.execute(request));
      assertEquals(PdeHarnessFailureCategory.CONFIGURATION, error.category());
    }
  }

  /** Interrompe o turno no teto e devolve timeout sem retentativa implícita. */
  @Test
  void interruptsTurnAfterTimeout() {
    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration("timeout", Duration.ofMillis(80)))) {
      PdeHarnessException error =
          assertThrows(
              PdeHarnessException.class,
              () -> sdk.execute(request("cliente-a", "missao-timeout", null)));

      assertEquals(PdeHarnessFailureCategory.TIMEOUT, error.category());
    }
  }

  /** Converte falha objetiva de login em bloqueio recuperável, nunca em sucesso. */
  @Test
  void mapsAuthenticationFailureToBlockedResult() {
    try (PdeHarnessSdk sdk =
        new PdeHarnessSdk(configuration("authentication-failure", Duration.ofSeconds(2)))) {
      PdeAgentRunResult result = sdk.execute(request("cliente-a", "missao-auth", null));

      assertEquals(PdeRunStatus.BLOCKED, result.status());
      assertTrue(result.errorMessage().contains("Unauthorized"));
    }
  }

  /** Rejeita conclusão que não contenha um JSON parseável. */
  @Test
  void rejectsInvalidStructuredOutput() {
    try (PdeHarnessSdk sdk =
        new PdeHarnessSdk(configuration("invalid-output", Duration.ofSeconds(2)))) {
      PdeHarnessException error =
          assertThrows(
              PdeHarnessException.class,
              () -> sdk.execute(request("cliente-a", "missao-json-invalido", null)));

      assertEquals(PdeHarnessFailureCategory.EXECUTION_FAILED, error.category());
    }
  }

  /** Rejeita JSON válido que contradiga o schema versionado da tarefa. */
  @Test
  void rejectsOutputThatViolatesSchema() {
    try (PdeHarnessSdk sdk =
        new PdeHarnessSdk(configuration("schema-mismatch", Duration.ofSeconds(2)))) {
      PdeHarnessException error =
          assertThrows(
              PdeHarnessException.class,
              () -> sdk.execute(request("cliente-a", "missao-schema-divergente", null)));

      assertEquals(PdeHarnessFailureCategory.EXECUTION_FAILED, error.category());
    }
  }

  /** Monta uma request nova ou retomada usando somente identificadores sintéticos. */
  private PdeAgentRunRequest request(
      String customerReference, String missionReference, String existingThreadId) {
    Path workspaceRoot = temporaryDirectory.resolve("workspaces");
    PdeRunContext context =
        new PdeRunContext(
            "produto-teste",
            "v1",
            customerReference,
            missionReference,
            workspaceRoot.resolve(customerReference + "-" + missionReference));
    if (existingThreadId == null) {
      return PdeAgentRunRequest.newThread(
          context,
          "gpt-test",
          customerReference,
          "prompt-v1",
          PdeHarnessTestSupport.validOutputSchema(),
          "schema-v1",
          false);
    }
    return PdeAgentRunRequest.resumeThread(
        context,
        "gpt-test",
        customerReference,
        "prompt-v1",
        PdeHarnessTestSupport.validOutputSchema(),
        "schema-v1",
        existingThreadId);
  }

  /** Cria a configuração sintética usando a raiz compartilhada pelos contextos do teste. */
  private PdeHarnessConfiguration configuration(String mode, Duration turnTimeout) {
    return PdeHarnessTestSupport.configuration(
        temporaryDirectory.resolve("codex-" + System.nanoTime()),
        temporaryDirectory.resolve("workspaces"),
        mode,
        turnTimeout);
  }
}
