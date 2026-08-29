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
import com.marketinghub.pde.harness.v1.internal.PdeHashing;
import com.marketinghub.pde.harness.v1.support.PdeHarnessTestSupport;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Homologa execução, memória e isolamento multicliente com App Server simulado. */
class PdeHarnessSdkTest {
  private static final Instant MEMORY_TIME = Instant.parse("2026-08-28T12:00:00Z");

  private final ObjectMapper mapper = new ObjectMapper();
  @TempDir Path temporaryDirectory;

  /** Consolida thread, memória, turno, saída, eventos, uso e hashes no caminho feliz. */
  @Test
  void executesCompleteAgentTurn() {
    PdeHarnessConfiguration configuration = configuration(null, Duration.ofSeconds(2));
    List<PdeHarnessEvent> observed = new CopyOnWriteArrayList<>();
    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration)) {
      PdeAgentRunResult result =
          sdk.execute(
              request("cliente-a", "conversa-a", "missao-1", null, emptyMemory("cliente-a")),
              observed::add);

      assertEquals(PdeRunStatus.COMPLETED, result.status());
      assertEquals("{\"message\":\"Resposta para cliente-a\"}", result.output());
      assertEquals("Resposta para cliente-a", result.structuredOutput().path("message").asText());
      assertTrue(result.threadId().startsWith("thread-"));
      assertEquals(result.threadId(), result.threadBinding().threadId());
      assertTrue(result.turnId().startsWith("turn-"));
      assertNotNull(result.tokenUsage());
      assertEquals(12, result.tokenUsage().path("inputTokens").asInt());
      assertFalse(result.events().isEmpty());
      assertEquals(result.events().size(), observed.size());
      assertEquals(0, result.memoryAudit().deliveredEntryCount());
      assertEquals(64, result.memoryAudit().snapshotSha256().length());
      assertEquals(64, result.promptSha256().length());
      assertEquals(64, result.effectiveInputSha256().length());
      assertEquals(64, result.outputSchemaSha256().length());
      assertEquals("gpt-test", result.model());
      assertEquals("prompt-v1", result.promptVersion());
      assertEquals("schema-v1", result.outputSchemaVersion());
      assertFalse(Files.exists(configuration.workspaceFor(result.context())));
      ((ObjectNode) result.structuredOutput()).put("message", "mutação");
      ((ObjectNode) result.tokenUsage()).put("inputTokens", 999);
      assertEquals("Resposta para cliente-a", result.structuredOutput().path("message").asText());
      assertEquals(12, result.tokenUsage().path("inputTokens").asInt());
    }
  }

  /** Copia a imagem validada ao workspace privado e envia somente esse caminho ao App Server. */
  @Test
  void executesMultimodalTurnWithMaterializedPrivateImage() throws Exception {
    byte[] png = syntheticPng();
    Path source = temporaryDirectory.resolve("foto-cliente.png");
    Files.write(source, png);
    PdeRunContext context =
        PdeHarnessTestSupport.context("cliente-a", "conversa-a", "missao-foto", "interacao-foto");
    PdeAgentRunRequest request =
        PdeAgentRunRequest.newThreadWithImages(
            context,
            emptyMemory("cliente-a"),
            "gpt-test",
            "Analise a foto autorizada.",
            "prompt-v1",
            PdeHarnessTestSupport.validOutputSchema(),
            "schema-v1",
            List.of(
                new PdeLocalImageInput("look-atual", source, "image/png", PdeHashing.sha256(png))),
            false);
    PdeHarnessConfiguration configuration = configuration("image-aware", Duration.ofSeconds(2));

    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration)) {
      PdeAgentRunResult result = sdk.execute(request);

      assertEquals("imagem-privada-copiada", result.structuredOutput().path("message").asText());
      assertTrue(Files.exists(source));
      assertFalse(Files.exists(configuration.workspaceFor(context)));
    }
  }

  /** Bloqueia hash divergente antes de iniciar o App Server ou expor a imagem ao turno. */
  @Test
  void rejectsImageWithDivergentAuditHash() throws Exception {
    byte[] png = syntheticPng();
    Path source = temporaryDirectory.resolve("foto-adulterada.png");
    Files.write(source, png);
    PdeAgentRunRequest request =
        PdeAgentRunRequest.newThreadWithImages(
            PdeHarnessTestSupport.context(
                "cliente-a", "conversa-a", "missao-foto", "interacao-hash"),
            emptyMemory("cliente-a"),
            "gpt-test",
            "Analise a foto autorizada.",
            "prompt-v1",
            PdeHarnessTestSupport.validOutputSchema(),
            "schema-v1",
            List.of(new PdeLocalImageInput("look-atual", source, "image/png", "0".repeat(64))),
            false);

    try (PdeHarnessSdk sdk = new PdeHarnessSdk(missingCommandConfiguration())) {
      PdeHarnessException error =
          assertThrows(PdeHarnessException.class, () -> sdk.execute(request));

      assertEquals(PdeHarnessFailureCategory.INPUT_INVALID, error.category());
      assertTrue(error.getMessage().contains("Hash da imagem"));
    }
  }

  /** Bloqueia arquivo que declara PNG mas não possui a assinatura binária desse formato. */
  @Test
  void rejectsImageWhoseContentDoesNotMatchDeclaredMediaType() throws Exception {
    byte[] content = "conteudo-que-nao-e-imagem".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    Path source = temporaryDirectory.resolve("arquivo-falso.png");
    Files.write(source, content);
    PdeAgentRunRequest request =
        PdeAgentRunRequest.newThreadWithImages(
            PdeHarnessTestSupport.context(
                "cliente-a", "conversa-a", "missao-foto", "interacao-tipo"),
            emptyMemory("cliente-a"),
            "gpt-test",
            "Analise a foto autorizada.",
            "prompt-v1",
            PdeHarnessTestSupport.validOutputSchema(),
            "schema-v1",
            List.of(
                new PdeLocalImageInput(
                    "look-atual", source, "image/png", PdeHashing.sha256(content))),
            false);

    try (PdeHarnessSdk sdk = new PdeHarnessSdk(missingCommandConfiguration())) {
      PdeHarnessException error =
          assertThrows(PdeHarnessException.class, () -> sdk.execute(request));

      assertEquals(PdeHarnessFailureCategory.INPUT_INVALID, error.category());
      assertTrue(error.getMessage().contains("tipo declarado"));
    }
  }

  /** Bloqueia mídia acima de quinze megabytes antes de iniciar qualquer processo externo. */
  @Test
  void rejectsImageLargerThanPrivateTransportLimit() throws Exception {
    Path source = temporaryDirectory.resolve("foto-grande.png");
    try (SeekableByteChannel channel =
        Files.newByteChannel(source, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
      channel.position(15L * 1024L * 1024L);
      channel.write(ByteBuffer.wrap(new byte[] {0x01}));
    }
    PdeAgentRunRequest request =
        PdeAgentRunRequest.newThreadWithImages(
            PdeHarnessTestSupport.context(
                "cliente-a", "conversa-a", "missao-foto", "interacao-tamanho"),
            emptyMemory("cliente-a"),
            "gpt-test",
            "Analise a foto autorizada.",
            "prompt-v1",
            PdeHarnessTestSupport.validOutputSchema(),
            "schema-v1",
            List.of(new PdeLocalImageInput("look-atual", source, "image/png", "0".repeat(64))),
            false);

    try (PdeHarnessSdk sdk = new PdeHarnessSdk(missingCommandConfiguration())) {
      PdeHarnessException error =
          assertThrows(PdeHarnessException.class, () -> sdk.execute(request));

      assertEquals(PdeHarnessFailureCategory.INPUT_INVALID, error.category());
      assertTrue(error.getMessage().contains("tamanho inválido"));
    }
  }

  /**
   * Bloqueia link simbólico para impedir que um caminho trocado exponha arquivo fora da interação.
   */
  @Test
  void rejectsSymbolicLinkAsPrivateImageSource() throws Exception {
    org.junit.jupiter.api.Assumptions.assumeTrue(
        FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
    byte[] png = syntheticPng();
    Path source = temporaryDirectory.resolve("foto-real.png");
    Path link = temporaryDirectory.resolve("foto-link.png");
    Files.write(source, png);
    Files.createSymbolicLink(link, source.getFileName());
    PdeAgentRunRequest request =
        PdeAgentRunRequest.newThreadWithImages(
            PdeHarnessTestSupport.context(
                "cliente-a", "conversa-a", "missao-foto", "interacao-link"),
            emptyMemory("cliente-a"),
            "gpt-test",
            "Analise a foto autorizada.",
            "prompt-v1",
            PdeHarnessTestSupport.validOutputSchema(),
            "schema-v1",
            List.of(
                new PdeLocalImageInput("look-atual", link, "image/png", PdeHashing.sha256(png))),
            false);

    try (PdeHarnessSdk sdk = new PdeHarnessSdk(missingCommandConfiguration())) {
      PdeHarnessException error =
          assertThrows(PdeHarnessException.class, () -> sdk.execute(request));

      assertEquals(PdeHarnessFailureCategory.INPUT_INVALID, error.category());
      assertTrue(error.getMessage().contains("arquivo regular"));
    }
  }

  /** Retoma a thread somente com o vínculo completo devolvido pela execução anterior. */
  @Test
  void resumesPersistedThread() {
    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration(null, Duration.ofSeconds(2)))) {
      PdeAgentRunResult first =
          sdk.execute(
              request("cliente-a", "conversa-a", "missao-1", null, emptyMemory("cliente-a")));
      PdeAgentRunResult resumed =
          sdk.execute(
              request(
                  "cliente-a",
                  "conversa-a",
                  "missao-2",
                  first.threadBinding(),
                  emptyMemory("cliente-a")));

      assertEquals(first.threadId(), resumed.threadId());
      assertNotEquals(first.turnId(), resumed.turnId());
      assertEquals(2, resumed.threadBinding().completedTurns());
    }
  }

  /** Recupera a memória canônica em uma thread nova durante um contato futuro. */
  @Test
  void remembersCustomerAcrossFutureContactWithoutOldThread() {
    PdeCustomerMemory memory =
        memory("cliente-a", 4, "Cliente recorrente", activePreference("cliente-a", "azul"));
    try (PdeHarnessSdk sdk =
        new PdeHarnessSdk(configuration("memory-aware", Duration.ofSeconds(2)))) {
      PdeAgentRunResult result =
          sdk.execute(request("cliente-a", "novo-contato", "missao-1", null, memory));

      assertTrue(result.output().contains("memória azul"));
      assertEquals(4, result.memoryAudit().memoryRevision());
      assertEquals(1, result.memoryAudit().deliveredEntryCount());
      assertEquals(4, result.threadBinding().memoryRevision());
    }
  }

  /** Recupera a memória do backend mesmo depois de reiniciar SDK e App Server. */
  @Test
  void remembersCustomerAfterSdkRestart() {
    try (PdeHarnessSdk firstSdk =
        new PdeHarnessSdk(configuration("memory-aware", Duration.ofSeconds(2)))) {
      PdeAgentRunResult firstContact =
          firstSdk.execute(
              request("cliente-a", "conversa-antiga", "missao-1", null, emptyMemory("cliente-a")));
      assertTrue(firstContact.output().contains("sem-memoria"));
    }

    PdeCustomerMemory promotedMemory =
        memory("cliente-a", 1, "Cliente recorrente", activePreference("cliente-a", "azul"));
    try (PdeHarnessSdk restartedSdk =
        new PdeHarnessSdk(configuration("memory-aware", Duration.ofSeconds(2)))) {
      PdeAgentRunResult futureContact =
          restartedSdk.execute(
              request("cliente-a", "conversa-nova", "missao-2", null, promotedMemory));

      assertTrue(futureContact.output().contains("memória azul"));
      assertEquals(1, futureContact.memoryAudit().memoryRevision());
      assertNotEquals(
          "conversa-antiga", futureContact.context().conversationScope().conversationReference());
    }
  }

  /** Mantém memória, threads, workspaces e saídas distintos entre dois clientes. */
  @Test
  void segregatesTwoCustomerExecutions() {
    PdeHarnessConfiguration configuration = configuration("memory-aware", Duration.ofSeconds(2));
    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration)) {
      PdeAgentRunResult customerA =
          sdk.execute(
              request(
                  "cliente-a",
                  "conversa-a",
                  "missao-1",
                  null,
                  memory("cliente-a", 1, "", activePreference("cliente-a", "azul"))));
      PdeAgentRunResult customerB =
          sdk.execute(
              request(
                  "cliente-b",
                  "conversa-b",
                  "missao-1",
                  null,
                  memory("cliente-b", 1, "", activePreference("cliente-b", "verde"))));

      assertNotEquals(customerA.threadId(), customerB.threadId());
      assertNotEquals(
          configuration.workspaceFor(customerA.context()),
          configuration.workspaceFor(customerB.context()));
      assertTrue(customerA.output().contains("azul"));
      assertFalse(customerA.output().contains("verde"));
      assertTrue(customerB.output().contains("verde"));
      assertFalse(customerB.output().contains("azul"));
    }
  }

  /** Mantém correlação correta quando clientes diferentes executam ao mesmo tempo. */
  @Test
  void segregatesConcurrentCustomerExecutions() throws Exception {
    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration(null, Duration.ofSeconds(2)));
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      CompletableFuture<PdeAgentRunResult> customerA =
          CompletableFuture.supplyAsync(
              () ->
                  sdk.execute(
                      request("cliente-a", "conversa-a", "missao", null, emptyMemory("cliente-a"))),
              executor);
      CompletableFuture<PdeAgentRunResult> customerB =
          CompletableFuture.supplyAsync(
              () ->
                  sdk.execute(
                      request("cliente-b", "conversa-b", "missao", null, emptyMemory("cliente-b"))),
              executor);

      PdeAgentRunResult resultA = customerA.get();
      PdeAgentRunResult resultB = customerB.get();
      assertNotEquals(resultA.threadId(), resultB.threadId());
      assertTrue(resultA.output().contains("cliente-a"));
      assertFalse(resultA.output().contains("cliente-b"));
      assertTrue(resultB.output().contains("cliente-b"));
      assertFalse(resultB.output().contains("cliente-a"));
    }
  }

  /** Bloqueia um segundo turno simultâneo para a mesma conversa. */
  @Test
  void rejectsConcurrentTurnsForSameConversation() throws Exception {
    CountDownLatch threadReady = new CountDownLatch(1);
    try (PdeHarnessSdk sdk =
            new PdeHarnessSdk(configuration("slow-completion", Duration.ofSeconds(2)));
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      PdeAgentRunRequest firstRequest =
          request("cliente-a", "conversa-a", "missao", null, emptyMemory("cliente-a"));
      CompletableFuture<PdeAgentRunResult> first =
          CompletableFuture.supplyAsync(
              () ->
                  sdk.execute(
                      firstRequest,
                      event -> {
                        if ("pde/thread/ready".equals(event.method())) {
                          threadReady.countDown();
                        }
                      }),
              executor);
      assertTrue(threadReady.await(1, TimeUnit.SECONDS));

      PdeHarnessException error =
          assertThrows(
              PdeHarnessException.class,
              () ->
                  sdk.execute(
                      request(
                          "cliente-a", "conversa-a", "missao", null, emptyMemory("cliente-a"))));

      assertEquals(PdeHarnessFailureCategory.CONVERSATION_BUSY, error.category());
      assertEquals(PdeRunStatus.COMPLETED, first.get(1, TimeUnit.SECONDS).status());
    }
  }

  /** Rejeita memória de outro cliente antes de iniciar um comando externo inexistente. */
  @Test
  void rejectsCrossCustomerMemoryBeforeStartingAppServer() {
    PdeHarnessConfiguration configuration = missingCommandConfiguration();
    PdeAgentRunRequest request =
        request("cliente-a", "conversa-a", "missao", null, emptyMemory("cliente-b"));

    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration)) {
      PdeHarnessException error =
          assertThrows(PdeHarnessException.class, () -> sdk.execute(request));
      assertEquals(PdeHarnessFailureCategory.ISOLATION_VIOLATION, error.category());
    }
  }

  /** Rejeita vínculo legítimo de outro cliente antes de carregar sua thread. */
  @Test
  void rejectsCrossCustomerThreadBinding() {
    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration(null, Duration.ofSeconds(2)))) {
      PdeAgentRunResult customerA =
          sdk.execute(request("cliente-a", "conversa-a", "missao", null, emptyMemory("cliente-a")));
      PdeAgentRunRequest customerB =
          request(
              "cliente-b",
              "conversa-b",
              "missao",
              customerA.threadBinding(),
              emptyMemory("cliente-b"));

      PdeHarnessException error =
          assertThrows(PdeHarnessException.class, () -> sdk.execute(customerB));
      assertEquals(PdeHarnessFailureCategory.ISOLATION_VIOLATION, error.category());
    }
  }

  /** Detecta vínculo forjado quando a thread já pertence a outro escopo no processo. */
  @Test
  void rejectsRuntimeReuseOfThreadByAnotherCustomer() {
    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration(null, Duration.ofSeconds(2)))) {
      PdeAgentRunResult customerA =
          sdk.execute(request("cliente-a", "conversa-a", "missao", null, emptyMemory("cliente-a")));
      PdeConversationScope customerBScope =
          PdeHarnessTestSupport.conversationScope("cliente-b", "conversa-b");
      PdeThreadBinding forged =
          new PdeThreadBinding(
              customerA.threadId(),
              customerBScope.fingerprint(),
              0,
              1,
              false,
              customerA.threadBinding().createdAt(),
              customerA.threadBinding().lastUsedAt());

      PdeHarnessException error =
          assertThrows(
              PdeHarnessException.class,
              () ->
                  sdk.execute(
                      request(
                          "cliente-b", "conversa-b", "missao", forged, emptyMemory("cliente-b"))));

      assertEquals(PdeHarnessFailureCategory.ISOLATION_VIOLATION, error.category());
    }
  }

  /** Rejeita snapshot anterior à última revisão já apresentada à thread. */
  @Test
  void rejectsRegressiveMemoryRevision() {
    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration(null, Duration.ofSeconds(2)))) {
      PdeAgentRunResult first =
          sdk.execute(
              request(
                  "cliente-a",
                  "conversa-a",
                  "missao-1",
                  null,
                  memory("cliente-a", 3, "", activePreference("cliente-a", "azul"))));

      PdeHarnessException error =
          assertThrows(
              PdeHarnessException.class,
              () ->
                  sdk.execute(
                      request(
                          "cliente-a",
                          "conversa-a",
                          "missao-2",
                          first.threadBinding(),
                          memory("cliente-a", 2, "", activePreference("cliente-a", "azul")))));

      assertEquals(PdeHarnessFailureCategory.MEMORY_CONFLICT, error.category());
    }
  }

  /** Não entrega um fato cuja validade terminou antes do contato atual. */
  @Test
  void omitsExpiredMemoryFromFutureContact() {
    PdeMemoryEntry expired =
        new PdeMemoryEntry(
            PdeHarnessTestSupport.customerScope("cliente-a"),
            "preferencia-expirada",
            PdeMemoryCategory.PREFERENCE,
            "prefere azul",
            PdeMemorySource.USER_STATED,
            "interacao-antiga",
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2025-02-01T00:00:00Z"),
            1.0d);
    try (PdeHarnessSdk sdk =
        new PdeHarnessSdk(configuration("memory-aware", Duration.ofSeconds(2)))) {
      PdeAgentRunResult result =
          sdk.execute(
              request(
                  "cliente-a",
                  "contato-futuro",
                  "missao",
                  null,
                  memory("cliente-a", 2, "", List.of(expired))));

      assertTrue(result.output().contains("sem-memoria"));
      assertEquals(0, result.memoryAudit().deliveredEntryCount());
    }
  }

  /** Exclui a thread correta e bloqueia tentativa de esquecimento com outro escopo. */
  @Test
  void forgetsOnlyThreadBoundToAuthorizedConversation() {
    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration(null, Duration.ofSeconds(2)))) {
      PdeAgentRunResult result =
          sdk.execute(request("cliente-a", "conversa-a", "missao", null, emptyMemory("cliente-a")));
      PdeConversationScope wrongScope =
          PdeHarnessTestSupport.conversationScope("cliente-b", "conversa-b");

      PdeHarnessException error =
          assertThrows(
              PdeHarnessException.class,
              () -> sdk.forgetThread(wrongScope, result.threadBinding()));
      assertEquals(PdeHarnessFailureCategory.ISOLATION_VIOLATION, error.category());

      sdk.forgetThread(result.context().conversationScope(), result.threadBinding());
    }
  }

  /** Valida schema antes de tentar iniciar um comando externo inexistente. */
  @Test
  void rejectsInvalidSchemaBeforeStartingAppServer() throws Exception {
    PdeHarnessConfiguration configuration = missingCommandConfiguration();
    JsonNode invalidSchema =
        mapper.readTree("{\"type\":\"array\",\"items\":{\"type\":\"string\"}}");
    PdeAgentRunRequest request =
        PdeAgentRunRequest.newThread(
            PdeHarnessTestSupport.context(
                "cliente-a", "conversa-a", "missao-schema", "interacao-schema"),
            emptyMemory("cliente-a"),
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
              () ->
                  sdk.execute(
                      request(
                          "cliente-a",
                          "conversa-a",
                          "missao-timeout",
                          null,
                          emptyMemory("cliente-a"))));

      assertEquals(PdeHarnessFailureCategory.TIMEOUT, error.category());
    }
  }

  /** Converte falha objetiva de login em bloqueio recuperável, nunca em sucesso. */
  @Test
  void mapsAuthenticationFailureToBlockedResult() {
    try (PdeHarnessSdk sdk =
        new PdeHarnessSdk(configuration("authentication-failure", Duration.ofSeconds(2)))) {
      PdeAgentRunResult result =
          sdk.execute(
              request("cliente-a", "conversa-a", "missao-auth", null, emptyMemory("cliente-a")));

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
              () ->
                  sdk.execute(
                      request(
                          "cliente-a",
                          "conversa-a",
                          "missao-json-invalido",
                          null,
                          emptyMemory("cliente-a"))));

      assertEquals(PdeHarnessFailureCategory.EXECUTION_FAILED, error.category());
    }
  }

  /** Descarta também os dados preparados no workspace quando a saída do modelo é inválida. */
  @Test
  void discardsWorkspaceAfterFailedExecution() throws Exception {
    PdeHarnessConfiguration configuration = configuration("invalid-output", Duration.ofSeconds(2));
    PdeAgentRunRequest request =
        request("cliente-a", "conversa-a", "missao-com-falha", null, emptyMemory("cliente-a"));
    Path workspace = configuration.workspaceFor(request.context());
    Files.createDirectories(workspace);
    Files.writeString(workspace.resolve("contexto-autorizado.txt"), "dado sintético");

    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration)) {
      assertThrows(PdeHarnessException.class, () -> sdk.execute(request));
    }

    assertFalse(Files.exists(workspace));
  }

  /** Rejeita JSON válido que contradiga o schema versionado da tarefa. */
  @Test
  void rejectsOutputThatViolatesSchema() {
    try (PdeHarnessSdk sdk =
        new PdeHarnessSdk(configuration("schema-mismatch", Duration.ofSeconds(2)))) {
      PdeHarnessException error =
          assertThrows(
              PdeHarnessException.class,
              () ->
                  sdk.execute(
                      request(
                          "cliente-a",
                          "conversa-a",
                          "missao-schema-divergente",
                          null,
                          emptyMemory("cliente-a"))));

      assertEquals(PdeHarnessFailureCategory.EXECUTION_FAILED, error.category());
    }
  }

  /** Monta uma request nova ou retomada usando somente identificadores sintéticos. */
  private PdeAgentRunRequest request(
      String customerReference,
      String conversationReference,
      String missionReference,
      PdeThreadBinding existingThreadBinding,
      PdeCustomerMemory memory) {
    PdeRunContext context =
        PdeHarnessTestSupport.context(
            customerReference,
            conversationReference,
            missionReference,
            "interacao-" + System.nanoTime());
    if (existingThreadBinding == null) {
      return PdeAgentRunRequest.newThread(
          context,
          memory,
          "gpt-test",
          customerReference,
          "prompt-v1",
          PdeHarnessTestSupport.validOutputSchema(),
          "schema-v1",
          false);
    }
    return PdeAgentRunRequest.resumeThread(
        context,
        memory,
        "gpt-test",
        customerReference,
        "prompt-v1",
        PdeHarnessTestSupport.validOutputSchema(),
        "schema-v1",
        existingThreadBinding);
  }

  /** Cria um fato ativo de preferência para provar personalização entre contatos. */
  private List<PdeMemoryEntry> activePreference(String customerReference, String color) {
    return List.of(
        new PdeMemoryEntry(
            PdeHarnessTestSupport.customerScope(customerReference),
            "preferencia-cor",
            PdeMemoryCategory.PREFERENCE,
            "prefere " + color,
            PdeMemorySource.USER_STATED,
            "interacao-origem",
            MEMORY_TIME,
            null,
            1.0d));
  }

  /** Cria um snapshot vazio com o escopo correto do cliente. */
  private PdeCustomerMemory emptyMemory(String customerReference) {
    return PdeHarnessTestSupport.emptyMemory(customerReference);
  }

  /** Cria um snapshot versionado com os fatos sintéticos informados. */
  private PdeCustomerMemory memory(
      String customerReference,
      long revision,
      String relationshipSummary,
      List<PdeMemoryEntry> entries) {
    return PdeHarnessTestSupport.memory(customerReference, revision, relationshipSummary, entries);
  }

  /** Cria a configuração sintética usando uma raiz comum de workspaces derivados. */
  private PdeHarnessConfiguration configuration(String mode, Duration turnTimeout) {
    return PdeHarnessTestSupport.configuration(
        temporaryDirectory.resolve("codex-" + System.nanoTime()),
        temporaryDirectory.resolve("workspaces"),
        mode,
        turnTimeout);
  }

  /** Cria configuração cujo comando inexistente prova a ordem das validações locais. */
  private PdeHarnessConfiguration missingCommandConfiguration() {
    return new PdeHarnessConfiguration(
        temporaryDirectory.resolve("codex-inexistente").toString(),
        List.of("app-server"),
        temporaryDirectory.resolve("codex-home-inexistente"),
        temporaryDirectory.resolve("workspaces-inexistentes"),
        Duration.ofSeconds(1),
        Duration.ofSeconds(1),
        "0.149.0",
        "test",
        "Test",
        "test",
        Map.of(),
        false);
  }

  /** Gera bytes mínimos com assinatura PNG para testar o transporte sem usar imagem real. */
  private byte[] syntheticPng() {
    return new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01, 0x02, 0x03};
  }
}
