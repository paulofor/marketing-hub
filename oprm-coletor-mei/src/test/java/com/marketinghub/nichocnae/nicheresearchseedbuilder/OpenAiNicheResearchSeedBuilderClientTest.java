package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: validar a resolução segura da chave OpenAI no módulo OPRM. */
class OpenAiNicheResearchSeedBuilderClientTest {
    @TempDir Path tempDir;

    /** Confirma que a variável direta tem prioridade sobre o arquivo montado no host compartilhado. */
    @Test
    void shouldPreferDirectApiKeyWhenConfigured() throws Exception {
        Path keyFile = tempDir.resolve("openai_api_key");
        Files.writeString(keyFile, "file-key");
        OpenAiNicheResearchSeedBuilderClient client = clientWithProperties(
                new NicheResearchSeedBuilderOpenAiProperties("https://api.openai.com/v1", "direct-key", keyFile.toString(), "gpt-test"));

        String apiKey = client.resolveApiKey(pending());

        assertThat(apiKey).isEqualTo("direct-key");
    }

    /** Confirma que a etapa dois lê a chave do arquivo já usado pelo host do ai-worker. */
    @Test
    void shouldReadApiKeyFromMountedFileWhenDirectKeyIsBlank() throws Exception {
        Path keyFile = tempDir.resolve("openai_api_key");
        Files.writeString(keyFile, " file-key \n");
        OpenAiNicheResearchSeedBuilderClient client = clientWithProperties(
                new NicheResearchSeedBuilderOpenAiProperties("https://api.openai.com/v1", "", keyFile.toString(), "gpt-test"));

        String apiKey = client.resolveApiKey(pending());

        assertThat(apiKey).isEqualTo("file-key");
    }

    /** Cria o client com dependências nulas porque o teste cobre apenas resolução de chave. */
    private OpenAiNicheResearchSeedBuilderClient clientWithProperties(NicheResearchSeedBuilderOpenAiProperties properties) {
        return new OpenAiNicheResearchSeedBuilderClient(null, null, properties, null, null);
    }

    /** Cria uma pendência mínima para contexto de logs de resolução de chave. */
    private NicheResearchSeedBuilderPending pending() {
        return new NicheResearchSeedBuilderPending(
                1001L,
                55L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Cabeleireiros, manicure e pedicure",
                BigDecimal.valueOf(92),
                "AUTO_SCORE_QUEUE",
                "RUNNING",
                Instant.now(),
                Instant.now());
    }
}
