package com.marketinghub.financialagentworker;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger o contrato de seguranca do executor financeiro. */
class FinancialCodexRunnerTest {
  /** Confirma pesquisa web, sandbox somente leitura e modelo configurado no comando Codex. */
  @Test
  void deveExecutarSomenteLeitura() {
    FinancialAgentProperties properties = new FinancialAgentProperties();
    properties.setCodexCommand("codex");
    properties.setRepositoryPath("/workspace/marketing-hub");
    properties.setModel("gpt-5.6-sol");
    FinancialCodexRunner runner = new FinancialCodexRunner(properties, new ObjectMapper());

    var command = runner.buildCommand(Path.of("/tmp/out"), Path.of("/tmp/schema"));

    assertThat(command).containsSubsequence("codex", "--search", "exec", "-");
    assertThat(command).containsSubsequence("--sandbox", "read-only");
    assertThat(command).containsSubsequence("--model", "gpt-5.6-sol");
    assertThat(command).doesNotContain("--dangerously-bypass-approvals-and-sandbox");
  }

  /** Confirma o limite operacional padrão de quarenta minutos. */
  @Test
  void deveLimitarExecucaoCodexAQuarentaMinutos() {
    FinancialAgentProperties properties = new FinancialAgentProperties();

    assertThat(properties.getCodexTimeout()).isEqualTo(Duration.ofMinutes(40));
  }
}
