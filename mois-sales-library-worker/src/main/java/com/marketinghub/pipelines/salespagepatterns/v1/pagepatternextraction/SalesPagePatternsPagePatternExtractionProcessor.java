package com.marketinghub.pipelines.salespagepatterns.v1.pagepatternextraction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.openai.OpenAiServiceTierRetryPolicy;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.pageanalysis.OpenAiProperties;
import com.marketinghub.pipelines.dossie.v1.DossierStageSupport;
import com.marketinghub.pipelines.dossie.v1.StageContext;
import com.marketinghub.pipelines.dossie.v1.StageProcessor;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import com.marketinghub.pipelines.dossie.v1.StageResult.OpenAiInteraction;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** Executa a extração de padrões de design, visual e copy do pipeline salespagepatterns.v1. */
@Slf4j
public class SalesPagePatternsPagePatternExtractionProcessor implements StageProcessor {

    private static final String STAGE_NAME = "page-pattern-extraction";
    private static final String PROMPT_PATH = "prompts/salespagepatterns/v1/page-pattern-extraction/prompt.md";
    private static final String SCHEMA_PATH = "prompts/salespagepatterns/v1/page-pattern-extraction/schema.json";
    private static final String LOCAL_OBJECTIVE = "Extrair padrões abstratos de estrutura, visual, copy, prova, oferta e CTA sem copiar conteúdo externo.";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient openAiClient;
    private final OpenAiProperties openAiProperties;

    /** Cria o processor em modo local para testes e ambientes sem chave OpenAI. */
    public SalesPagePatternsPagePatternExtractionProcessor() {
        this.openAiClient = null;
        this.openAiProperties = null;
    }

