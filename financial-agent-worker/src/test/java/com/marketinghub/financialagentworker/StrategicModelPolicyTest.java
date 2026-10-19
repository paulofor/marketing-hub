package com.marketinghub.financialagentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Protege a escolha de modelo econômico sem elevar consultas e conciliações de rotina. */
class StrategicModelPolicyTest {
  /** Confere autoridade, comando e auditoria contra a mesma seleção de modelo. */
  @ParameterizedTest
  @CsvSource({
    "READ_ONLY_REVENUE_PROJECTION,gpt-6-astra",
    "COMMERCIAL_ASSUMPTIONS_VALIDATION,gpt-6-astra",
    "READ_ONLY_FINANCIAL_RECONCILIATION,operational-test"
  })
  void selectsByAuthority(String authority, String expected) {
    var properties = new FinancialAgentProperties();
    properties.setModel("operational-test");
    properties.setRepositoryPath("/tmp/local-only");
    properties.setCodexCommand("codex");
    String selected = properties.modelForAuthority(authority);
    assertThat(selected).isEqualTo(expected);
    var runner = new FinancialCodexRunner(properties, new ObjectMapper());
    assertThat(runner.buildCommand(Path.of("out"), Path.of("schema"), Path.of("mcp"), selected))
        .containsSubsequence("--model", expected);
    var prompt =
        new FinancialCodexRunner.PromptComposition("agente\natividade", "agente", "atividade");
    assertThat(runner.executionAudit(prompt, selected)).containsEntry("modelCode", expected);
    assertThat(runner.buildCommand(Path.of("out"), Path.of("schema")))
        .containsSubsequence("--model", "operational-test");
  }

  /** Recusa configuração incompleta em vez de simular a adoção de Astra. */
  @Test
  void requiresStrategicModel() {
    var properties = new FinancialAgentProperties();
    properties.setStrategicModel(null);
    assertThatThrownBy(() -> properties.modelForAuthority("COMMERCIAL_ASSUMPTIONS_VALIDATION"))
        .hasMessageContaining("CODEX_STRATEGIC_MODEL");
  }
}
