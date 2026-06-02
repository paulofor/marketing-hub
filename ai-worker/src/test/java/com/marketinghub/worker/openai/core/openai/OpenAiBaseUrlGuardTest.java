package com.marketinghub.worker.openai.core.openai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Responsabilidade: validar o bloqueio de base URL local acidental para clientes OpenAI. */
class OpenAiBaseUrlGuardTest {

    /** Deve trocar localhost pelo endpoint oficial quando a permissão local não estiver habilitada. */
    @Test
    void resolveShouldReplaceLocalhostWhenLocalBaseUrlIsNotAllowed() {
        String resolved = OpenAiBaseUrlGuard.resolve("http://localhost:34303", false);

        assertThat(resolved).isEqualTo(OpenAiBaseUrlGuard.DEFAULT_OPENAI_BASE_URL);
    }

    /** Deve preservar localhost apenas quando a execução de teste/desenvolvimento permitir explicitamente. */
    @Test
    void resolveShouldKeepLocalhostWhenLocalBaseUrlIsAllowed() {
        String resolved = OpenAiBaseUrlGuard.resolve("http://127.0.0.1:34303", true);

        assertThat(resolved).isEqualTo("http://127.0.0.1:34303");
    }

    /** Deve preservar a URL oficial da OpenAI usada em produção. */
    @Test
    void resolveShouldKeepOfficialOpenAiUrl() {
        String resolved = OpenAiBaseUrlGuard.resolve("https://api.openai.com/v1", false);

        assertThat(resolved).isEqualTo("https://api.openai.com/v1");
    }
}
