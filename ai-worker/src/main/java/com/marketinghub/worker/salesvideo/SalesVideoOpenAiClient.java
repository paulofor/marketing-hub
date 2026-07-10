package com.marketinghub.worker.salesvideo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Encapsula as chamadas à API de Responses da OpenAI para geração de script do Avatar Sales Video.
 */
@Component
public class SalesVideoOpenAiClient {
    private static final Logger log = LoggerFactory.getLogger(SalesVideoOpenAiClient.class);
    private static final String SYSTEM_PROMPT = "Você é um roteirista especialista em vídeos curtos de venda."
            + " Gere textos persuasivos, claros e orientados a conversão.";
    private static final String RESPONSES_PATH = "/responses";
    private static final String DOMAIN = "SALES_VIDEO_SCRIPT";
    private static final String SCHEMA_PATH = "prompts/salesvideo/sales-video-script-schema.json";
    private static final String SCHEMA_NAME = "sales_video_script";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AiGenerationRecorder generationRecorder;
    private final String model;
    private final boolean enabled;
    private final int maxOutputTokens;

    public SalesVideoOpenAiClient(WebClient.Builder builder,
                                  ObjectMapper objectMapper,
                                  AiGenerationRecorder generationRecorder,
                                  @Value("${openai.api-key:}") String apiKey,
                                  @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                  @Value("${openai.model:o3}") String model,
                                  @Value("${sales-video.script.max-output-tokens:1200}") int maxOutputTokens) {
        this.objectMapper = objectMapper;
        this.generationRecorder = generationRecorder;
        this.model = model;
        this.maxOutputTokens = Math.max(400, maxOutputTokens);
        this.enabled = apiKey != null && !apiKey.isBlank();
        WebClient.Builder clientBuilder = builder.clone().baseUrl(baseUrl);
        if (enabled) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            if (OpenAiRequestUtils.requiresReasoning(model)) {
                clientBuilder.defaultHeader("OpenAI-Beta", "reasoning=1");
            }
        } else {
            log.warn("OPENAI_API_KEY não configurada; jobs de vídeo serão ignorados");
        }
        this.webClient = clientBuilder.build();
    }

    /** Indica se o client pode chamar a OpenAI com chave configurada. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Gera o roteiro do vídeo usando Responses API com JSON Schema estrito e modo Flex. */
    public GeneratedScriptResult generateScript(long jobId, String prompt) {
        if (!enabled) {
            throw new SalesVideoOpenAiException("OpenAI API key is not configured");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", List.of(
                OpenAiRequestUtils.message("system", SYSTEM_PROMPT),
                OpenAiRequestUtils.message("user", prompt)
        ));
        payload.put("text", Map.of("format", responseFormat()));
        payload.put("service_tier", "flex");
        payload.put("max_output_tokens", maxOutputTokens);
        if (OpenAiRequestUtils.supportsTemperature(model)) {
            payload.put("temperature", 0.7);
        }
        OpenAiRequestUtils.maybeAddReasoning(payload, model);

        log.debug("Enviando payload de script para OpenAI (job {})", jobId);
        OpenAiResponse response = webClient.post()
                .uri(RESPONSES_PATH)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block();
        if (response == null) {
            throw new SalesVideoOpenAiException("Resposta vazia da OpenAI");
        }
        String rawResponseJson = serializeRawResponse(response);
        if (response.hasError()) {
            throw new SalesVideoOpenAiException(response.errorMessage());
        }
        if (!"completed".equalsIgnoreCase(response.status())) {
            throw new SalesVideoOpenAiException("Resposta da OpenAI não finalizada: " + response.status());
        }
        String output = response.firstText();
        if (!StringUtils.hasText(output)) {
            throw new SalesVideoOpenAiException("OpenAI não retornou conteúdo textual utilizável");
        }
        ScriptResponse scriptResponse;
        try {
            scriptResponse = objectMapper.readValue(output, ScriptResponse.class);
        } catch (Exception ex) {
            throw new SalesVideoOpenAiException("Falha ao interpretar JSON retornado pela OpenAI", ex);
        }
        Map<String, Object> payloadDto = new LinkedHashMap<>();
        payloadDto.put("hookText", scriptResponse.hook());
        payloadDto.put("scriptText", scriptResponse.script());
        payloadDto.put("ctaText", scriptResponse.cta());
        payloadDto.put("captionText", scriptResponse.caption());
        payloadDto.put("storyboardJson", serializeStoryboard(scriptResponse.storyboard()));
        payloadDto.put("prompt", prompt);
        payloadDto.put("model", model);

        generationRecorder.record(DOMAIN, String.valueOf(jobId), prompt, rawResponseJson, model, response.usage());
        return new GeneratedScriptResult(payloadDto, rawResponseJson, response.usage(), response.id());
    }

    /** Monta o formato de saída estruturada com schema versionado no classpath. */
    private Map<String, Object> responseFormat() {
        try {
            Object schema = objectMapper.readValue(loadSchema(), Object.class);
            Map<String, Object> format = new LinkedHashMap<>();
            format.put("type", "json_schema");
            format.put("name", SCHEMA_NAME);
            format.put("schema", schema);
            format.put("strict", true);
            return format;
        } catch (JsonProcessingException ex) {
            throw new SalesVideoOpenAiException("Schema de roteiro de vídeo inválido", ex);
        }
    }

    /** Serializa o storyboard para persistência no backend. */
    private String serializeStoryboard(List<StoryboardScene> storyboard) {
        List<StoryboardScene> safeList = storyboard == null ? Collections.emptyList() : storyboard;
        try {
            return objectMapper.writeValueAsString(safeList);
        } catch (Exception ex) {
            throw new SalesVideoOpenAiException("Falha ao serializar storyboard", ex);
        }
    }

    /** Carrega o schema versionado de roteiro de vídeo. */
    private String loadSchema() {
        try {
            return StreamUtils.copyToString(new ClassPathResource(SCHEMA_PATH).getInputStream(),
                    StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new SalesVideoOpenAiException("Schema de roteiro de vídeo não encontrado: " + SCHEMA_PATH, ex);
        }
    }

    /** Serializa a resposta completa para auditoria no backend. */
    private String serializeRawResponse(OpenAiResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new SalesVideoOpenAiException("Falha ao serializar resposta bruta da OpenAI", ex);
        }
    }

    /** Resultado bruto da OpenAI pronto para ser enviado ao backend. */
    public record GeneratedScriptResult(Map<String, Object> payload,
                                        String rawResponse,
                                        OpenAiResponse.OpenAiUsage usage,
                                        String responseId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ScriptResponse(String hook,
                                  String script,
                                  String cta,
                                  String caption,
                                  List<StoryboardScene> storyboard) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StoryboardScene(Integer scene,
                                   String visual,
                                   String voiceover,
                                   Integer durationSeconds) {
    }

    public static class SalesVideoOpenAiException extends RuntimeException {
        public SalesVideoOpenAiException(String message) {
            super(message);
        }

        public SalesVideoOpenAiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
