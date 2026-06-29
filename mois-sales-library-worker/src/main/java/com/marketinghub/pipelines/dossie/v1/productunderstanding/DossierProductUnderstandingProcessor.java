package com.marketinghub.pipelines.dossie.v1.productunderstanding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** Executa a etapa entendimento do produto cumprindo o objetivo funcional contratado para o dossiê. */
@Slf4j
public class DossierProductUnderstandingProcessor implements StageProcessor {

    private static final String STAGE_NAME = "product-understanding";
    private static final String OBJECTIVE = "Estruturar produto, público, dores, promessa, mecanismo, oferta, prova, objeções, urgência e hipótese de sucesso antes da pesquisa externa.";
    private static final String PROMPT_PATH = "prompts/dossieproduto/v1/product-understanding/prompt.md";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient openAiClient;
    private final OpenAiProperties openAiProperties;

    /** Cria a etapa em modo local para testes e ambientes sem integração OpenAI configurada. */
    public DossierProductUnderstandingProcessor() {
        this.openAiClient = null;
        this.openAiProperties = null;
    }

    /** Cria a etapa com cliente OpenAI Responses Flex para gerar auditoria bruta e saída funcional. */
    public DossierProductUnderstandingProcessor(RestClient.Builder builder, OpenAiProperties openAiProperties) {
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

    /** Informa o nome canônico da etapa entendimento do produto. */
    @Override
    public String stageName() {
        return STAGE_NAME;
    }

    /** Produz saída funcional auditável alinhada ao objetivo da etapa. */
    @Override
    public StageResult process(StageContext context) {
        if (openAiClient != null && openAiProperties != null && StringUtils.hasText(openAiProperties.resolvedApiKey())) {
            return processWithOpenAi(context);
        }
        return processLocally(context);
    }

    /** Executa a etapa usando OpenAI e preserva request, response, texto final, modelo e tokens para auditoria. */
    private StageResult processWithOpenAi(StageContext context) {
        try {
            String rawRequest = buildOpenAiRequest(context);
            log.info("MOIS dossie v1 product-understanding enviando request cru para OpenAI. jobId={}, requestPayload={}",
                    context.stageExecutionId(), rawRequest);
            String rawResponse = openAiClient.post().uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(rawRequest)
                    .retrieve()
                    .body(String.class);
            if (!StringUtils.hasText(rawResponse)) {
                throw new IllegalStateException("OpenAI Responses Flex retornou corpo vazio em product-understanding");
            }
            log.info("MOIS dossie v1 product-understanding recebeu resposta crua da OpenAI. jobId={}, rawResponse={}",
                    context.stageExecutionId(), rawResponse);
            JsonNode root = objectMapper.readTree(rawResponse);
            String outputText = extractOutputText(root);
            JsonNode usage = root.path("usage");
            Integer inputTokens = nullableInt(usage.path("input_tokens"));
            Integer outputTokens = nullableInt(usage.path("output_tokens"));
            String model = root.path("model").asText(openAiProperties.normalizedModel());
            Map<String, Object> evidence = DossierStageSupport.evidenceFor(context, STAGE_NAME);
            DossierProductUnderstandingOutput output = new DossierProductUnderstandingOutput(
                    context.dossierId(),
                    "OBJECTIVE_FULFILLED",
                    outputText,
                    evidence);
            return StageResult.doneWithOpenAiInteractions(
                    Map.of(STAGE_NAME, output, "openAiFinalText", outputText),
                    List.of(DossierStageSupport.objectiveArtifact(context, STAGE_NAME, outputText, evidence)),
                    List.of(new OpenAiInteraction(rawRequest, rawResponse, inputTokens, outputTokens, null, model, null)));
        } catch (Exception ex) {
            log.error("Falha na etapa product-understanding com OpenAI. stageExecutionId={}, dossierId={}",
                    context.stageExecutionId(), context.dossierId(), ex);
            return StageResult.failed("Falha ao entender produto via OpenAI: " + ex.getMessage(), Map.of(), List.of());
        }
    }

    /** Monta o request Responses Flex enviado diretamente à OpenAI para entender o produto. */
    private String buildOpenAiRequest(StageContext context) throws Exception {
        String prompt = loadPrompt().replace("{{context}}", objectMapper.writeValueAsString(context.input()));
        return objectMapper.writeValueAsString(Map.of(
                "model", openAiProperties.normalizedModel(),
                "service_tier", "flex",
                "metadata", Map.of(
                        "stage", STAGE_NAME,
                        "stage_execution_id", Long.toString(context.stageExecutionId()),
                        "dossier_id", Long.toString(context.dossierId())),
                "input", List.of(
                        Map.of("role", "system", "content", "Você estrutura entendimento comercial de produto, protege o dossiê contra inferência sem evidência e responde exclusivamente JSON válido sem markdown."),
                        Map.of("role", "user", "content", prompt)),
                "text", Map.of("format", Map.of("type", "json_object"))));
    }

    /** Carrega o prompt versionado da etapa para evitar contrato hardcoded na classe Java. */
    private String loadPrompt() throws IOException {
        return new ClassPathResource(PROMPT_PATH).getContentAsString(StandardCharsets.UTF_8);
    }

    /** Extrai o texto final retornado pelo endpoint Responses da OpenAI. */
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
        throw new IllegalStateException("OpenAI Responses Flex não retornou texto final em product-understanding");
    }

    /** Lê um contador de tokens preservando nulo quando o campo não foi enviado pela OpenAI. */
    private Integer nullableInt(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asInt();
    }

    /** Mantém a saída local anterior quando a integração OpenAI não está configurada. */
    private StageResult processLocally(StageContext context) {
        Map<String, Object> evidence = DossierStageSupport.evidenceFor(context, STAGE_NAME);
        DossierProductUnderstandingOutput output = new DossierProductUnderstandingOutput(
                context.dossierId(),
                "OBJECTIVE_FULFILLED",
                OBJECTIVE,
                evidence);
        return StageResult.done(
                Map.of(STAGE_NAME, output),
                List.of(DossierStageSupport.objectiveArtifact(context, STAGE_NAME, OBJECTIVE, evidence)));
    }
}
