package com.marketinghub.pde.harness.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/** Valida o handshake real com a versão local fixada sem abrir turno ou consumir modelo. */
@EnabledIfSystemProperty(named = "pde.codex.it", matches = "true")
class CodexAppServerHandshakeIT {
  @TempDir Path temporaryDirectory;

  /** Executa somente `initialize`/`initialized` contra o Codex App Server real. */
  @Test
  void initializesPinnedRealCodexAppServer() {
    PdeHarnessConfiguration configuration =
        PdeHarnessConfiguration.standard(
            temporaryDirectory.resolve("codex-home"), temporaryDirectory.resolve("workspaces"));

    try (PdeHarnessSdk sdk = new PdeHarnessSdk(configuration)) {
      PdeHarnessHealth health = sdk.start();

      assertTrue(health.ready());
      assertEquals("0.149.0", health.codexVersion());
      assertEquals(configuration.codexHome().toAbsolutePath().normalize(), health.codexHome());
    }
  }
}
