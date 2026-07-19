package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Valida os textos transacionais enviados por e-mail na experiência PDE. */
class PdeMailServiceTest {

    /** Garante que o magic link da MUSA usa português brasileiro com acentuação. */
    @Test
    void buildsMagicLinkTextWithPortugueseAccents() {
        PdeMailService service = new PdeMailService("smtp", "us-east-1", "", 1025, "area-musa@sandbox.local", "", "");

        String text = service.buildMagicLinkText("https://clubemusa.com.br/access/teste");

        assertThat(text)
                .contains("está pronto", "diagnóstico inicial", "Método MUSA", "você não pediu")
                .doesNotContain("esta pronto", "diagnostico", "Metodo", "voce", "nao");
    }
}
