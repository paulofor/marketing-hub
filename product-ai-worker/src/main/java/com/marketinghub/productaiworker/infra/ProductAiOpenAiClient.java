package com.marketinghub.productaiworker.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.productaiworker.config.ProductAiWorkerProperties;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** Responsabilidade: chamar OpenAI seguindo a regra de tentativas Flex, Flex e Standard. */
@Component
public class ProductAiOpenAiClient {
    private final RestClient restClient;
    private final ProductAiWorkerProperties properties;
    private final ObjectMapper objectMapper;

    /** Inicializa cliente HTTP da OpenAI. */
    public ProductAiOpenAiClient(RestClient.Builder builder, ProductAiWorkerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = builder.baseUrl(properties.getOpenAiBaseUrl()).build();
    }

    /** Executa chamada Responses API com fallback de service tier na terceira tentativa. */
    public OpenAiCallResult generate(
            String model,
            String prompt,
            String schemaJson,
            BiConsumer<String, Map<String, Object>> requestObserver) {
        RuntimeException lastError = null;
        List<String> tiers = List.of("flex", "flex", "default");
        for (String tier : tiers) {
            Map<String, Object> request = requestBody(model, prompt, schemaJson, tier);
            try {
                requestObserver.accept(tier, request);
                OpenAiResponse response = restClient.post()
                        .uri("/responses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getOpenAiApiKey())
                        .body(request)
                        .retrieve()
                        .body(OpenAiResponse.class);
                return new OpenAiCallResult(tier, request, response);
            } catch (RuntimeException ex) {
                lastError = ex;
            }
        }
        throw lastError != null ? lastError : new IllegalStateException("OpenAI não retornou resposta");
    }

    /** Monta corpo da Responses API com schema recebido do backend. */
    /** Monta corpo da Responses API com schema recebido do backend. */
    private Map<String, Object> requestBody(String model, String prompt, String schemaJson, String serviceTier) {
        return Map.of(
                "model", model,
                "input", prompt,
                "service_tier", serviceTier,
                "text", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "name", "product_ai_paid_delivery",
                                "schema", parseSchema(schemaJson),
                                "strict", true)));
    }

    /** Converte schema textual do backend em objeto JSON para a OpenAI. */
    private Object parseSchema(String schemaJson) {
        try {
            return objectMapper.readValue(
                    StringUtils.hasText(schemaJson) ? schemaJson : "{\"type\":\"object\"}",
                    Object.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Schema JSON inválido recebido do backend", ex);
        }
    }

    /** Resultado consolidado da chamada à OpenAI. */
    public record OpenAiCallResult(String serviceTier, Map<String, Object> requestBody, OpenAiResponse response) {}

    /** Resposta mínima da OpenAI usada pelo worker. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenAiResponse(
            String id,
            @JsonProperty("output_text") String outputText,
            Usage usage) {}

    /** Uso de tokens retornado pela OpenAI. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("input_tokens") Integer inputTokens,
            @JsonProperty("output_tokens") Integer outputTokens) {}
}
