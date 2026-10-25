package com.marketinghub.pde.kit.privateprototype.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

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
}
