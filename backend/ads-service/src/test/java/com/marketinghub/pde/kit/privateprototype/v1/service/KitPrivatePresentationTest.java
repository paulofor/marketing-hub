package com.marketinghub.pde.kit.privateprototype.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: impedir que a apresentação do kit dependa de diagnósticos ou IDs internos. */
class KitPrivatePresentationTest {
  /**
   * Confere mensagens seguras para estados reais em identidades distintas, sem gerar resultados.
   */
  @ParameterizedTest
  @ValueSource(strings = {"INPUT", "QUEUED", "RUNNING", "READY", "FAILED", "BLOCKED_SAFE"})
  void separatesClientMessagesFromOperationalContext(String status) {
    for (long productId : new long[] {7L, 9918L}) {
      var presentation =
          KitPrivatePresentation.resolve(status, "UNVERIFIED_VISUAL_ORIGIN", productId, 8831L);
      assertThat(presentation.title()).isNotBlank();
      assertThat(presentation.introduction()).isNotBlank();
      assertThat(presentation.statusMessage())
          .doesNotContain("productId", "AGENT_VALIDATION", "fixture", "SQLException", "8831");
      if (!"INPUT".equals(status)) assertThat(presentation.statusMessage()).isNotBlank();
      if ("FAILED".equals(status))
        assertThat(presentation.statusMessage()).contains("dados estão guardados");
    }
  }

  /** Impede o retorno ao painel administrativo tanto no bloqueio quanto na falha de composição. */
  @Test
  void keepsFailureGuidanceInsideCustomerExperience() {
    for (String status : new String[] {"BLOCKED_SAFE", "FAILED"}) {
      for (String reason :
          new String[] {"UNVERIFIED_VISUAL_ORIGIN", "EXTERNAL_ACTION", "REVIEW_REQUIRED"}) {
        var presentation = KitPrivatePresentation.resolve(status, reason, 9918L, 8831L);
        assertThat(presentation.nextActionPath()).isEqualTo("#kit-continuation");
        assertThat(presentation.nextActionLabel()).isNotBlank().doesNotContain("ciclo");
        assertThat(presentation.nextStep())
            .contains("equipe do produto")
            .doesNotContain("8831", "9918", "entrada compatível", "revisão deste ciclo");
        assertThat(presentation.statusMessage()).doesNotContain("revisão interna");
      }
    }
  }
}
