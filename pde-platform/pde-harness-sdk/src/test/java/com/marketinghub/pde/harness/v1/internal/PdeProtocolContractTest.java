package com.marketinghub.pde.harness.v1.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.harness.v1.PdeHarnessConfiguration;
import com.marketinghub.pde.harness.v1.PdeHarnessException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Comprova versão e integridade do bundle oficial embarcado no SDK. */
class PdeProtocolContractTest {
  @TempDir Path temporaryDirectory;

  /** Carrega o bundle cujo SHA-256 pertence ao Codex 0.149.0. */
  @Test
  void validatesEmbeddedProtocolBundle() {
    PdeProtocolContract contract = new PdeProtocolContract(new ObjectMapper());

    assertEquals("0.149.0", contract.codexVersion());
    assertEquals(
        "02a4c63a638fdae4a5f6c3ad32a41a377b642c66f3abc84f6fc47c7f3d6074df",
        contract.manifest().schemaSha256());
  }

  /** Bloqueia configuração que tente usar outra versão com o bundle antigo. */
  @Test
  void rejectsCodexVersionDrift() {
    PdeProtocolContract contract = new PdeProtocolContract(new ObjectMapper());
    PdeHarnessConfiguration configuration =
        new PdeHarnessConfiguration(
            "codex",
            List.of("app-server"),
            temporaryDirectory.resolve("codex"),
            temporaryDirectory.resolve("workspaces"),
            Duration.ofSeconds(1),
            Duration.ofSeconds(1),
            "9.9.9",
            "test",
            "Test",
            "test",
            Map.of(),
            false);

    assertThrows(PdeHarnessException.class, () -> contract.verifyConfiguration(configuration));
  }

  /** Confirma que o bundle fixado contém todas as operações usadas pela fachada Java v1. */
  @Test
  void containsFacadeMethodsAndNotifications() throws Exception {
    JsonNode bundle;
    try (var input =
        getClass()
            .getResourceAsStream(
                "/codex-app-server/0.149.0/codex_app_server_protocol.schemas.json")) {
      bundle = new ObjectMapper().readTree(input);
    }
    Set<String> clientMethods =
        collectMethods(bundle.path("definitions").path("ClientRequest").path("oneOf"));
    Set<String> serverNotifications =
        collectMethods(bundle.path("definitions").path("ServerNotification").path("oneOf"));

    assertEquals(
        Set.of("thread/start", "thread/resume", "turn/start", "turn/interrupt"),
        intersection(
            clientMethods,
            Set.of("thread/start", "thread/resume", "turn/start", "turn/interrupt")));
    assertEquals(
        Set.of(
            "item/agentMessage/delta",
            "item/completed",
            "thread/tokenUsage/updated",
            "turn/completed"),
        intersection(
            serverNotifications,
            Set.of(
                "item/agentMessage/delta",
                "item/completed",
                "thread/tokenUsage/updated",
                "turn/completed")));
  }

  /** Extrai os literais `method` de cada variante do bundle oficial. */
  private Set<String> collectMethods(JsonNode variants) {
    Set<String> methods = new HashSet<>();
    variants.forEach(
        variant ->
            variant
                .path("properties")
                .path("method")
                .path("enum")
                .forEach(value -> methods.add(value.asText())));
    return methods;
  }

  /** Retorna somente os métodos relevantes para uma asserção de fachada. */
  private Set<String> intersection(Set<String> actual, Set<String> expected) {
    Set<String> result = new HashSet<>(actual);
    result.retainAll(expected);
    return result;
  }
}
