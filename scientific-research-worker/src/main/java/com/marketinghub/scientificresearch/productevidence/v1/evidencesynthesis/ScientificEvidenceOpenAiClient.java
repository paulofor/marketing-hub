package com.marketinghub.scientificresearch.productevidence.v1.evidencesynthesis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.scientificresearch.config.ScientificResearchProperties;
import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Executa a chamada à OpenAI para síntese de evidências científicas.
 */
@Component
public class ScientificEvidenceOpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(ScientificEvidenceOpenAiClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final PromptLoader promptLoader;
    private final ScientificResearchProperties properties;
    private Map<String, Object> lastRequest = Map.of();
    private JsonNode lastResponse;

    /**
     * Configura o client da OpenAI para a etapa.
     */
    public ScientificEvidenceOpenAiClient(
            WebClient.Builder builder,
            ObjectMapper objectMapper,
            PromptLoader promptLoader,
            ScientificResearchProperties properties) {
        this.webClient = builder
                .baseUrl(properties.getOpenAiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + resolveOpenAiApiKey(properties))
                .build();
        this.objectMapper = objectMapper;
        this.promptLoader = promptLoader;
        this.properties = properties;
    }

    /**
     * Sintetiza evidências científicas em JSON validável.
     */
    public EvidenceSynthesisOutput synthesize(StageContext context) {
        String prompt = promptLoader.load("prompts/product-evidence/v1/evidence-synthesis.md")
                .replace("{{productIdea}}", safe(context.productIdea()))
                .replace("{{scientificQuestion}}", safe(context.scientificQuestion()))
                .replace("{{inputJson}}", toJson(context.input()));
        JsonNode schema = readSchema();

        Map<String, Object> textFormat = new LinkedHashMap<>();
        textFormat.put("format", Map.of(
                "type", "json_schema",
                "name", "scientific_evidence_synthesis",
                "strict", true,
                "schema", schema));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.getOpenAiModel());
        request.put("service_tier", "flex");
        request.put("input", List.of(
                Map.of("role", "system", "content", List.of(Map.of(
                        "type", "input_text",
                        "text", "Responda em português do Brasil e nunca invente evidências."))),
                Map.of("role", "user", "content", List.of(Map.of("type", "input_text", "text", prompt)))));
        request.put("text", textFormat);
        this.lastRequest = request;

        log.info(
                "Enviando síntese científica para OpenAI url=/responses model={} jobId={} request={}",
                properties.getOpenAiModel(),
                context.jobId(),
                request);
        JsonNode response = webClient.post()
                .uri("/responses")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(properties.getRequestTimeout());
        log.info("Resposta bruta OpenAI url=/responses jobId={} response={}", context.jobId(), response);
        this.lastResponse = response;
        String content = extractText(response);
        try {
            return objectMapper.readValue(content, EvidenceSynthesisOutput.class);
        } catch (Exception ex) {
            log.error("Falha ao validar JSON da síntese científica jobId={} response={}", context.jobId(), content, ex);
            throw new IllegalStateException("Resposta da IA fora do schema científico", ex);
        }
    }

    /**
     * Retorna o último request bruto enviado para auditoria.
     */
    public Map<String, Object> lastRequest() {
        return lastRequest;
    }

    /**
     * Retorna o último response bruto recebido para auditoria.
     */
    public JsonNode lastResponse() {
        return lastResponse;
    }

    /**
     * Lê o schema JSON versionado da etapa.
     */
    private JsonNode readSchema() {
        try {
            return objectMapper.readTree(promptLoader.load("prompts/product-evidence/v1/evidence-synthesis-schema.json"));
        } catch (Exception ex) {
            throw new IllegalStateException("Schema de síntese científica inválido", ex);
        }
    }

    /**
     * Extrai texto da resposta Responses API.
     */
    private String extractText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("Resposta vazia da OpenAI");
        }
        String outputText = response.path("output_text").asText("");
        if (!outputText.isBlank()) {
            return outputText;
        }
        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                String text = content.path("text").asText("");
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        throw new IllegalStateException("OpenAI não retornou texto de síntese");
    }

    /**
     * Serializa um objeto para JSON textual.
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível serializar entrada científica", ex);
        }
    }

    /**
     * Normaliza valores textuais nulos.
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * Resolve a chave OpenAI por variável direta ou arquivo de segredo montado no container.
     */
    private String resolveOpenAiApiKey(ScientificResearchProperties properties) {
        if (properties.getOpenAiApiKey() != null && !properties.getOpenAiApiKey().isBlank()) {
            return properties.getOpenAiApiKey().trim();
        }
        String keyFile = properties.getOpenAiApiKeyFile();
        if (keyFile == null || keyFile.isBlank()) {
            return "";
        }
        try {
            return Files.readString(Path.of(keyFile)).trim();
        } catch (Exception ex) {
            log.error("Falha ao ler arquivo de chave OpenAI no scientific-research-worker path={}", keyFile, ex);
            return "";
        }
    }
}
