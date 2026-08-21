package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Valida os textos transacionais enviados por e-mail na experiência PDE. */
class PdeMailServiceTest {

    /** Garante que o magic link genérico usa português brasileiro e permite retomar a entrega. */
    @Test
    void buildsMagicLinkTextWithPortugueseAccents() {
        PdeMailService service = new PdeMailService("smtp", "us-east-1", "", 1025, "area-musa@sandbox.local", "", "");

        String text = service.buildMagicLinkText("https://clubemusa.com.br/access/teste");

        assertThat(text)
                .contains("está pronto", "retomar seu progresso", "consultar entregas", "você não pediu")
                .doesNotContain("esta pronto", "voce", "nao");
    }
}
