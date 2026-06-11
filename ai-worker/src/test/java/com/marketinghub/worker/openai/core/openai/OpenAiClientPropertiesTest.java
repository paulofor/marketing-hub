package com.marketinghub.worker.openai.core.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: validar a resolução segura da credencial OpenAI usada pelo Worker AI. */
class OpenAiClientPropertiesTest {
    @TempDir
    Path tempDir;

    /** Deve usar o arquivo seguro quando a variável explícita contém apenas placeholder de teste. */
    @Test
    void shouldResolveApiKeyFromSecureFileWhenConfiguredValueIsPlaceholder() throws Exception {
        Path apiKeyFile = tempDir.resolve("openai_api_key");
        Files.writeString(apiKeyFile, "sk-real-from-file\n", StandardCharsets.UTF_8);

        OpenAiClientProperties properties = new OpenAiClientProperties(
                "test-key",
                apiKeyFile.toString(),
                "https://api.openai.com/v1",
                "gpt-5.5",
                Duration.ofSeconds(5),
                "http://backend/api/modelos/openai/catalogo/v1/modelos",
                false);

        assertThat(properties.apiKey()).isEqualTo("sk-real-from-file");
    }

    /** Deve bloquear inicialização real quando só existe placeholder e não há arquivo seguro válido. */
    @Test
    void shouldRejectPlaceholderApiKeyForRealOpenAiBaseUrl() {
        assertThatThrownBy(() -> new OpenAiClientProperties(
                        "test-key",
                        tempDir.resolve("missing-token").toString(),
                        "https://api.openai.com/v1",
                        "gpt-5.5",
                        Duration.ofSeconds(5),
                        "http://backend/api/modelos/openai/catalogo/v1/modelos",
                        false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OPENAI_API_KEY inválida ou ausente no Worker AI");
    }
}
