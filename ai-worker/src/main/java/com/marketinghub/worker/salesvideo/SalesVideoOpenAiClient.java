package com.marketinghub.worker.salesvideo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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

    public boolean isEnabled() {
        return enabled;
    }

    public GeneratedScriptResult generateScript(long jobId, String prompt) {
        if (!enabled) {
            throw new SalesVideoOpenAiException("OpenAI API key is not configured");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("modalities", List.of("text"));
        payload.put("max_output_tokens", maxOutputTokens);
        payload.put("messages", List.of(
                OpenAiRequestUtils.message("system", SYSTEM_PROMPT),
                OpenAiRequestUtils.message("user", prompt)
        ));
        payload.put("response_format", responseFormat());
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

        generationRecorder.record(DOMAIN, String.valueOf(jobId), prompt, output, model, response.usage());
        return new GeneratedScriptResult(payloadDto, output, response.usage(), response.id());
    }

    private Map<String, Object> responseFormat() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("required", List.of("hook", "script", "cta", "caption", "storyboard"));
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("hook", Map.of("type", "string"));
        properties.put("script", Map.of("type", "string"));
        properties.put("cta", Map.of("type", "string"));
        properties.put("caption", Map.of("type", "string"));
        Map<String, Object> storyboardItem = new LinkedHashMap<>();
        storyboardItem.put("type", "object");
        storyboardItem.put("required", List.of("scene", "visual", "voiceover", "durationSeconds"));
        Map<String, Object> storyboardProps = new LinkedHashMap<>();
        storyboardProps.put("scene", Map.of("type", "integer"));
        storyboardProps.put("visual", Map.of("type", "string"));
        storyboardProps.put("voiceover", Map.of("type", "string"));
        storyboardProps.put("durationSeconds", Map.of("type", "number"));
        storyboardItem.put("properties", storyboardProps);
        properties.put("storyboard", Map.of("type", "array", "items", storyboardItem));
        schema.put("properties", properties);
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("type", "json_schema");
        wrapper.put("json_schema", Map.of("name", "sales_video_script", "schema", schema));
        return wrapper;
    }

    private String serializeStoryboard(List<StoryboardScene> storyboard) {
        List<StoryboardScene> safeList = storyboard == null ? Collections.emptyList() : storyboard;
        try {
            return objectMapper.writeValueAsString(safeList);
        } catch (Exception ex) {
            throw new SalesVideoOpenAiException("Falha ao serializar storyboard", ex);
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
