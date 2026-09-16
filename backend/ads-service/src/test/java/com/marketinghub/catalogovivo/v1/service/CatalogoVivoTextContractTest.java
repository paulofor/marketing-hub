package com.marketinghub.catalogovivo.v1.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Responsabilidade: proteger o contrato textual do piloto sem depender de ambiente ou modelo
 * externo.
 */
class CatalogoVivoTextContractTest {
  /** Recusa ausência de instrução, placeholders desconhecidos, truncados ou repetidos. */
  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        " ",
        "Sem contexto",
        "{{UNKNOWN}}",
        "{{TASK_CONTEXT}} {{TASK_CONTEXT}}",
        "{{TASK_CONTEXT}} {{broken",
        "{{TASK_CONTEXT}} {{OTHER}}"
      })
  void rejectsInvalidTemplate(String text) {
    assertThatThrownBy(() -> CatalogoVivoService.validateText(text))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  }

  /** Conserva acentuação e a identidade exata do conteúdo operacional revisado. */
  @Test
  void validatesUtf8TextWithoutNormalizingTheReviewedContent() {
    String text = "Preparação Opala: {{TASK_CONTEXT}}\nPreserve a aprovação.";
    CatalogoVivoService.validateText(text);
    assertThat(CatalogoVivoService.sha256(text))
        .hasSize(64)
        .isNotEqualTo(CatalogoVivoService.sha256(text + "\n"));
  }
}
