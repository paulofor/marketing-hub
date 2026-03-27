package com.marketinghub.worker.hypothesisframework;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.OpenAiCostEstimator;
import com.marketinghub.worker.openai.OpenAiResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class HypothesisFrameworkOpenAiClient {
    private static final Logger log = LoggerFactory.getLogger(HypothesisFrameworkOpenAiClient.class);

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final boolean enabled;

    public HypothesisFrameworkOpenAiClient(WebClient.Builder builder,
                                           ObjectMapper objectMapper,
                                           @Value("${openai.api-key:}") String apiKey,
                                           @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.enabled = StringUtils.hasText(apiKey);
        WebClient.Builder clientBuilder = builder.clone().baseUrl(baseUrl);
        if (enabled) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
        } else {
            log.warn("OPENAI_API_KEY não configurada; jobs de framework de hipótese ficarão pendentes");
        }
        this.webClient = clientBuilder.build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public HypothesisFrameworkJobCompletionPayload generate(HypothesisFrameworkJobDto job) {
        if (!enabled) {
            throw new IllegalStateException("OpenAI API key não configurada");
        }
        Map<String, Object> payload = null;
        try {
            payload = objectMapper.readValue(job.requestBodyJson(), new TypeReference<>() {
            });
            ensureJsonSchemaCompatibility(payload, job);
            log.info("Enviando requisição para OpenAI [jobId={}, hypothesisId={}, section={}, model={}]",
                    job.id(),
                    job.hypothesisId(),
                    job.section(),
                    job.model());
            log.debug("Payload OpenAI [jobId={}]: {}", job.id(), safeJson(payload));
            OpenAiResponse response = webClient.post()
                    .uri("/responses")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .block();
            if (response == null) {
                throw new IllegalStateException("Resposta vazia da OpenAI");
            }
            if (response.hasError()) {
                throw new IllegalStateException(response.errorMessage());
            }
            String content = response.firstText();
            if (!StringUtils.hasText(content)) {
                throw new IllegalStateException("Modelo não retornou conteúdo");
            }
            Integer inputTokens = response.usage() != null ? response.usage().effectiveInputTokens() : null;
            Integer outputTokens = response.usage() != null ? response.usage().effectiveOutputTokens() : null;
            return new HypothesisFrameworkJobCompletionPayload(
                    content,
                    content,
                    inputTokens,
                    outputTokens,
                    OpenAiCostEstimator.estimateUsd(job.model(), response.usage()));
        } catch (WebClientResponseException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();
            log.error("Falha HTTP ao chamar OpenAI [jobId={}, hypothesisId={}, section={}, model={}, status={}, responseBody={}]",
                    job.id(),
                    job.hypothesisId(),
                    job.section(),
                    job.model(),
                    statusCode.value(),
                    truncate(ex.getResponseBodyAsString()));
            log.debug("Payload da requisição com erro [jobId={}]: {}", job.id(), safeJson(payload));
            throw new IllegalStateException("Falha ao gerar seção " + job.section() + " para hipótese " + job.hypothesisId(), ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao gerar seção " + job.section() + " para hipótese " + job.hypothesisId(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void ensureJsonSchemaCompatibility(Map<String, Object> payload, HypothesisFrameworkJobDto job) {
        if (payload == null) {
            return;
        }
        Object textNode = payload.get("text");
        if (!(textNode instanceof Map<?, ?> textMapRaw)) {
            return;
        }
        Object formatNode = textMapRaw.get("format");
        if (!(formatNode instanceof Map<?, ?> formatMapRaw)) {
            return;
        }

        Map<String, Object> formatMap = (Map<String, Object>) formatMapRaw;
        String type = formatMap.get("type") instanceof String value ? value : null;
        if (!"json_schema".equals(type)) {
            return;
        }
        ensureJsonSchemaName(formatMap, job);
        Object schemaNode = formatMap.get("schema");
        if (schemaNode instanceof Map<?, ?> schemaMap) {
            normalizeRequiredForObjectSchemas((Map<String, Object>) schemaMap);
        }
    }

    private void ensureJsonSchemaName(Map<String, Object> formatMap, HypothesisFrameworkJobDto job) {
        Object name = formatMap.get("name");
        if (name instanceof String value && StringUtils.hasText(value)) {
            return;
        }
        String section = job != null && StringUtils.hasText(job.section())
                ? job.section().trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
                : "response";
        formatMap.put("name", "hypothesis_framework_" + section);
    }

    @SuppressWarnings("unchecked")
    private void normalizeRequiredForObjectSchemas(Map<String, Object> schema) {
        if (schema == null) {
            return;
        }
        if (isObjectSchema(schema)) {
            Object propertiesNode = schema.get("properties");
            if (propertiesNode instanceof Map<?, ?> propertiesRaw) {
                Map<String, Object> properties = (Map<String, Object>) propertiesRaw;
                mergeRequiredWithProperties(schema, properties.keySet());
                for (Object propertySchema : properties.values()) {
                    if (propertySchema instanceof Map<?, ?> nestedPropertySchema) {
                        normalizeRequiredForObjectSchemas((Map<String, Object>) nestedPropertySchema);
                    }
                }
            }
        }

        Object itemsNode = schema.get("items");
        if (itemsNode instanceof Map<?, ?> itemsMap) {
            normalizeRequiredForObjectSchemas((Map<String, Object>) itemsMap);
        }

        normalizeCombinator(schema.get("anyOf"));
        normalizeCombinator(schema.get("allOf"));
        normalizeCombinator(schema.get("oneOf"));

        Object defsNode = schema.get("$defs");
        if (defsNode instanceof Map<?, ?> defsMap) {
            for (Object nestedSchema : defsMap.values()) {
                if (nestedSchema instanceof Map<?, ?> nestedMap) {
                    normalizeRequiredForObjectSchemas((Map<String, Object>) nestedMap);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void normalizeCombinator(Object node) {
        if (!(node instanceof List<?> entries)) {
            return;
        }
        for (Object entry : entries) {
            if (entry instanceof Map<?, ?> entryMap) {
                normalizeRequiredForObjectSchemas((Map<String, Object>) entryMap);
            }
        }
    }

    private boolean isObjectSchema(Map<String, Object> schema) {
        Object type = schema.get("type");
        if (type instanceof String value) {
            return "object".equals(value);
        }
        if (type instanceof List<?> values) {
            return values.stream().anyMatch("object"::equals);
        }
        return false;
    }

    private void mergeRequiredWithProperties(Map<String, Object> schema, Set<String> propertyNames) {
        LinkedHashSet<String> requiredFields = new LinkedHashSet<>();
        Object requiredNode = schema.get("required");
        if (requiredNode instanceof List<?> requiredList) {
            requiredList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .forEach(requiredFields::add);
        }
        requiredFields.addAll(propertyNames);
        schema.put("required", new ArrayList<>(requiredFields));
    }

    private String safeJson(Object value) {
        if (value == null) {
            return "<null>";
        }
        try {
            return truncate(objectMapper.writeValueAsString(value));
        } catch (Exception ex) {
            return "<erro ao serializar payload: " + ex.getMessage() + ">";
        }
    }

    private String truncate(String text) {
        if (!StringUtils.hasText(text)) {
            return "<vazio>";
        }
        int maxLength = 3_000;
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... [truncated]";
    }
}
