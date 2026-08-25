package com.marketinghub.videomanagement.referenceanalysisv1.pipeline.analyze;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.referenceanalysisv1.pipeline.ReferenceAnalysisStageContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Monta, executa e audita a leitura multimodal de referências sob direção de Apolo. */
@Component
public class ReferenceAnalysisAiClient {
    private static final Logger log = LoggerFactory.getLogger(ReferenceAnalysisAiClient.class);
    private static final String PROMPT_PATH = "prompts/apollo/reference-analysis/v1/analyze.md";
    private static final String SCHEMA_PATH = "prompts/apollo/reference-analysis/v1/analyze-schema.json";
    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient openAi;

    /** Configura o cliente externo exclusivamente dentro da etapa concreta analyze. */
    public ReferenceAnalysisAiClient(VideoManagementProperties properties,
                                     ObjectMapper objectMapper,
                                     WebClient.Builder builder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.openAi = builder.baseUrl(properties.getReferenceAnalysis().getOpenAiBaseUrl().toString()).build();
    }

    /** Envia métricas e contact sheets no modo Flex e devolve request e response brutos. */
    public AiInteraction analyze(ReferenceAnalysisStageContext context,
                                 ReferenceMediaInspector.Evidence evidence) {
        String key = resolveApiKey();
        if (!StringUtils.hasText(key)) {
            throw new IllegalStateException("Credencial OpenAI ausente para análise de referência");
        }
        ObjectNode request = request(context, evidence);
        String url = "/responses";
        try {
            log.info("Request OpenAI análise de referência; executionId={} url={} payload={}",
                    context.executionId(), url, request);
            JsonNode response = openAi.post().uri(url).contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + key).bodyValue(request).retrieve()
                    .bodyToMono(JsonNode.class).block();
            log.info("Response OpenAI análise de referência; executionId={} url={} response={}",
                    context.executionId(), url, response);
            return new AiInteraction(request, response);
        } catch (WebClientResponseException ex) {
            log.error("Falha OpenAI na análise de referência; executionId={} url={}",
                    context.executionId(), url, ex);
            throw new AiFailure("OpenAI rejeitou a análise de referência", ex, request,
                    errorResponse(ex));
        } catch (RuntimeException ex) {
            log.error("Falha OpenAI na análise de referência; executionId={} url={}",
                    context.executionId(), url, ex);
            throw new AiFailure("OpenAI não concluiu a análise de referência", ex, request, null);
        }
    }

    /** Monta o contrato multimodal com prompt e schema integralmente versionados. */
    private ObjectNode request(ReferenceAnalysisStageContext context,
                               ReferenceMediaInspector.Evidence evidence) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", properties.getReferenceAnalysis().getModel());
        request.put("service_tier", "flex");
        request.put("store", false);
        request.put("max_output_tokens", properties.getReferenceAnalysis().getMaxOutputTokens());
        ArrayNode input = request.putArray("input");
        ObjectNode message = input.addObject();
        message.put("role", "user");
        ArrayNode content = message.putArray("content");
        ObjectNode text = content.addObject();
        text.put("type", "input_text");
        text.put("text", resource(PROMPT_PATH)
                .replace("{{REFERENCE}}", context.input().toPrettyString())
                .replace("{{TECHNICAL_EVIDENCE}}", evidence.artifacts().toPrettyString()));
        for (String dataUrl : evidence.contactSheetDataUrls()) {
            ObjectNode image = content.addObject();
            image.put("type", "input_image");
            image.put("image_url", dataUrl);
            image.put("detail", "high");
        }
        ObjectNode format = request.putObject("text").putObject("format");
        format.put("type", "json_schema");
        format.put("name", "apollo_reference_analysis_v1");
        format.put("strict", true);
        format.set("schema", readJson(resource(SCHEMA_PATH)));
        request.set("metadata", objectMapper.valueToTree(Map.of(
                "agent", "APOLLO",
                "pipeline", "reference-analysis-v1",
                "executionId", String.valueOf(context.executionId()),
                "referenceId", String.valueOf(context.referenceId()))));
        return request;
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
            log.error("Falha ao ler secret OpenAI da análise de referência; arquivo={}", file, ex);
            return null;
        }
    }

    /** Carrega integralmente os contratos versionados do classpath. */
    private String resource(String path) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Recurso ausente: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("Falha ao carregar contrato da análise de referência; recurso={}", path, ex);
            throw new IllegalStateException("Contrato da análise de referência indisponível", ex);
        }
    }

    /** Converte o schema versionado em JSON sem tolerar contrato inválido. */
    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (IOException ex) {
            log.error("Falha ao interpretar schema da análise de referência", ex);
            throw new IllegalStateException("Schema da análise de referência é inválido", ex);
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
            log.warn("Resposta de erro OpenAI não é JSON; status={}", error.getStatusCode().value(), ex);
            result.put("body", body);
        }
        return result;
    }

    /** Preserva exatamente o request enviado e a response bruta recebida. */
    public record AiInteraction(ObjectNode request, JsonNode response) { }

    /** Falha de integração acompanhada do request e da resposta bruta disponíveis. */
    public static class AiFailure extends RuntimeException {
        private final ObjectNode request;
        private final JsonNode response;

        /** Encapsula uma falha externa sem perder a auditoria da tentativa. */
        public AiFailure(String message, Throwable cause, ObjectNode request, JsonNode response) {
            super(message, cause);
            this.request = request;
            this.response = response;
        }

        /** Devolve o request efetivamente montado para o modelo. */
        public ObjectNode request() {
            return request;
        }

        /** Devolve a resposta externa disponível, inclusive erros estruturados. */
        public JsonNode response() {
            return response;
        }
    }
}
