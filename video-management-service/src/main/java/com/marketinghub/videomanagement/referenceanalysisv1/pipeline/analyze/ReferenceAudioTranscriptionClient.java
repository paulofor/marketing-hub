package com.marketinghub.videomanagement.referenceanalysisv1.pipeline.analyze;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.referenceanalysisv1.pipeline.ReferenceAnalysisStageContext;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Transcreve a faixa falada da referência e preserva auditoria e custo separados da análise visual. */
@Component
public class ReferenceAudioTranscriptionClient {
    private static final Logger log = LoggerFactory.getLogger(ReferenceAudioTranscriptionClient.class);
    private static final String PROMPT_PATH = "prompts/apollo/reference-analysis/v1/transcribe.md";
    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient openAi;

    /** Configura o cliente de transcrição dentro da etapa concreta de análise. */
    public ReferenceAudioTranscriptionClient(VideoManagementProperties properties,
                                             ObjectMapper objectMapper,
                                             WebClient.Builder builder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.openAi = builder.baseUrl(properties.getReferenceAnalysis().getOpenAiBaseUrl().toString()).build();
    }

    /** Transcreve áudio existente ou registra ausência sem fazer uma chamada externa. */
    public TranscriptionInteraction transcribe(ReferenceAnalysisStageContext context,
                                                ReferenceMediaInspector.Evidence evidence) {
        if (evidence.audioTrack() == null) {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("status", "NOT_APPLICABLE");
            request.put("reason", "NO_AUDIO_STREAM");
            ObjectNode response = request.deepCopy();
            return new TranscriptionInteraction(
                    "", request, response, BigDecimal.ZERO, null, null, "NOT_APPLICABLE");
        }
        String key = resolveApiKey();
        ObjectNode request = auditRequest(context, evidence.audioTrack());
        if (!StringUtils.hasText(key)) {
            throw new TranscriptionFailure(
                    "Credencial OpenAI ausente para transcrição da referência", null, request, null);
        }
        MultipartBodyBuilder parts = new MultipartBodyBuilder();
        ByteArrayResource audio = new ByteArrayResource(evidence.audioTrack().content()) {
            /** Fornece um nome e extensão reconhecíveis ao multipart de áudio. */
            @Override
            public String getFilename() {
                return evidence.audioTrack().filename();
            }
        };
        parts.part("file", audio).contentType(MediaType.parseMediaType(evidence.audioTrack().contentType()));
        parts.part("model", properties.getReferenceAnalysis().getTranscriptionModel());
        parts.part("response_format", "json");
        parts.part("prompt", request.path("prompt").asText());
        String url = "/audio/transcriptions";
        try {
            log.info("Request OpenAI transcrição de referência; executionId={} url={} payload={}",
                    context.executionId(), url, request);
            JsonNode response = openAi.post().uri(url)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header("Authorization", "Bearer " + key)
                    .body(BodyInserters.fromMultipartData(parts.build()))
                    .retrieve().bodyToMono(JsonNode.class).block();
            log.info("Response OpenAI transcrição de referência; executionId={} url={} response={}",
                    context.executionId(), url, response);
            String text = response == null ? null : response.path("text").asText(null);
            if (response == null || !response.isObject()) {
                throw new TranscriptionFailure(
                        "OpenAI não retornou resposta válida para a faixa de áudio", null, request, response);
            }
            JsonNode usage = response.path("usage");
            return new TranscriptionInteraction(
                    StringUtils.hasText(text) ? text.trim() : "",
                    request,
                    response,
                    estimatedCost(evidence.artifacts().path("durationSeconds").asDouble()),
                    nullableLong(usage, "input_tokens"),
                    nullableLong(usage, "output_tokens"),
                    StringUtils.hasText(text) ? "COMPLETED" : "NO_SPEECH_DETECTED");
        } catch (TranscriptionFailure ex) {
            log.error("Transcrição inválida da referência; executionId={} url={}",
                    context.executionId(), url, ex);
            throw ex;
        } catch (WebClientResponseException ex) {
            log.error("Falha OpenAI na transcrição da referência; executionId={} url={}",
                    context.executionId(), url, ex);
            throw new TranscriptionFailure(
                    "OpenAI rejeitou a transcrição da referência", ex, request, errorResponse(ex));
        } catch (RuntimeException ex) {
            log.error("Falha OpenAI na transcrição da referência; executionId={} url={}",
                    context.executionId(), url, ex);
            throw new TranscriptionFailure(
                    "OpenAI não concluiu a transcrição da referência", ex, request, null);
        }
    }

