package com.marketinghub.customeragentworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir regressões na política de raciocínio máximo de Psique. */
class PsiqueReasoningPolicyTest {

  /** Aceita e normaliza somente o valor máximo canônico. */
  @Test
  void acceptsMaximumReasoning() {
    assertThat(PsiqueReasoningPolicy.requireMaximum(" max ")).isEqualTo("max");
  }

  /** Rejeita ausência e todos os níveis inferiores de raciocínio. */
  @Test
  void rejectsMissingOrLowerReasoning() {
    assertThatThrownBy(() -> PsiqueReasoningPolicy.requireMaximum(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("deve ser max");
    assertThatThrownBy(() -> PsiqueReasoningPolicy.requireMaximum(""))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("deve ser max");
    assertThatThrownBy(() -> PsiqueReasoningPolicy.requireMaximum("high"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("deve ser max");
    assertThatThrownBy(() -> PsiqueReasoningPolicy.requireMaximum("xhigh"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("deve ser max");
  }
}