    /** Cria o processor com cliente OpenAI configurado para execução auditável. */
    public SalesPagePatternsPagePatternExtractionProcessor(RestClient.Builder builder, OpenAiProperties openAiProperties) {
        this.openAiProperties = openAiProperties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(30));
        requestFactory.setReadTimeout(Duration.ofMillis(openAiProperties.normalizedRequestTimeoutMs()));
        this.openAiClient = builder.requestFactory(requestFactory)
                .baseUrl(openAiProperties.normalizedBaseUrl())
                .defaultHeader("Authorization", "Bearer " + openAiProperties.resolvedApiKey())
                .defaultHeader("OpenAI-Beta", "reasoning=1")
                .build();
    }

    /** Informa a etapa canônica executada por este processor. */
    @Override
    public String stageName() {
        return STAGE_NAME;
    }

    /** Executa a extração com OpenAI quando configurada, ou resposta local rastreável em testes. */
    @Override
    public StageResult process(StageContext context) {
        if (openAiClient != null && openAiProperties != null && StringUtils.hasText(openAiProperties.resolvedApiKey())) {
            return processWithOpenAi(context);
        }
        return processLocally(context);
    }

    /** Executa OpenAI preservando request, response, tokens e modelo para auditoria no backend. */
    private StageResult processWithOpenAi(StageContext context) {
        try {
            OpenAiCallResult callResult = executeOpenAiWithCanonicalRetry(context);
            if (!StringUtils.hasText(callResult.rawResponse())) {
                throw new IllegalStateException("OpenAI retornou corpo vazio em salespagepatterns.v1");
            }
            JsonNode root = objectMapper.readTree(callResult.rawResponse());
            String outputText = extractOutputText(root);
            JsonNode usage = root.path("usage");
            Integer inputTokens = nullableInt(usage.path("input_tokens"));
            Integer outputTokens = nullableInt(usage.path("output_tokens"));
            String model = root.path("model").asText(openAiProperties.normalizedModel());
            Map<String, Object> evidence = DossierStageSupport.evidenceFor(context, STAGE_NAME);
            SalesPagePatternsPagePatternExtractionOutput output = new SalesPagePatternsPagePatternExtractionOutput(
                    context.dossierId(), "OBJECTIVE_FULFILLED", outputText, evidence);
            return StageResult.doneWithOpenAiInteractions(
                    Map.of(STAGE_NAME, output, "openAiFinalText", outputText),
                    List.of(DossierStageSupport.objectiveArtifact(context, STAGE_NAME, outputText, evidence)),
                    List.of(new OpenAiInteraction(callResult.rawRequest(), callResult.rawResponse(), inputTokens, outputTokens, null, model, null)));
        } catch (Exception ex) {
            log.error("Falha na extração salespagepatterns.v1. stageExecutionId={}, pageId={}",
                    context.stageExecutionId(), context.dossierId(), ex);
            return StageResult.failed("Falha ao extrair padrões da página: " + ex.getMessage(), Map.of(), List.of());
        }
    }

    /** Monta o request Responses API com prompt e schema versionados. */
    private String buildOpenAiRequest(StageContext context, int attempt) throws Exception {
        String prompt = loadPrompt().replace("{{context}}", objectMapper.writeValueAsString(context.input()));
        JsonNode schema = objectMapper.readTree(loadSchema());
        ObjectNode request = objectMapper.valueToTree(Map.of(
                "model", openAiProperties.normalizedModel(),
                "metadata", Map.of(
                        "pipeline", "salespagepatterns.v1",
                        "stage", STAGE_NAME,
                        "stage_execution_id", Long.toString(context.stageExecutionId()),
                        "page_id", Long.toString(context.dossierId()),
                        "openai_attempt", Integer.toString(attempt),
                        "service_tier_effective", OpenAiServiceTierRetryPolicy.serviceTierForAttempt(attempt)),
                "input", List.of(
                        Map.of("role", "system", "content", "Você extrai padrões comerciais reutilizáveis de páginas de venda vencedoras sem copiar texto, marca ou layout literal. Responda somente JSON válido."),
                        Map.of("role", "user", "content", prompt)),
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "sales_page_patterns_v1",
                        "schema", schema,
                        "strict", true))));
        if (!OpenAiServiceTierRetryPolicy.shouldOmitServiceTier(attempt)) {
            request.put("service_tier", OpenAiServiceTierRetryPolicy.serviceTierForAttempt(attempt));
        }
        return objectMapper.writeValueAsString(request);
    }

    /** Executa duas tentativas Flex e uma tentativa Standard/default, conforme regra canônica. */
    private OpenAiCallResult executeOpenAiWithCanonicalRetry(StageContext context) throws Exception {
        RuntimeException lastFailure = null;
        String lastRequest = null;
        for (int attempt = 1; attempt <= OpenAiServiceTierRetryPolicy.MAX_ATTEMPTS; attempt++) {
            lastRequest = buildOpenAiRequest(context, attempt);
            String tier = OpenAiServiceTierRetryPolicy.serviceTierForAttempt(attempt);
            try {
                log.info("MOIS salespagepatterns.v1 enviando request OpenAI. jobId={}, attempt={}, serviceTier={}, requestPayload={}",
                        context.stageExecutionId(), attempt, tier, lastRequest);
                String rawResponse = openAiClient.post().uri("/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(lastRequest)
                        .retrieve()
                        .body(String.class);
                return new OpenAiCallResult(lastRequest, rawResponse);
            } catch (RuntimeException ex) {
                lastFailure = ex;
                log.warn("Falha transitória OpenAI salespagepatterns.v1. jobId={}, attempt={}, serviceTier={}",
                        context.stageExecutionId(), attempt, tier, ex);
            }
        }
        throw lastFailure == null ? new IllegalStateException("OpenAI não executou nenhuma tentativa") : lastFailure;
    }

    /** Guarda o request e response brutos da chamada OpenAI. */
    private record OpenAiCallResult(String rawRequest, String rawResponse) {
    }

    /** Carrega o prompt versionado da etapa. */
    private String loadPrompt() throws IOException {
        return new ClassPathResource(PROMPT_PATH).getContentAsString(StandardCharsets.UTF_8);
    }

    /** Carrega o schema JSON versionado da etapa. */
    private String loadSchema() throws IOException {
        return new ClassPathResource(SCHEMA_PATH).getContentAsString(StandardCharsets.UTF_8);
    }

    /** Extrai o texto final retornado pela Responses API. */
    private String extractOutputText(JsonNode root) {
        JsonNode outputText = root.path("output_text");
        if (!outputText.isMissingNode() && StringUtils.hasText(outputText.asText())) {
            return outputText.asText();
        }
        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (content.isArray()) {
                    for (JsonNode contentItem : content) {
                        JsonNode text = contentItem.path("text");
                        if (!text.isMissingNode() && StringUtils.hasText(text.asText())) {
                            return text.asText();
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("OpenAI não retornou texto final em salespagepatterns.v1");
    }

    /** Lê tokens como inteiro nulo quando a OpenAI não informa uso. */
    private Integer nullableInt(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asInt();
    }

    /** Mantém saída funcional local para testes sem integração OpenAI. */
    private StageResult processLocally(StageContext context) {
        Map<String, Object> evidence = DossierStageSupport.evidenceFor(context, STAGE_NAME);
        SalesPagePatternsPagePatternExtractionOutput output = new SalesPagePatternsPagePatternExtractionOutput(
                context.dossierId(), "OBJECTIVE_FULFILLED", LOCAL_OBJECTIVE, evidence);
        return StageResult.done(
                Map.of(STAGE_NAME, output),
                List.of(DossierStageSupport.objectiveArtifact(context, STAGE_NAME, LOCAL_OBJECTIVE, evidence)));
    }
}
