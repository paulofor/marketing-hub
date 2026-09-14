package com.marketinghub.videomanagement.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: prevenir inversão de negações sem liberar texto realmente solicitado ao provider. */
class EmbeddedTextInstructionGuardTest {
    /** Preserva proibições simples, listas coordenadas e a formulação histórica do job 21240. */
    @ParameterizedTest
    @ValueSource(strings = {
            "Tocar o celular sem revelar interface legível e sem mostrar texto",
            "Não gerar texto, interface, logo, depoimento ou reação de satisfação.",
            "Encerrar sem apontar, celebrar, depor ou mostrar tela, logo, letras ou CTA gerados.",
            "Encerrar sem apontar, celebrar, depor, ou mostrar logo.",
            "Não mostrar texto nem gerar interface.",
            "Evitar inserir legendas no vídeo.",
            "Mostrar a roupa sem gerar texto.",
            "Mostrar uma peça sem texto. Encerrar com gesto de decisão.",
            "Fazer um match cut para a captura homologada, sem reconstruir interface."
    })
    void shouldKeepExplicitProhibitions(String objective) {
        assertThat(EmbeddedTextInstructionGuard.isRequested(objective)).isFalse();
    }

    /** Mantém bloqueios quando a ordem positiva vem depois de outra proibição ou oração. */
    @ParameterizedTest
    @ValueSource(strings = {
            "Mostrar texto PROMESSA dentro do vídeo gerado",
            "Inserir um logo no canto da cena",
            "Exibir palavras no celular",
            "Aplicar preço na tela",
            "Não mostrar texto; inserir logo no final.",
            "Sem revelar interface, mas mostrar texto.",
            "Não gerar texto. Exibir legenda no encerramento.",
            "Não mostrar texto e inserir logo.",
            "Mostrar a roupa sem moldura e inserir texto.",
            "Mostrar uma peça sem texto, incluir logo ao fundo.",
            "Sem apontar, celebrar ou depor. Mostrar logo."
    })
    void shouldBlockPositiveInstructions(String objective) {
        assertThat(EmbeddedTextInstructionGuard.isRequested(objective)).isTrue();
    }
}
