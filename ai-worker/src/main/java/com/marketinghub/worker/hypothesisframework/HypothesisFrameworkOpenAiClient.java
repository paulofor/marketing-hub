package com.marketinghub.worker.hypothesisframework;

import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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
    private static final String FRAMEWORK_PROMPT_PREFIX = """
            Você está preenchendo campos de um framework comercial para o Marketing Hub.

            Seu trabalho não é apenas “completar campos”.
            Seu trabalho é produzir respostas úteis para vender melhor.

            Regras obrigatórias:
            1. Priorize sempre: DOR → RESULTADO → MECANISMO → PROVA → OFERTA.
            2. Trate ferramenta, volume, automação, IA, peças, criativos e templates como MEIOS, não como mensagem principal.
            3. Sempre escreva pensando na persona no nível de maturidade informado. Não sofistique demais o problema.
            4. Se houver dúvida entre uma resposta “tecnicamente sofisticada” e uma resposta “mais clara e vendável”, escolha a mais clara e vendável.
            5. O resultado deve ser expresso primeiro como transformação de negócio, percepção de valor, interesse, autoridade, captação ou previsibilidade — e só depois como mecanismo operacional.
            6. Evite jargão técnico desnecessário, especialmente linguagem de mídia paga avançada, salvo se a persona for explicitamente madura nisso.
            7. Não transforme mecanismo em promessa.
            8. Não invente entregáveis, provas ou métricas complexas demais para a realidade operacional da persona.
            9. Sempre reduza abstração: prefira linguagem que um decisor do nicho entenda em poucos segundos.
            10. Quando citar fontes, priorize fontes oficiais, documentação primária e referências amplamente reconhecidas. Evite usar Reddit, fóruns e blogs frágeis como sustentação principal.
            11. Se o campo estiver ligado à venda, escolha a opção com maior clareza comercial e menor atrito de entendimento.
            12. Se houver conflito entre “soar inteligente” e “soar vendável”, escolha o vendável.

            Nível de maturidade da persona:
            - Este público é de massa dentro do nicho, não um subgrupo avançado.
            - Assuma conhecimento baixo a moderado em marketing, tráfego pago e testes criativos.
            - A persona entende sintomas do problema, mas não necessariamente entende a causa técnica.
            - Portanto, descreva primeiro o problema como ela sente, e só depois explique a causa estrutural.
            - Evite linguagem como: leilão, learning phase, fragmentação, creative grind, saturação algorítmica, salvo se isso aparecer apenas como tradução simples e curta.

            Critérios de qualidade da resposta:
            - A resposta precisa ser útil para venda.
            - A resposta precisa ser compreensível para a persona.
            - A resposta precisa diferenciar claramente:
              - sintoma vs causa
              - resultado vs mecanismo
              - prova vs entregável
            - A resposta não pode depender de jargão para parecer boa.
            - A resposta deve soar como algo que pode ser usado por marketing, vendas e produto.
            - Antes de finalizar, verifique:
              1. Isso está claro para a persona?
              2. Isso comunica transformação antes de ferramenta?
              3. Isso está simples o suficiente para virar copy ou oferta?

            """;

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final boolean enabled;
    private final int retryMinOutputTokens;

    public HypothesisFrameworkOpenAiClient(WebClient.Builder builder,
                                           ObjectMapper objectMapper,
                                           @Value("${openai.api-key:}") String apiKey,
                                           @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                           @Value("${openai.hypothesis-framework.retry-min-output-tokens:3500}")
                                           int retryMinOutputTokens) {
        this.objectMapper = objectMapper;
        this.enabled = StringUtils.hasText(apiKey);
        this.retryMinOutputTokens = retryMinOutputTokens;
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

    public String prepareRequestPayloadForLog(HypothesisFrameworkJobDto job) {
        if (job == null || !StringUtils.hasText(job.requestBodyJson())) {
            return "<vazio>";
        }
        try {
            return toLogJson(preparePayload(job));
        } catch (Exception ex) {
            return truncate(job.requestBodyJson());
        }
    }

    public HypothesisFrameworkJobCompletionPayload generate(HypothesisFrameworkJobDto job) {
        if (!enabled) {
            throw new IllegalStateException("OpenAI API key não configurada");
        }
        Map<String, Object> payload = null;
        try {
            payload = preparePayload(job);
            return generateWithRetry(job, payload);
        } catch (WebClientResponseException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();
            log.error("Falha HTTP ao chamar OpenAI [jobId={}, hypothesisId={}, section={}, model={}, status={}, responseBody={}]",
                    job.id(),
                    job.hypothesisId(),
                    job.section(),
                    job.model(),
                    statusCode.value(),
                    truncate(ex.getResponseBodyAsString()));
            log.debug("Payload da requisição com erro [jobId={}]: {}", job.id(), toLogJson(payload));
            throw new IllegalStateException("Falha ao gerar seção " + job.section() + " para hipótese " + job.hypothesisId(), ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao gerar seção " + job.section() + " para hipótese " + job.hypothesisId(), ex);
        }
    }

    private HypothesisFrameworkJobCompletionPayload generateWithRetry(HypothesisFrameworkJobDto job, Map<String, Object> payload)
            throws Exception {
        log.info("Enviando requisição para OpenAI [jobId={}, hypothesisId={}, section={}, model={}]",
                job.id(),
                job.hypothesisId(),
                job.section(),
                job.model());
        log.debug("Payload OpenAI [jobId={}]: {}", job.id(), toLogJson(payload));
        OpenAiResponse response = callOpenAi(payload);
        try {
            return buildCompletionPayload(job, payload, response);
        } catch (InvalidJsonResponseException ex) {
            if (!ex.shouldRetry()) {
                throw ex;
            }
            Map<String, Object> retryPayload = copyPayload(payload);
            int appliedMaxOutputTokens = increaseMaxOutputTokens(retryPayload);
            log.warn(
                    "Resposta possivelmente truncada para framework [jobId={}, hypothesisId={}, section={}]. Reenviando com max_output_tokens={}",
                    job.id(),
                    job.hypothesisId(),
                    job.section(),
                    appliedMaxOutputTokens);
            OpenAiResponse retryResponse = callOpenAi(retryPayload);
            return buildCompletionPayload(job, retryPayload, retryResponse);
        }
    }

    private HypothesisFrameworkJobCompletionPayload buildCompletionPayload(
            HypothesisFrameworkJobDto job,
            Map<String, Object> payload,
            OpenAiResponse response) {
        String content = extractAndValidateJsonContent(response, job);
        Integer inputTokens = response.usage() != null ? response.usage().effectiveInputTokens() : null;
        Integer outputTokens = response.usage() != null ? response.usage().effectiveOutputTokens() : null;
        return new HypothesisFrameworkJobCompletionPayload(
                content,
                safeJson(response),
                safeJson(payload),
                inputTokens,
                outputTokens,
                OpenAiCostEstimator.estimateUsd(job.model(), response.usage()));
    }

    private Map<String, Object> preparePayload(HypothesisFrameworkJobDto job) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(job.requestBodyJson(), new TypeReference<>() {
        });
        prependFrameworkPromptPrefix(payload);
        ensureJsonSchemaCompatibility(payload, job);
        return payload;
    }

    private OpenAiResponse callOpenAi(Map<String, Object> payload) {
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
        return response;
    }

    private String extractAndValidateJsonContent(OpenAiResponse response, HypothesisFrameworkJobDto job) {
        String content = response.firstText();
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("Modelo não retornou conteúdo");
        }
        try {
            JsonNode node = objectMapper.readTree(content);
            if (node == null || !node.isObject()) {
                throw new IllegalStateException("Modelo retornou conteúdo JSON inválido para seção " + job.section());
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidJsonResponseException(
                    "Modelo retornou JSON inválido para seção " + job.section() + ". Resposta recebida: " + truncate(content),
                    ex,
                    shouldRetryForInvalidJson(response, ex, content));
        }
        return content;
    }

    private boolean shouldRetryForInvalidJson(OpenAiResponse response, Exception parseException, String content) {
        if (parseException instanceof JsonEOFException) {
            return true;
        }
        if (response != null && "incomplete".equalsIgnoreCase(response.status())) {
            return true;
        }
        return content != null && content.length() >= 2_500;
    }

    private Map<String, Object> copyPayload(Map<String, Object> payload) throws Exception {
        return objectMapper.readValue(objectMapper.writeValueAsString(payload), new TypeReference<>() {
        });
    }

    private int increaseMaxOutputTokens(Map<String, Object> payload) {
        Object currentValue = payload.get("max_output_tokens");
        int current = currentValue instanceof Number number ? number.intValue() : 0;
        int updated = Math.max(retryMinOutputTokens, current > 0 ? current * 2 : retryMinOutputTokens);
        payload.put("max_output_tokens", updated);
        return updated;
    }

    @SuppressWarnings("unchecked")
    private void prependFrameworkPromptPrefix(Map<String, Object> payload) {
        if (payload == null) {
            return;
        }
        Object inputNode = payload.get("input");
        if (!(inputNode instanceof List<?> inputList)) {
            return;
        }
        for (Object item : inputList) {
            if (!(item instanceof Map<?, ?> messageRaw)) {
                continue;
            }
            Map<String, Object> message = (Map<String, Object>) messageRaw;
            Object contentNode = message.get("content");
            if (contentNode instanceof String textContent) {
                message.put("content", withFrameworkPrefix(textContent));
                continue;
            }
            if (!(contentNode instanceof List<?> contentList)) {
                continue;
            }
            for (Object contentItem : contentList) {
                if (!(contentItem instanceof Map<?, ?> contentRaw)) {
                    continue;
                }
                Map<String, Object> content = (Map<String, Object>) contentRaw;
                Object textNode = content.get("text");
                if (textNode instanceof String text) {
                    content.put("text", withFrameworkPrefix(text));
                }
            }
        }
    }

    private String withFrameworkPrefix(String text) {
        if (!StringUtils.hasText(text)) {
            return FRAMEWORK_PROMPT_PREFIX;
        }
        if (text.startsWith(FRAMEWORK_PROMPT_PREFIX)) {
            return text;
        }
        return FRAMEWORK_PROMPT_PREFIX + text;
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
        return serializeJson(value, false);
    }

    private String toLogJson(Object value) {
        return serializeJson(value, true);
    }

    private String serializeJson(Object value, boolean shouldTruncate) {
        if (value == null) {
            return "<null>";
        }
        try {
            String serialized = objectMapper.writeValueAsString(value);
            return shouldTruncate ? truncate(serialized) : serialized;
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

    private static class InvalidJsonResponseException extends IllegalStateException {
        private final boolean shouldRetry;

        private InvalidJsonResponseException(String message, Throwable cause, boolean shouldRetry) {
            super(message, cause);
            this.shouldRetry = shouldRetry;
        }

        private boolean shouldRetry() {
            return shouldRetry;
        }
    }
}
