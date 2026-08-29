package com.marketinghub.pde.harness.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.pde.harness.v1.support.PdeHarnessTestSupport;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Protege a configuração canônica contra credencial de API e contrato inconsistente. */
class PdeHarnessConfigurationTest {
  @TempDir Path temporaryDirectory;

  /** Confirma os defaults fixados para Java, stdio e Codex 0.149.0. */
  @Test
  void createsStandardJavaConfiguration() {
    PdeHarnessConfiguration configuration =
        PdeHarnessConfiguration.standard(
            temporaryDirectory.resolve("codex"), temporaryDirectory.resolve("workspaces"));

    assertEquals("codex", configuration.codexCommand());
    assertEquals(List.of("app-server", "--listen", "stdio://"), configuration.codexArguments());
    assertEquals("0.149.0", configuration.expectedCodexVersion());
    assertEquals("0.3.0", configuration.sdkVersion());
  }

  /** Rejeita chave direta mesmo quando ela aparece apenas como override do processo filho. */
  @Test
  void rejectsOpenAiApiKeyOverride() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PdeHarnessConfiguration(
                "codex",
                List.of("app-server"),
                temporaryDirectory.resolve("codex"),
                temporaryDirectory.resolve("workspaces"),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                "0.149.0",
                "test",
                "Test",
                "test",
                Map.of("OPENAI_API_KEY", "proibida"),
                false));
  }

  /** Impede retomar uma thread declarada como efêmera. */
  @Test
  void rejectsResumeOfEphemeralThread() {
    PdeRunContext context =
        PdeHarnessTestSupport.context("cliente", "conversa", "missao", "interacao");
    Instant now = Instant.parse("2026-08-28T12:00:00Z");
    PdeThreadBinding binding =
        new PdeThreadBinding(
            "thread-1", context.conversationScope().fingerprint(), 0, 1, true, now, now);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PdeAgentRunRequest(
                context,
                PdeHarnessTestSupport.emptyMemory("cliente"),
                "gpt-test",
                "prompt",
                "v1",
                PdeHarnessTestSupport.validOutputSchema(),
                "v1",
                binding,
                true));
  }

  /** Impede que o chamador altere o schema armazenado depois de criar a request. */
  @Test
  void protectsRequestSchemaFromExternalMutation() {
    PdeRunContext context =
        PdeHarnessTestSupport.context("cliente", "conversa", "missao", "interacao");
    PdeAgentRunRequest request =
        PdeAgentRunRequest.newThread(
            context,
            PdeHarnessTestSupport.emptyMemory("cliente"),
            "gpt-test",
            "prompt",
            "v1",
            PdeHarnessTestSupport.validOutputSchema(),
            "v1",
            false);

    ((ObjectNode) request.outputSchema()).put("campoMutante", true);

    assertFalse(request.outputSchema().has("campoMutante"));
  }

  /** Deriva workspaces distintos sem inserir identificadores pessoais nos caminhos. */
  @Test
  void derivesPrivateWorkspaceForEachConversation() {
    PdeHarnessConfiguration configuration =
        PdeHarnessConfiguration.standard(
            temporaryDirectory.resolve("codex"), temporaryDirectory.resolve("workspaces"));
    Path customerA =
        configuration.workspaceFor(
            PdeHarnessTestSupport.context("cliente-a", "conversa-a", "missao", "interacao"));
    Path customerB =
        configuration.workspaceFor(
            PdeHarnessTestSupport.context("cliente-b", "conversa-b", "missao", "interacao"));

    assertNotEquals(customerA, customerB);
    assertFalse(customerA.toString().contains("cliente-a"));
    assertFalse(customerB.toString().contains("cliente-b"));
  }
}