    /** Monta o request sanitizado persistido sem incorporar o binário da faixa. */
    private ObjectNode auditRequest(ReferenceAnalysisStageContext context,
                                    ReferenceMediaInspector.AudioTrack audioTrack) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("endpoint", "/audio/transcriptions");
        request.put("model", properties.getReferenceAnalysis().getTranscriptionModel());
        request.put("responseFormat", "json");
        request.put("serviceTier", "NOT_SUPPORTED_BY_AUDIO_TRANSCRIPTIONS_API");
        request.put("audioFilename", audioTrack.filename());
        request.put("audioContentType", audioTrack.contentType());
        request.put("audioSha256", audioTrack.sha256());
        request.put("audioBytes", audioTrack.bytes());
        request.put("prompt", transcriptionPrompt(context));
        return request;
    }

    /** Resolve o texto versionado que orienta nomes próprios sem alterar o conteúdo falado. */
    private String transcriptionPrompt(ReferenceAnalysisStageContext context) {
        return resource(PROMPT_PATH)
                .replace("{{TITLE}}", context.input().path("title").asText("não informado"))
                .replace("{{NICHE}}", context.input().path("niche").asText("não informado"));
    }

    /** Calcula a estimativa explícita por duração usando a tarifa versionada. */
    private BigDecimal estimatedCost(double durationSeconds) {
        if (!Double.isFinite(durationSeconds) || durationSeconds <= 0) {
            throw new IllegalArgumentException("Duração inválida para estimar a transcrição");
        }
        return properties.getReferenceAnalysis().getTranscriptionPricePerMinuteUsd()
                .multiply(BigDecimal.valueOf(durationSeconds))
                .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);
    }

    /** Resolve a credencial direta ou montada em arquivo sem registrá-la. */
    private String resolveApiKey() {
        String direct = properties.getReferenceAnalysis().getApiKey();
        if (StringUtils.hasText(direct)) {
            return direct.trim();
        }
        String file = properties.getReferenceAnalysis().getApiKeyFile();
        if (!StringUtils.hasText(file)) {
            return null;
        }
        try {
            return Files.readString(Path.of(file), StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            log.error("Falha ao ler secret OpenAI da transcrição de referência; arquivo={}", file, ex);
            return null;
        }
    }

    /** Carrega integralmente o prompt versionado do classpath. */
    private String resource(String path) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Recurso ausente: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("Falha ao carregar prompt da transcrição de referência; recurso={}", path, ex);
            throw new IllegalStateException("Prompt da transcrição de referência indisponível", ex);
        }
    }

    /** Preserva status e body devolvidos pela integração sem inventar resposta funcional. */
    private JsonNode errorResponse(WebClientResponseException error) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("status", error.getStatusCode().value());
        String body = error.getResponseBodyAsString();
        if (!StringUtils.hasText(body)) {
            result.putNull("body");
            return result;
        }
        try {
            result.set("body", objectMapper.readTree(body));
        } catch (IOException ex) {
            log.warn("Resposta de erro da transcrição não é JSON; status={}",
                    error.getStatusCode().value(), ex);
            result.put("body", body);
        }
        return result;
    }

    /** Lê uma contagem de tokens quando o endpoint a disponibilizar. */
    private Long nullableLong(JsonNode node, String field) {
        return node.has(field) && node.path(field).canConvertToLong() ? node.path(field).asLong() : null;
    }

    /** Consolida texto, auditoria, uso e custo da chamada de transcrição. */
    public record TranscriptionInteraction(
            String text,
            ObjectNode request,
            JsonNode response,
            BigDecimal estimatedCostUsd,
            Long inputTokens,
            Long outputTokens,
            String status) { }

    /** Falha de transcrição acompanhada do request e da resposta externa disponíveis. */
    public static class TranscriptionFailure extends RuntimeException {
        private final ObjectNode request;
        private final JsonNode response;

        /** Encapsula a falha sem perder a auditoria da tentativa. */
        public TranscriptionFailure(String message, Throwable cause, ObjectNode request, JsonNode response) {
            super(message, cause);
            this.request = request;
            this.response = response;
        }

        /** Devolve o request sanitizado efetivamente montado. */
        public ObjectNode request() {
            return request;
        }

        /** Devolve a resposta externa disponível, inclusive erro estruturado. */
        public JsonNode response() {
            return response;
        }
    }
}
