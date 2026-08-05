package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: proteger o contrato de navegação pública e evidência mobile. */
class BrowserObservationRunnerTest {
  @TempDir Path temporaryDirectory;

  /** Confirma que fatos e screenshots produzidos pelo navegador são preservados. */
  @Test
  void shouldReturnBrowserFactsAndEvidence() throws Exception {
    Path script = temporaryDirectory.resolve("fake-browser.sh");
    Files.writeString(
        script,
        "mkdir -p \"$3\"\n"
            + "printf evidence > \"$3/page-1.png\"\n"
            + "printf '%s' '{\"deviceProfile\":\"IPHONE_15_PRO\",\"pages\":[]}' > \"$2\"\n");
    var runner = new BrowserObservationRunner(new ObjectMapper(), "/bin/sh", script.toString());

    var observation =
        runner.observe("[\"https://example.com/page\"]", temporaryDirectory.resolve("work"));

    assertThat(observation.facts()).containsEntry("deviceProfile", "IPHONE_15_PRO");
    assertThat(observation.screenshots()).hasSize(1);
  }

  /** Confirma que rede privada não pode ser usada para sondagem pelo worker. */
  @Test
  void shouldBlockPrivateSourcesBeforeStartingBrowser() {
    var runner = new BrowserObservationRunner(new ObjectMapper(), "node", "unused");

    assertThatThrownBy(
            () ->
                runner.observe(
                    "[\"http://127.0.0.1/private\"]", temporaryDirectory.resolve("blocked")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("privada");
  }
}
