package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: validar o isolamento e os limites da análise observacional por Codex. */
class CodexObservationAnalyzerTest {
  @TempDir java.nio.file.Path temporaryDirectory;

  /** Confirma que o JSON final é lido do arquivo dedicado, sem misturar logs do processo. */
  @Test
  void shouldReadOnlyLastMessage() throws Exception {
    var executable = temporaryDirectory.resolve("codex-ok.sh");
    Files.writeString(
        executable,
        "#!/bin/sh\nwhile [ \"$1\" != \"\" ]; do if [ \"$1\" = \"--output-last-message\" ]; then shift; out=$1; fi; shift; done\nprintf diagnostics\nprintf '{\"observation\":{}}' > \"$out\"\n");
    executable.toFile().setExecutable(true);
    var analyzer =
        new CodexObservationAnalyzer(
            executable.toString(), "test-model", "test-schema", Duration.ofSeconds(2));

    assertThat(analyzer.analyze("prompt", temporaryDirectory)).isEqualTo("{\"observation\":{}}");
  }

  /** Confirma que processos bloqueados são encerrados pelo limite configurado. */
  @Test
  void shouldStopBlockedProcess() throws Exception {
    var executable = temporaryDirectory.resolve("codex-timeout.sh");
    Files.writeString(executable, "#!/bin/sh\nsleep 5\n");
    executable.toFile().setExecutable(true);
    var analyzer =
        new CodexObservationAnalyzer(
            executable.toString(), "test-model", "test-schema", Duration.ofMillis(100));

    assertThatThrownBy(() -> analyzer.analyze("prompt", temporaryDirectory))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Timeout da análise observacional");
  }

  /** Confirma que todos os objetos do schema cumprem o contrato estrito do provedor. */
  @Test
  void shouldKeepEverySchemaObjectStrict() throws Exception {
    JsonNode schema =
        new ObjectMapper()
            .readTree(
                java.nio.file.Path.of(
                        "src/main/resources/prompts/customer-agent/v1/digital-observation-schema.json")
                    .toFile());

    assertStrictObjects(schema);
  }

  /** Percorre recursivamente o schema e exige fechamento explícito de cada objeto. */
  private void assertStrictObjects(JsonNode node) {
    if (node.isObject() && "object".equals(node.path("type").asText())) {
      assertThat(node.path("additionalProperties").asBoolean()).isFalse();
    }
    node.elements().forEachRemaining(this::assertStrictObjects);
  }
}
