package com.marketinghub.pde.harness.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.pde.harness.v1.support.PdeHarnessTestSupport;
import java.nio.file.Path;
import java.time.Duration;
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
        new PdeRunContext(
            "produto", "v1", "cliente", "missao", temporaryDirectory.resolve("workspaces/cliente"));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PdeAgentRunRequest(
                context,
                "gpt-test",
                "prompt",
                "v1",
                PdeHarnessTestSupport.validOutputSchema(),
                "v1",
                "thread-1",
                true));
  }

  /** Impede que o chamador altere o schema armazenado depois de criar a request. */
  @Test
  void protectsRequestSchemaFromExternalMutation() {
    PdeRunContext context =
        new PdeRunContext(
            "produto", "v1", "cliente", "missao", temporaryDirectory.resolve("workspaces/cliente"));
    PdeAgentRunRequest request =
        PdeAgentRunRequest.newThread(
            context,
            "gpt-test",
            "prompt",
            "v1",
            PdeHarnessTestSupport.validOutputSchema(),
            "v1",
            false);

    ((ObjectNode) request.outputSchema()).put("campoMutante", true);

    assertFalse(request.outputSchema().has("campoMutante"));
  }
}
