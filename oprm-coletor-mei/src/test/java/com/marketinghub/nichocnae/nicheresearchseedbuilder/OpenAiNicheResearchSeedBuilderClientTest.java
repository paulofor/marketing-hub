package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Responsabilidade: validar a resolução segura da chave OpenAI no módulo OPRM. */
class OpenAiNicheResearchSeedBuilderClientTest {
    @TempDir Path tempDir;

    /** Confirma que a variável direta tem prioridade sobre o arquivo montado no host compartilhado. */
    @Test
    void shouldPreferDirectApiKeyWhenConfigured() throws Exception {
        Path keyFile = tempDir.resolve("openai_api_key");
        Files.writeString(keyFile, "file-key");
        OpenAiNicheResearchSeedBuilderClient client = clientWithProperties(new NicheResearchSeedBuilderOpenAiProperties(
                "https://api.openai.com/v1", "direct-key", keyFile.toString(), "gpt-test", "flex"));

        String apiKey = client.resolveApiKey(pending());

        assertThat(apiKey).isEqualTo("direct-key");
    }

    /** Confirma que a etapa dois lê a chave do arquivo já usado pelo host do ai-worker. */
    @Test
    void shouldReadApiKeyFromMountedFileWhenDirectKeyIsBlank() throws Exception {
        Path keyFile = tempDir.resolve("openai_api_key");
        Files.writeString(keyFile, " file-key \n");
        OpenAiNicheResearchSeedBuilderClient client = clientWithProperties(new NicheResearchSeedBuilderOpenAiProperties(
                "https://api.openai.com/v1", "", keyFile.toString(), "gpt-test", "flex"));

        String apiKey = client.resolveApiKey(pending());

        assertThat(apiKey).isEqualTo("file-key");
    }

    /** Confirma que a configuração operacional recebida do backend define o modelo enviado à OpenAI. */
    @Test
    void shouldResolveModelFromPendingStageConfiguration() {
        OpenAiNicheResearchSeedBuilderClient client = clientWithProperties(new NicheResearchSeedBuilderOpenAiProperties(
                "https://api.openai.com/v1", "direct-key", "", "gpt-4.1-mini", "flex"));

        String model = client.resolveModel(pending());

        assertThat(model).isEqualTo("gpt-5.4");
    }

    /** Confirma que a requisição da etapa dois usa Flex Processing por padrão para reduzir custo operacional. */
    @Test
    void shouldUseFlexServiceTierInRequestBodyByDefault() {
        OpenAiNicheResearchSeedBuilderClient client = clientWithProperties(new NicheResearchSeedBuilderOpenAiProperties(
                "https://api.openai.com/v1", "direct-key", "", "gpt-test", null));

        Map<String, Object> requestBody = client.buildRequestBody("prompt", "gpt-test");

        assertThat(requestBody).containsEntry("service_tier", "flex");
    }

    /** Confirma que falhas de transporte na OpenAI geram mensagem operacional clara para a tela. */
    @Test
    void shouldReportOpenAiWhenTransportFails() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo(URI.create("https://api.openai.com/v1/responses")))
                .andExpect(jsonPath("$.service_tier").value("flex"))
                .andRespond(withException(new IOException("Broken pipe")));
        OpenAiNicheResearchSeedBuilderClient client = new OpenAiNicheResearchSeedBuilderClient(
                builder.build(),
                new ObjectMapper(),
                new NicheResearchSeedBuilderOpenAiProperties(
                        "https://api.openai.com/v1", "direct-key", "", "gpt-test", null),
                new NicheResearchSeedBuilderPromptBuilder(),
                new NicheResearchSeedBuilderSchema());

        assertThatThrownBy(() -> client.generate(pending()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Falha na OpenAI ao gerar seed da etapa dois OPRM nichocnae")
                .hasRootCauseMessage("Broken pipe");
        server.verify();
    }

    /** Cria o client com dependências mínimas porque o teste cobre resolução de chave, modelo e corpo de requisição. */
    private OpenAiNicheResearchSeedBuilderClient clientWithProperties(
            NicheResearchSeedBuilderOpenAiProperties properties) {
        return new OpenAiNicheResearchSeedBuilderClient(
                null,
                new ObjectMapper(),
                properties,
                new NicheResearchSeedBuilderPromptBuilder(),
                new NicheResearchSeedBuilderSchema());
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
                125000L,
                "gpt-5.4",
                "gpt-5.4 (gpt-5.4)",
                "AUTO_SCORE_QUEUE",
                null,
                null,
                null,
                null,
                List.of(),
                "RUNNING",
                Instant.now(),
                Instant.now());
    }
}
