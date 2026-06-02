package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Executa a chamada síncrona à OpenAI Responses API para gerar o seed e as queries da etapa dois. */
@Component
public class OpenAiNicheResearchSeedBuilderClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiNicheResearchSeedBuilderClient.class);
    private static final String RESPONSES_PATH = "/responses";
    private static final String SCHEMA_NAME = "oprm_niche_research_seed_builder";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final NicheResearchSeedBuilderOpenAiProperties properties;
    private final NicheResearchSeedBuilderPromptBuilder promptBuilder;
    private final NicheResearchSeedBuilderSchema schema;
    private final NicheResearchSeedBuilderValidator validator;

    /** Inicializa o client da OpenAI com dependências de prompt, schema e validação da etapa dois. */
    public OpenAiNicheResearchSeedBuilderClient(
            RestClient restClient,
            ObjectMapper objectMapper,
            NicheResearchSeedBuilderOpenAiProperties properties,
            NicheResearchSeedBuilderPromptBuilder promptBuilder,
            NicheResearchSeedBuilderSchema schema,
            NicheResearchSeedBuilderValidator validator) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.schema = schema;
        this.validator = validator;
    }

    /** Gera seed e queries por IA, extrai o JSON estruturado e valida regras de contrato antes do retorno. */
    public OpenAiSeedBuilderResult generate(NicheResearchSeedBuilderPending input) {
        if (properties.apiKey().isBlank()) {
            throw new IllegalStateException("OPRM nichocnae seed builder OpenAI API key não configurada.");
        }

        String prompt = promptBuilder.buildPrompt(input);
        Map<String, Object> requestBody = buildRequestBody(prompt);
        String url = properties.baseUrl() + RESPONSES_PATH;
        try {
            Map<String, Object> raw = responseBody(url, requestBody);
            if (raw == null) {
                throw new IllegalStateException("OpenAI retornou corpo vazio para a etapa dois OPRM nichocnae.");
            }
            String rawModelResponse = extractModelResponse(raw);
            NicheResearchSeedBuilderOutput output = objectMapper.readValue(rawModelResponse, NicheResearchSeedBuilderOutput.class);
            validator.validate(input, output);
            return new OpenAiSeedBuilderResult(
                    output,
                    rawModelResponse,
                    extractInteger(raw, "usage", "input_tokens"),
                    extractInteger(raw, "usage", "output_tokens"),
                    stringValue(raw.get("id")),
                    properties.model());
        } catch (RestClientException | JsonProcessingException | IllegalStateException | IllegalArgumentException ex) {
            log.error(
                    "Erro ao gerar seed da etapa dois OPRM nichocnae com OpenAI (endpoint={}, researchCycleId={}, cnaeCode={})",
                    url,
                    input.researchCycleId(),
                    input.cnaeCode(),
                    ex);
            throw new IllegalStateException("Falha ao gerar seed da etapa dois OPRM nichocnae.", ex);
        }
    }

    /** Executa a requisição HTTP e isola o cast seguro do corpo retornado pelo RestClient. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> responseBody(String url, Map<String, Object> requestBody) {
        Map<?, ?> response = restClient.post()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(Map.class);
        return response == null ? null : (Map<String, Object>) response;
    }

    /** Monta o corpo da Responses API com schema JSON estrito para evitar saída ambígua ou fora do contrato. */
    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", SCHEMA_NAME);
        format.put("schema", schema.buildSchema());
        format.put("strict", true);

        Map<String, Object> text = new LinkedHashMap<>();
        text.put("format", format);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.model());
        body.put("input", prompt);
        body.put("text", text);
        return body;
    }

    /** Extrai o texto principal da resposta da OpenAI, aceitando output_text ou conteúdo textual estruturado. */
    private String extractModelResponse(Map<String, Object> raw) {
        Object outputText = raw.get("output_text");
        if (outputText != null && !outputText.toString().isBlank()) {
            return outputText.toString();
        }

        Object output = raw.get("output");
        if (output instanceof List<?> list) {
            StringBuilder builder = new StringBuilder();
            for (Object item : list) {
                appendOutputItemText(builder, item);
            }
            if (!builder.isEmpty()) {
                return builder.toString();
            }
        }

        throw new IllegalStateException("Não foi possível extrair texto da resposta OpenAI da etapa dois.");
    }

    /** Acrescenta textos encontrados em um item de output da Responses API ao buffer de resposta. */
    private void appendOutputItemText(StringBuilder builder, Object item) {
        if (!(item instanceof Map<?, ?> itemMap)) {
            return;
        }
        Object content = itemMap.get("content");
        if (!(content instanceof List<?> contentList)) {
            return;
        }
        for (Object contentItem : contentList) {
            if (contentItem instanceof Map<?, ?> contentMap) {
                Object text = contentMap.get("text");
                if (text != null) {
                    builder.append(text);
                }
            }
        }
    }

    /** Extrai contadores inteiros de uso da resposta para auditoria de custo e volume. */
    private Integer extractInteger(Map<String, Object> raw, String parentKey, String childKey) {
        Object parent = raw.get(parentKey);
        if (!(parent instanceof Map<?, ?> map)) {
            return null;
        }
        Object value = map.get(childKey);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        return null;
    }

    /** Converte qualquer valor opcional de metadado da OpenAI para texto simples. */
    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
