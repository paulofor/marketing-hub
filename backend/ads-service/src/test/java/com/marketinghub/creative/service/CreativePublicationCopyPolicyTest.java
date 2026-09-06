package com.marketinghub.creative.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.creative.Creative;
import org.junit.jupiter.api.Test;

/** Protege os limites do publicador e a integridade Unicode da copy aprovada. */
class CreativePublicationCopyPolicyTest {
  /** Aceita os limites exatos sem confundir pares substitutos com dois caracteres. */
  @Test
  void acceptsUnicodeBoundariesWithoutTruncation() {
    Creative creative =
        Creative.builder()
            .primaryText("😀".repeat(125))
            .headline("A".repeat(40))
            .description("D".repeat(25))
            .build();
    assertThat(CreativePublicationCopyPolicy.violations(creative)).isEmpty();
    assertThat(creative.getPrimaryText()).isEqualTo("😀".repeat(125));
  }

  /** Explica todos os excessos de uma vez e preserva o conteúdo recebido. */
  @Test
  void reportsEveryOversizedField() {
    Creative creative =
        Creative.builder()
            .primaryText("x".repeat(202))
            .headline("A".repeat(41))
            .description("D".repeat(26))
            .build();
    assertThat(CreativePublicationCopyPolicy.violations(creative))
        .containsExactly(
            "Texto principal: 202/125 caracteres.",
            "Título: 41/40 caracteres.",
            "Descrição: 26/25 caracteres.");
    assertThat(creative.getPrimaryText()).hasSize(202);
  }
}
