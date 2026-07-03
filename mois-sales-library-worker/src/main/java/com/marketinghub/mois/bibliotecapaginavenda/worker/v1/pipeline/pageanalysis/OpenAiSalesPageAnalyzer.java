package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.pageanalysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.openai.OpenAiServiceTierRetryPolicy;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Executa a análise comercial de páginas de venda via OpenAI Responses e normaliza o JSON retornado.
 */
@Component
@Slf4j
public class OpenAiSalesPageAnalyzer {

    private final RestClient restClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Configura o cliente OpenAI usando as propriedades operacionais do worker.
     */
    public OpenAiSalesPageAnalyzer(RestClient.Builder builder, OpenAiProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(30));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.normalizedRequestTimeoutMs()));
        this.restClient = builder.requestFactory(requestFactory).baseUrl(properties.normalizedBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.resolvedApiKey())
                .defaultHeader("OpenAI-Beta", "reasoning=1")
                .build();
    }

    /**
     * Envia o texto capturado da página para análise comercial com retry Flex/Flex/Standard e retorna o diagnóstico estruturado.
     */
    public SalesPageAnalysisResult analyze(long jobId, long pageId, String canonicalUrl, String htmlBodyText) {
        if (!StringUtils.hasText(properties.resolvedApiKey())) {
            throw new IllegalStateException("OpenAI api key não configurada para análise de sales page");
        }
        OpenAiCallResult callResult = executeResponsesRequestWithCanonicalRetry(jobId, pageId, canonicalUrl, htmlBodyText);
        String requestPayload = callResult.requestPayload();
        String responsePayload = callResult.responsePayload();
        log.info("MOIS sales-library recebeu resposta crua da OpenAI. jobId={}, rawResponse={}", jobId, responsePayload);
        return parseResponsesOutput(responsePayload, requestPayload);
    }

    /**
     * Monta o JSON enviado diretamente ao endpoint Responses da OpenAI com o tier da tentativa.
     */
    String buildResponsesRequestPayload(long pageId, String canonicalUrl, String htmlBodyText, int attempt) {
        String prompt = "Analise a página de vendas para identificar por que este produto alcançou sucesso e devolva JSON válido com os campos: score_total (0-100), sections_json (objeto), copy_json (objeto), visual_json (objeto), image_json (objeto), geralanding_wireframe_json (objeto), geralanding_copy_json (objeto), geralanding_image_prompt_json (objeto), geralanding_design_preset_json (objeto), analysis_notes (texto curto). "
                + "A análise é diagnóstico de sucesso, não consultoria de melhoria: não inclua sugestões, recomendações, próximos passos, itens a adicionar/remover, nem chaves como recommended, suggestions, melhorias ou lacunas em nenhum campo. "
                + "No campo image_json, explique somente a função persuasiva das imagens existentes no fluxo real: densidade visual, repetição de depoimentos/antes-e-depois, provas visuais, risco assumido de poluição visual e como isso sustenta ou prejudica a clareza da oferta já vencedora. Nos campos geralanding_* extraia somente padrões observados que sirvam de insumo para o pipeline GeraLanding: wireframe deve mapear estrutura/seções/CTAs/formulário; copy deve mapear promessa, dor, mecanismo, prova, oferta e CTA; image_prompt deve mapear funções comerciais das imagens, tipo visual, objeção removida e padrão de prompt reaproveitável; design_preset deve mapear direção visual, hierarquia, CTAs, cards, mockups, mobile e confiança. Não proponha melhorias para a página analisada; descreva padrões vencedores observados como insumo reutilizável. "
                + "Use o eixo Dor → Resultado → Mecanismo → Prova → Oferta apenas para explicar a fórmula observada que parece ter levado à venda, nunca para propor mudanças. URL: "
                + canonicalUrl + "\nConteúdo e resumo visual: " + htmlBodyText;
        try {
            ObjectNode request = objectMapper.valueToTree(Map.of(
                    "model", properties.normalizedModel(),
                    "metadata", Map.of(
                            "page_id", Long.toString(pageId),
                            "openai_attempt", Integer.toString(attempt),
                            "service_tier_effective", OpenAiServiceTierRetryPolicy.serviceTierForAttempt(attempt)),
                    "input", List.of(
                            Map.of("role", "system", "content", "Você analisa páginas de venda e responde exclusivamente em JSON válido sem markdown."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "text", Map.of(
                            "format", Map.of("type", "json_object")
                    )
            ));
            if (!OpenAiServiceTierRetryPolicy.shouldOmitServiceTier(attempt)) {
                request.put("service_tier", OpenAiServiceTierRetryPolicy.serviceTierForAttempt(attempt));
            }
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar request Responses OpenAI", e);
        }
    }

    /**
     * Executa a chamada síncrona ao endpoint Responses usando duas tentativas Flex e terceira Standard/default.
     */
    private OpenAiCallResult executeResponsesRequestWithCanonicalRetry(long jobId, long pageId, String canonicalUrl, String htmlBodyText) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= OpenAiServiceTierRetryPolicy.MAX_ATTEMPTS; attempt++) {
            String requestPayload = buildResponsesRequestPayload(pageId, canonicalUrl, htmlBodyText, attempt);
            String tier = OpenAiServiceTierRetryPolicy.serviceTierForAttempt(attempt);
            try {
                log.info("MOIS sales-library enviando request cru para OpenAI. jobId={}, attempt={}, serviceTier={}, requestPayload={}",
                        jobId, attempt, tier, requestPayload);
                String response = restClient.post().uri("/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestPayload)
                        .retrieve()
                        .body(String.class);
                if (!StringUtils.hasText(response)) {
                    throw new IllegalStateException("OpenAI Responses retornou corpo vazio");
                }
                return new OpenAiCallResult(requestPayload, response);
            } catch (RuntimeException ex) {
                lastFailure = ex;
                log.warn("Falha transitória OpenAI sales-library. jobId={}, attempt={}, serviceTier={}",
                        jobId, attempt, tier, ex);
            }
        }
        throw lastFailure == null ? new IllegalStateException("OpenAI não executou nenhuma tentativa") : lastFailure;
    }

    /** Guarda o request e a response brutos da tentativa OpenAI bem-sucedida. */
    private record OpenAiCallResult(String requestPayload, String responsePayload) {
    }

    /**
     * Interpreta o JSON de saída da OpenAI e devolve o resultado estruturado da análise.
     */
    private SalesPageAnalysisResult parseResponsesOutput(String responsePayloadJson, String requestPayloadJson) {
        try {
            JsonNode root = objectMapper.readTree(responsePayloadJson);
            JsonNode error = root.path("error");
            if (!error.isMissingNode() && !error.isNull()) {
                throw new IllegalStateException(buildResponsesErrorMessage(root));
            }
            JsonNode outputText = findOutputText(root);
            if (outputText.isMissingNode() || outputText.isNull() || !StringUtils.hasText(outputText.asText())) {
                throw new IllegalStateException("OpenAI Responses Flex não retornou texto de saída");
            }
            JsonNode parsed = objectMapper.readTree(outputText.asText());
            JsonNode usage = root.path("usage");
            Integer inputTokens = nullableInt(usage.path("input_tokens"));
            Integer outputTokens = nullableInt(usage.path("output_tokens"));
            return new SalesPageAnalysisResult(
                    parsed.path("score_total").isNumber() ? parsed.path("score_total").decimalValue() : BigDecimal.ZERO,
                    objectMapper.writeValueAsString(parsed.path("sections_json")),
                    objectMapper.writeValueAsString(parsed.path("copy_json")),
                    objectMapper.writeValueAsString(parsed.path("visual_json")),
                    objectMapper.writeValueAsString(parsed.path("image_json")),
                    objectMapper.writeValueAsString(parsed.path("geralanding_wireframe_json")),
                    objectMapper.writeValueAsString(parsed.path("geralanding_copy_json")),
                    objectMapper.writeValueAsString(parsed.path("geralanding_image_prompt_json")),
                    objectMapper.writeValueAsString(parsed.path("geralanding_design_preset_json")),
                    parsed.path("analysis_notes").asText("Análise gerada via OpenAI Responses Flex"),
                    requestPayloadJson,
                    responsePayloadJson,
                    "html-v1",
                    "openai-flex-geralanding-insights-v2",
                    properties.normalizedModel(),
                    inputTokens,
                    outputTokens,
                    null
            );
        } catch (Exception e) {
            log.error("Falha parse output Responses Flex OpenAI. output={}", responsePayloadJson, e);
            throw new IllegalStateException("Falha ao interpretar output do Responses Flex OpenAI", e);
        }
    }

    /**
     * Localiza o texto final tanto no campo consolidado quanto na estrutura detalhada do Responses.
     */
    private JsonNode findOutputText(JsonNode root) {
        JsonNode outputText = root.path("output_text");
        if (!outputText.isMissingNode() && !outputText.isNull() && StringUtils.hasText(outputText.asText())) {
            return outputText;
        }
        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (!content.isArray()) {
                    continue;
                }
                for (JsonNode contentItem : content) {
                    JsonNode text = contentItem.path("text");
                    if (!text.isMissingNode() && !text.isNull() && StringUtils.hasText(text.asText())) {
                        return text;
                    }
                }
            }
        }
        return outputText;
    }

    /**
     * Lê um inteiro opcional do JSON de uso da OpenAI preservando nulo quando o campo não veio.
     */
    private Integer nullableInt(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asInt();
    }

    /**
     * Constrói mensagem legível para erro retornado pelo endpoint Responses.
     */
    private String buildResponsesErrorMessage(JsonNode root) {
        JsonNode error = root.path("error");
        String upstreamMessage = error.path("message").asText("");
        String upstreamType = error.path("type").asText("");
        String upstreamCode = error.path("code").asText("");
        return "OpenAI Responses Flex retornou erro"
                + " (type=" + upstreamType + ", code=" + upstreamCode + "): "
                + upstreamMessage;
    }
}
