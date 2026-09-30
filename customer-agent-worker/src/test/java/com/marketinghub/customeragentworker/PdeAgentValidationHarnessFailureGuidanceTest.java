package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar a separação entre falha do executor e defeito do protótipo. */
class PdeAgentValidationHarnessFailureGuidanceTest {

  /** Mantém a mesma versão quando catálogo, configuração ou integração do harness falha. */
  @Test
  void classifiesExecutorFailuresWithoutRequestingProductCorrection() {
    var unsupported =
        PdeAgentValidationHarnessConsumer.failureGuidance(
            PdeAgentValidationHarnessRunner.HarnessException.executor(
                "O harness instalado não possui cenários próprios para este produto."));
    var integration =
        PdeAgentValidationHarnessConsumer.failureGuidance(
            new IOException("O processo do navegador não iniciou."));

    assertThat(unsupported.get("category")).isEqualTo("EXECUTOR_FAILURE");
    assertThat(integration.get("category")).isEqualTo("EXECUTOR_FAILURE");
    assertThat(unsupported.get("recommendedAction").toString())
        .contains("mesma homologação", "sem criar nova versão");
  }

  /** Continua encaminhando ao protótipo uma causa realmente observada durante a homologação. */
  @Test
  void preservesProductTechnicalFailureGuidance() {
    var guidance =
        PdeAgentValidationHarnessConsumer.failureGuidance(
            new PdeAgentValidationHarnessRunner.HarnessException(
                "A URL do PDE é inválida ou contém parâmetros não permitidos."));

    assertThat(guidance.get("category")).isEqualTo("TECHNICAL_FAILURE");
    assertThat(guidance.get("recommendedAction").toString()).contains("Corrija no protótipo");
  }
}
