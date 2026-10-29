package com.marketinghub.financialagentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: impedir redução silenciosa do raciocínio de Plutus nas chamadas e auditorias.
 */
class MaximumReasoningContractTest {
  /** Confirma max no uso sem sobrescrita operacional. */
  @Test
  void deveUsarMaxComoPadrao() {
    var properties = new FinancialAgentProperties();
    assertThat(properties.getReasoningEffort()).isEqualTo("max");
    assertThat(properties.requiredReasoningEffort()).isEqualTo("max");
  }

  /** Bloqueia configuração antiga ou inválida em todos os acessos ao esforço. */
  @Test
  void deveRejeitarSobrescritaInferiorAntesDoModelo() {
    for (String effort : new String[] {null, "", " ", "low", "medium", "high", "xhigh", "ultra"}) {
      var properties = new FinancialAgentProperties();
      properties.setReasoningEffort(effort);
      assertThatThrownBy(properties::getReasoningEffort)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("max");
      assertThatThrownBy(properties::requiredReasoningEffort)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("max");
    }
  }

  /** Normaliza apenas espaços, preservando a configuração máxima explícita. */
  @Test
  void deveManterMesmoValorNaAuditoriaEComando() {
    var properties = new FinancialAgentProperties();
    properties.setReasoningEffort(" max ");
    assertThat(properties.getReasoningEffort()).isEqualTo("max");
    assertThat(properties.requiredReasoningEffort()).isEqualTo("max");
  }
}
