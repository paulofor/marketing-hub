package com.marketinghub.videomanagement.referenceanalysisv1.pipeline.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.referenceanalysisv1.pipeline.ReferenceAnalysisStageContext;
import java.time.Instant;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Valida transcrição auditável, custo por duração e ausência de consumo em vídeo silencioso. */
class ReferenceAudioTranscriptionClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockWebServer server;

    /** Inicializa uma API de transcrição simulada antes de cada cenário. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra a API simulada ao final de cada cenário. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Envia MP3 compacto, prompt versionado e calcula custo pela duração auditada. */
    @Test
    void shouldTranscribeAudioWithSanitizedAuditAndEstimatedCost() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"text\":\"Seu trabalho merece ser visto. Veja como começar.\","
                        + "\"usage\":{\"input_tokens\":30,\"output_tokens\":12}}"));
        VideoManagementProperties properties = properties();
        ReferenceAudioTranscriptionClient client = new ReferenceAudioTranscriptionClient(
                properties, objectMapper, WebClient.builder());

        var result = client.transcribe(context(), evidence(true));

        var request = server.takeRequest();
        String multipart = request.getBody().readUtf8();
        assertThat(request.getPath()).isEqualTo("/audio/transcriptions");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer transcription-test-key");
        assertThat(multipart)
                .contains("gpt-transcribe", "reference-91.mp3", "Rio Antigo", "Transcreva fielmente")
                .doesNotContain("transcription-test-key");
        assertThat(result.text()).contains("Seu trabalho merece ser visto");
        assertThat(result.estimatedCostUsd()).isEqualByComparingTo("0.004781");
        assertThat(result.inputTokens()).isEqualTo(30L);
        assertThat(result.outputTokens()).isEqualTo(12L);
        assertThat(result.request().path("serviceTier").asText())
                .isEqualTo("NOT_SUPPORTED_BY_AUDIO_TRANSCRIPTIONS_API");
        assertThat(result.request().toString()).doesNotContain("transcription-test-key");
    }

    /** Evita chamada e custo quando o arquivo não possui faixa de áudio. */
    @Test
    void shouldSkipExternalCallWhenReferenceHasNoAudio() throws Exception {
        ReferenceAudioTranscriptionClient client = new ReferenceAudioTranscriptionClient(
                properties(), objectMapper, WebClient.builder());

        var result = client.transcribe(context(), evidence(false));

        assertThat(result.status()).isEqualTo("NOT_APPLICABLE");
        assertThat(result.text()).isEmpty();
        assertThat(result.estimatedCostUsd()).isZero();
        assertThat(server.getRequestCount()).isZero();
    }

    /** Mantém análise e custo quando existe áudio, mas nenhuma fala é identificada. */
    @Test
    void shouldKeepAudioWithoutSpeechAsValidEvidence() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"text\":\"\",\"usage\":{\"input_tokens\":30,\"output_tokens\":0}}"));
        ReferenceAudioTranscriptionClient client = new ReferenceAudioTranscriptionClient(
                properties(), objectMapper, WebClient.builder());

        var result = client.transcribe(context(), evidence(true));

        assertThat(result.status()).isEqualTo("NO_SPEECH_DETECTED");
        assertThat(result.text()).isEmpty();
        assertThat(result.estimatedCostUsd()).isEqualByComparingTo("0.004781");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    /** Preserva request e erro bruto quando o endpoint recusa a transcrição. */
    @Test
    void shouldPreserveAuditWhenTranscriptionFails() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"message\":\"rate limited\"}}"));
        ReferenceAudioTranscriptionClient client = new ReferenceAudioTranscriptionClient(
                properties(), objectMapper, WebClient.builder());

        assertThatThrownBy(() -> client.transcribe(context(), evidence(true)))
                .isInstanceOfSatisfying(
                        ReferenceAudioTranscriptionClient.TranscriptionFailure.class,
                        failure -> {
                            assertThat(failure.request().path("model").asText())
                                    .isEqualTo("gpt-transcribe");
                            assertThat(failure.response().path("status").asInt()).isEqualTo(429);
                            assertThat(failure.response().toString()).contains("rate limited");
                        });
    }

    /** Monta propriedades apontando para a API simulada. */
    private VideoManagementProperties properties() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getReferenceAnalysis().setOpenAiBaseUrl(server.url("/").uri());
        properties.getReferenceAnalysis().setApiKey("transcription-test-key");
        return properties;
    }

    /** Monta o contexto comercial persistido da referência. */
    private ReferenceAnalysisStageContext context() throws Exception {
        return new ReferenceAnalysisStageContext(
                91L,
                2L,
                "tenant-test",
                1,
                "producer-91",
                objectMapper.readTree(
                        "{\"title\":\"Rio Antigo\",\"niche\":\"documentário histórico\"}"),
                Instant.now());
    }

    /** Monta evidência com ou sem faixa de áudio sem usar arquivo externo. */
    private ReferenceMediaInspector.Evidence evidence(boolean withAudio) {
        ObjectNode artifacts = objectMapper.createObjectNode();
        artifacts.put("durationSeconds", 63.745);
        ReferenceMediaInspector.AudioTrack audio = withAudio
                ? new ReferenceMediaInspector.AudioTrack(
                        new byte[] {1, 2, 3, 4},
                        "reference-91.mp3",
                        "audio/mpeg",
                        "audio-sha",
                        4)
                : null;
        return new ReferenceMediaInspector.Evidence(artifacts, List.of(), audio);
    }
}
