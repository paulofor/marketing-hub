package com.marketinghub.financialagentworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Responsabilidade: exercitar o servidor MCP real com fontes locais e operações segregadas. */
class FinancialMcpContractTest {

  /** Inclui na suíte Java a prova de contexto, anotações e consultas reais de duas execuções. */
  @Test
  void validatesRealReadOnlyQueriesWithoutExternalConsumption() throws Exception {
    Path report = Path.of("target", "financial-mcp-contract.log");
    var process =
        new ProcessBuilder("node", "--test", "src/test/mcp/financial-agent.test.mjs")
            .redirectErrorStream(true)
            .redirectOutput(report.toFile())
            .start();
    try {
      assertThat(process.waitFor(Duration.ofSeconds(30).toMillis(), TimeUnit.MILLISECONDS))
          .as("O contrato MCP deve terminar sem espera indefinida")
          .isTrue();
      var output = java.nio.file.Files.readString(report, StandardCharsets.UTF_8);
      assertThat(process.exitValue()).as(output).isZero();
      assertThat(output).contains("# pass 2", "# fail 0");
    } finally {
      process.destroyForcibly();
    }
  }
}
