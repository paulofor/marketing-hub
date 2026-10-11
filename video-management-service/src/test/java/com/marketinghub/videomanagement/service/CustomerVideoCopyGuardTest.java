package com.marketinghub.videomanagement.service;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Previne a regressão de Mira sem proibir contexto interno ou transparência útil à cliente. */
class CustomerVideoCopyGuardTest {
    /** Rejeita falas do incidente e variantes de caixa/acentos antes de qualquer fornecedor. */
    @ParameterizedTest
    @ValueSource(strings = {"Experimentar Mira em acesso privado", "Demonstração interna · sem cobrança", "HOMOLOGAÇÃO aprovada", "callback registrado"})
    void rejectsOperationalNarration(String text) {
        var input = new ObjectMapper().createObjectNode().put("voiceOverScript", text);
        assertThatThrownBy(() -> CustomerVideoCopyGuard.validate(input))
                .hasMessageContaining("linguagem operacional");
    }

    /** Aceita linguagem comercial clara de outros produtos e mantém instruções fora da copy. */
    @Test
    void preservesValidCopyAndPrivateOperationalContext() {
        var input = new ObjectMapper().createObjectNode();
        input.put("voiceOverScript", "Organize seus produtos. Veja um exemplo antes de escolher continuar.");
        input.put("captionText", "Exemplo ilustrativo. Voz gerada por IA.");
        input.put("internalNotes", "Acesso privado; homologação; ciclo #9; callback");
        assertThatCode(() -> CustomerVideoCopyGuard.validate(input)).doesNotThrowAnyException();
        input.putObject("post_production").put("cta_text", "Abrir acesso privado");
        assertThatThrownBy(() -> CustomerVideoCopyGuard.validate(input)).hasMessageContaining("cta_text");
    }
}
