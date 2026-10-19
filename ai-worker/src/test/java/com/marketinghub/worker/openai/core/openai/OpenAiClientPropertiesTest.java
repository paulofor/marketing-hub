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

    /** Codex usa sessão própria sem exigir, ler ou criar uma chave da API paga. */
    @Test
    void shouldAllowCodexWithoutApiKey() throws Exception {
        Path missingFile = tempDir.resolve("no-api-token");
        var properties = new OpenAiClientProperties(null, missingFile.toString(), "https://api.openai.com/v1",
                "gpt-test", Duration.ofSeconds(5), "http://backend/catalog", false, "codex-app-server");
        assertThat(properties.apiKey()).isNull();
        assertThat(properties.textTransport()).isEqualTo("codex-app-server");
    }

    /** Erro de configuração não pode reativar implicitamente a API paga. */
    @Test
    void shouldRejectUnknownTextTransport() {
        assertThatThrownBy(() -> new OpenAiClientProperties(null, null, "https://api.openai.com/v1",
                "gpt-test", Duration.ofSeconds(5), "http://backend/catalog", false, "codex-typo"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Transporte");
    }

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

    /** Comprova que a rotação do arquivo só chega ao cliente após criar uma nova configuração. */
    @Test
    void shouldLoadRotatedFileOnlyInANewConfiguration() throws Exception {
        Path file = tempDir.resolve("rotation-key");
        Files.writeString(file, "synthetic-before-rotation", StandardCharsets.UTF_8);
        OpenAiClientProperties before = propertiesFromFile(file);
        Files.writeString(file, "synthetic-after-rotation", StandardCharsets.UTF_8);

        assertThat(before.apiKey()).isEqualTo("synthetic-before-rotation");
        assertThat(propertiesFromFile(file).apiKey()).isEqualTo("synthetic-after-rotation");
    }

    /** Cria configuração de teste que lê somente o arquivo temporário, sem chamar a API. */
    private OpenAiClientProperties propertiesFromFile(Path file) {
        return new OpenAiClientProperties(null, file.toString(), "https://api.openai.com/v1",
                "gpt-test", Duration.ofSeconds(5), "http://backend/catalog", false);
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
