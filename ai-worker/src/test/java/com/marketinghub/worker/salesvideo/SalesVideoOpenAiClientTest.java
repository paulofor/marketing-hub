package com.marketinghub.worker.salesvideo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar o contrato OpenAI usado para roteiros de vídeo. */
class SalesVideoOpenAiClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockWebServer server;

    /** Inicializa a API OpenAI simulada para capturar o request enviado. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra a API simulada após o teste. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Deve enviar payload de Responses API com input, text.format json_schema e service_tier flex. */
    @Test
    void shouldSendResponsesApiPayloadWithStrictSchemaAndFlexTier() throws Exception {
        String output = """
                {"hook":"Gancho forte","script":"Script completo com promessa clara e prova concreta para vender melhor.","cta":"Baixe agora","caption":"Legenda pronta para redes sociais","storyboard":[{"scene":1,"visual":"Pessoa frustrada no trabalho","voiceover":"Voce trava na hora de negociar","durationSeconds":3},{"scene":2,"visual":"Checklist de scripts aparecendo","voiceover":"Use frases prontas para pedir mais","durationSeconds":3},{"scene":3,"visual":"Pessoa confiante fechando acordo","voiceover":"Comece pelo primeiro script hoje","durationSeconds":3}]}
                """;
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "id":"resp_123",
                          "status":"completed",
                          "output_text":%s
                        }
                        """.formatted(objectMapper.writeValueAsString(output))));
        AiGenerationRecorder recorder = org.mockito.Mockito.mock(AiGenerationRecorder.class);
        SalesVideoOpenAiClient client = new SalesVideoOpenAiClient(
                WebClient.builder(),
                objectMapper,
                recorder,
                "test-key",
                server.url("/v1").toString(),
                "gpt-5.2",
                1200);

        SalesVideoOpenAiClient.GeneratedScriptResult result = client.generateScript(77L, "Prompt comercial");

        var request = server.takeRequest();
        JsonNode body = objectMapper.readTree(request.getBody().readUtf8());
        assertThat(request.getPath()).isEqualTo("/v1/responses");
        assertThat(body.path("input")).isNotEmpty();
        assertThat(body.path("text").path("format").path("type").asText()).isEqualTo("json_schema");
        assertThat(body.path("text").path("format").path("name").asText()).isEqualTo("sales_video_script");
        assertThat(body.path("text").path("format").path("strict").asBoolean()).isTrue();
        assertThat(body.path("service_tier").asText()).isEqualTo("flex");
        assertThat(result.payload()).containsEntry("model", "gpt-5.2");
        verify(recorder).record(eq("SALES_VIDEO_SCRIPT"), eq("77"), eq("Prompt comercial"), startsWith("{"), eq("gpt-5.2"), isNull());
    }
}
