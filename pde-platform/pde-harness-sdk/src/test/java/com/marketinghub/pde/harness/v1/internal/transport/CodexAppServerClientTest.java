package com.marketinghub.pde.harness.v1.internal.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.harness.v1.PdeHarnessConfiguration;
import com.marketinghub.pde.harness.v1.PdeHarnessException;
import com.marketinghub.pde.harness.v1.support.PdeHarnessTestSupport;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Homologa o transporte JSONL, a correlação e a recusa segura de requests reversas. */
class CodexAppServerClientTest {
  private final ObjectMapper mapper = new ObjectMapper();
  @TempDir Path temporaryDirectory;

  /** Executa initialize antes de aceitar requests operacionais. */
  @Test
  void initializesAndAnswersRequests() {
    PdeHarnessConfiguration configuration = configuration(null);
    try (CodexAppServerClient client = new CodexAppServerClient(configuration, mapper)) {
      JsonNode initialized = client.start();
      JsonNode environment = client.request("test/environment", mapper.createObjectNode());

      assertTrue(client.isReady());
      assertEquals(configuration.codexHome().toString(), initialized.path("codexHome").asText());
      assertFalse(environment.path("openAiApiKeyPresent").asBoolean());
      assertFalse(environment.path("openAiApiKeyFilePresent").asBoolean());
    }
  }

  /** Correlaciona respostas pelo id mesmo quando chegam na ordem inversa. */
  @Test
  void correlatesOutOfOrderResponses() throws Exception {
    try (CodexAppServerClient client = new CodexAppServerClient(configuration(null), mapper)) {
      client.start();
      CompletableFuture<JsonNode> first =
          CompletableFuture.supplyAsync(
              () -> client.request("test/outOfOrderA", mapper.createObjectNode()));
      CompletableFuture<JsonNode> second =
          CompletableFuture.supplyAsync(
              () -> client.request("test/outOfOrderB", mapper.createObjectNode()));

      assertEquals("test/outOfOrderB", second.get(1, TimeUnit.SECONDS).path("method").asText());
      assertEquals("test/outOfOrderA", first.get(1, TimeUnit.SECONDS).path("method").asText());
      assertEquals(0, client.pendingRequestCount());
    }
  }

  /** Responde com erro a uma aprovação reversa que o backend não autorizou. */
  @Test
  void refusesUnknownServerRequest() throws Exception {
    try (CodexAppServerClient client = new CodexAppServerClient(configuration(null), mapper)) {
      CompletableFuture<JsonNode> denied = new CompletableFuture<>();
      client.addNotificationListener(
          notification -> {
            if ("test/serverRequestDenied".equals(notification.method())) {
              denied.complete(notification.params());
            }
          });
      client.start();
      client.request("test/requestServerApproval", mapper.createObjectNode());

      assertTrue(denied.get(1, TimeUnit.SECONDS).path("denied").asBoolean());
    }
  }

  /** Rejeita request pendente quando o processo encerra sem response. */
  @Test
  void rejectsPendingRequestAfterUnexpectedExit() {
    try (CodexAppServerClient client = new CodexAppServerClient(configuration(null), mapper)) {
      client.start();

      assertThrows(
          PdeHarnessException.class, () -> client.request("test/exit", mapper.createObjectNode()));
    }
  }

  /** Encerra a conexão e classifica uma linha não JSON como incompatibilidade de protocolo. */
  @Test
  void rejectsMalformedProtocolLine() {
    try (CodexAppServerClient client =
        new CodexAppServerClient(configuration("malformed-json"), mapper)) {
      client.start();

      PdeHarnessException error =
          assertThrows(
              PdeHarnessException.class,
              () -> client.request("test/environment", mapper.createObjectNode()));
      assertEquals(
          com.marketinghub.pde.harness.v1.PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
          error.category());
    }
  }

  /** Classifica comando inexistente como App Server indisponível com causa preservada. */
  @Test
  void rejectsMissingAppServerCommand() {
    PdeHarnessConfiguration base = configuration(null);
    PdeHarnessConfiguration missing =
        new PdeHarnessConfiguration(
            temporaryDirectory.resolve("binario-inexistente").toString(),
            base.codexArguments(),
            base.codexHome(),
            base.workspaceRoot(),
            base.requestTimeout(),
            base.turnTimeout(),
            base.expectedCodexVersion(),
            base.clientName(),
            base.clientTitle(),
            base.sdkVersion(),
            base.environmentOverrides(),
            false);

    try (CodexAppServerClient client = new CodexAppServerClient(missing, mapper)) {
      PdeHarnessException error = assertThrows(PdeHarnessException.class, client::start);
      assertEquals(
          com.marketinghub.pde.harness.v1.PdeHarnessFailureCategory.APP_SERVER_UNAVAILABLE,
          error.category());
    }
  }

  /** Cria uma configuração isolada para cada processo sintético. */
  private PdeHarnessConfiguration configuration(String mode) {
    return PdeHarnessTestSupport.configuration(
        temporaryDirectory.resolve("codex-" + System.nanoTime()),
        temporaryDirectory.resolve("workspaces-" + System.nanoTime()),
        mode,
        Duration.ofSeconds(2));
  }
}
