package com.marketinghub.worker.successproduct;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.successproduct.SuccessProduct;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * OpenAI implementation that asks ChatGPT to extract a market niche and
 * hypothesis from a success product description.
 */
@Component
@Profile("!test & !dummy")
public class SuccessProductOpenAiChatGptClient implements ChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(SuccessProductOpenAiChatGptClient.class);
    private static final ProductCapabilities DEFAULT_PRODUCT_CAPABILITIES = new ProductCapabilities(
            true,
            true,
            true,
            true,
            true,
            false,
            false,
            false,
            false,
            false);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final AiGenerationRecorder generationRecorder;
    private static final String DOMAIN = "SUCCESS_PRODUCT_EXTRACTION";

    public SuccessProductOpenAiChatGptClient(
            WebClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${openai.model:gpt-3.5-turbo}") String model,
            AiGenerationRecorder generationRecorder) {
        WebClient.Builder clientBuilder = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        if (OpenAiRequestUtils.requiresReasoning(model)) {
            clientBuilder.defaultHeader("OpenAI-Beta", "reasoning=1");
        }
        this.webClient = clientBuilder.build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.generationRecorder = generationRecorder;
    }

    @Override
    public NicheHypothesis extract(SuccessProduct product) {
        Map<String, Object> productCapabilities = DEFAULT_PRODUCT_CAPABILITIES.asMap();
        String prompt = buildPrompt(product, productCapabilities);
        List<Map<String, Object>> input = List.of(
                OpenAiRequestUtils.message("system", "Você é um especialista em marketing."),
                OpenAiRequestUtils.message("user", prompt)
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", input);
        payload.put("response_format", responseFormat());
        OpenAiRequestUtils.maybeAddReasoning(payload, model);

        log.info("Sending prompt to ChatGPT for product {}", product.getId());
        OpenAiResponse response = webClient.post()
                .uri("/responses")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block();

        log.info("ChatGPT raw response: {}", response);

        if (response == null) {
            log.warn("ChatGPT returned no choices for product {}", product.getId());
            return null;
        }
        if (response.hasError()) {
            log.error("OpenAI error while processing product {}: {}", product.getId(), response.errorMessage());
            return null;
        }
        String content = response.firstText();
        generationRecorder.record(DOMAIN,
                product != null ? String.valueOf(product.getId()) : null,
                prompt,
                content,
                model,
                response.usage());
        if (content == null || content.isBlank()) {
            log.warn("ChatGPT returned empty content for product {}", product.getId());
            return null;
        }
        log.info("ChatGPT content: {}", content);
        try {
            JsonNode node = objectMapper.readTree(content);
            return new NicheHypothesis(
                    asText(node, "nicheName"),
                    asText(node, "nicheDescription"),
                    asText(node, "hypothesisTitle"),
                    asText(node, "persona"),
                    asText(node, "problem"),
                    asText(node, "promise"),
                    asText(node, "uniqueMechanism"));
        } catch (Exception e) {
            log.error("Failed to parse ChatGPT response: {}", content, e);
            return null;
        }
    }

    private String buildPrompt(SuccessProduct product, Map<String, Object> productCapabilities) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analise o input estruturado do worker e retorne um JSON com as chaves ")
                .append("\"nicheName\", \"nicheDescription\", \"hypothesisTitle\", \"persona\", \"problem\", \"promise\", \"uniqueMechanism\".\n")
                .append("Toda resposta deve respeitar as capacidades reais do produto e nunca propor nada fora delas.\n");
        sb.append("Input estruturado do worker:\n");
        sb.append("{\n");
        appendJsonField(sb, "name", product != null ? product.getName() : null, true);
        appendJsonField(sb, "description", product != null ? product.getDescription() : null, true);
        appendJsonObjectField(sb, "productCapabilities", productCapabilities, false);
        sb.append("}\n");
        sb.append("Capacidades reais do produto:\n");
        sb.append(writeAsJson(productCapabilities)).append("\n");
        sb.append("Toda resposta deve respeitar essas capacidades. Não proponha nada fora delas.\n");
        return sb.toString();
    }

    private Map<String, Object> responseFormat() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of("nicheName", "nicheDescription", "hypothesisTitle", "persona", "problem", "promise", "uniqueMechanism"));
        schema.put("properties", Map.of(
                "nicheName", nullableStringSchema("Nome do nicho dentro das capacidades reais do produto"),
                "nicheDescription", nullableStringSchema("Descrição resumida do nicho"),
                "hypothesisTitle", nullableStringSchema("Título curto da hipótese"),
                "persona", nullableStringSchema("Persona ideal para a hipótese"),
                "problem", nullableStringSchema("Problema principal da persona"),
                "promise", nullableStringSchema("Promessa alinhada às capacidades reais"),
                "uniqueMechanism", nullableStringSchema("Mecanismo único coerente com as capacidades reais")
        ));
        return Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "success_product_niche_hypothesis",
                        "schema", schema,
                        "strict", true));
    }

    private Map<String, Object> nullableStringSchema(String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", List.of("string", "null"));
        schema.put("description", description);
        return schema;
    }

    private void appendJsonField(StringBuilder sb, String field, String value, boolean addComma) {
        sb.append("  \"").append(field).append("\": ");
        if (value == null) {
            sb.append("null");
        } else {
            sb.append(writeAsJson(value));
        }
        if (addComma) {
            sb.append(",");
        }
        sb.append("\n");
    }

    private void appendJsonObjectField(StringBuilder sb, String field, Map<String, Object> value, boolean addComma) {
        sb.append("  \"").append(field).append("\": ").append(writeAsJson(value));
        if (addComma) {
            sb.append(",");
        }
        sb.append("\n");
    }

    private String writeAsJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            log.warn("Could not serialize value to JSON for prompt", exception);
            return String.valueOf(value);
        }
    }

    private static String asText(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && !v.isNull() ? v.asText() : null;
    }

    private record ProductCapabilities(
            boolean canGenerateImages,
            boolean canGenerateEbooks,
            boolean canGenerateCopies,
            boolean canGenerateDigitalKits,
            boolean canGeneratePersonalizedSamples,
            boolean canDoLiveConsulting,
            boolean canDoManualReview,
            boolean canDoOngoingFollowup,
            boolean canManageAds,
            boolean canDoSalesCalls) {

        private Map<String, Object> asMap() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("canGenerateImages", canGenerateImages);
            data.put("canGenerateEbooks", canGenerateEbooks);
            data.put("canGenerateCopies", canGenerateCopies);
            data.put("canGenerateDigitalKits", canGenerateDigitalKits);
            data.put("canGeneratePersonalizedSamples", canGeneratePersonalizedSamples);
            data.put("canDoLiveConsulting", canDoLiveConsulting);
            data.put("canDoManualReview", canDoManualReview);
            data.put("canDoOngoingFollowup", canDoOngoingFollowup);
            data.put("canManageAds", canManageAds);
            data.put("canDoSalesCalls", canDoSalesCalls);
            return data;
        }
    }
}
