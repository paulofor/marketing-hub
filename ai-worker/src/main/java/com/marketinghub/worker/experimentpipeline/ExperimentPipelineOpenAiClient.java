package com.marketinghub.worker.experimentpipeline;

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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class ExperimentPipelineOpenAiClient {
    private static final Logger log = LoggerFactory.getLogger(ExperimentPipelineOpenAiClient.class);
    private static final String REQUIRED_TEXT_MODEL = "gpt-5.2";
    private static final int TRANSIENT_ERROR_MAX_ATTEMPTS = 3;
    private static final long TRANSIENT_ERROR_RETRY_DELAY_MS = 1_500L;
    private static final String PIPELINE_PROMPT_PREFIX = """
            Você cria ativos de campanha para o Marketing Hub.

            Regras globais:
            1. O anúncio e a landing devem ter a mesma promessa central.
            2. O CTA do anúncio deve combinar com a ação principal da landing.
            3. O material precisa caber no envelope real do produto:
               - pode entregar ativos digitais gerados por IA
               - não pode prometer consultoria, call, gestão humana ou acompanhamento manual
            4. Priorize clareza comercial:
               DOR → RESULTADO → MECANISMO → PROVA → AÇÃO
            5. Não transforme mecanismo em promessa principal.
            6. Não use jargão técnico desnecessário.
            7. O público é geral dentro do nicho, com baixa a moderada maturidade em marketing.
            8. Sempre escreva pensando em alta escala e geração automatizada.
            9. O anúncio deve ser rápido de entender.
            10. A landing deve aprofundar a promessa e reduzir ceticismo.

            """;
    private static final String CAMPAIGN_ANGLE_PROMPT_SUFFIX = """
            Contexto do nicho: {nicho}

            Dor consolidada:
            {dor_resumida}

            Resultado consolidado:
            {resultado_resumido}

            Mecanismo consolidado:
            {mecanismo_resumido}

            Prova consolidada:
            {prova_resumida}

            Envelope do produto:
            {envelope_produto}

            Tarefa:
            Crie a base estratégica de uma campanha Meta Ads + landing page para este produto.

            Regras:
            1. Escolha 1 dor principal e 1 transformação principal.
            2. A promessa central deve ser simples e rápida de entender.
            3. O anúncio deve abrir pela dor ou pelo resultado.
            4. A landing deve aprofundar a mesma promessa, sem mudar o ângulo.
            5. O CTA deve ser compatível com escala, por exemplo:
               - gerar amostra
               - preencher briefing
               - receber prévia
               - desbloquear kit
            6. Não proponha nada fora do envelope do produto.

            Formato esperado:
            JSON com:
            primaryPromise,
            primaryPain,
            mechanismSummary,
            proofSummary,
            cta,
            singleMindedPromise,
            primaryCTA,
            landingMatchLine,
            funnelStage,
            tone
            """;
    private static final String LANDING_COPY_PROMPT_SUFFIX = """
            Objetivo da landing:
            Continuar exatamente a promessa do anúncio clicado e levar o usuário ao mesmo CTA declarado no anúncio.

            Contexto mínimo disponível no prompt:
            - Ângulo completo do experimento
            - Headline do anúncio clicado
            - CTA aprovado para o anúncio/landing
            - landingMatchLine com a frase de continuidade

            Regras:
            1. Repita a mesma promessa no hero (hero.headline + hero.promise) e em pageGoal.
            2. messageMatchSource deve citar qual headline do anúncio está sendo espelhada e messageMatchNotes precisa explicar como cada seção mantém essa continuidade.
            3. hero.ctaLabel, primaryCTA e todos os ctaBlocks devem usar exatamente o mesmo texto do CTA do anúncio.
            4. bodySections precisa ter no mínimo quatro blocos cobrindo dor, mecanismo, prova e oferta; cada bloco deve preencher sectionType e sectionDependsOn (primaryPromise, mechanismSummary, proofSummary ou primaryCTA).
            5. ctaBlocks deve mapear onde cada CTA aparece (hero, mid, final, sticky ou inline) especificando ctaVariant, matchAdCta e messageMatchNotes.
            6. faq precisa trazer pelo menos três perguntas com objectionTag deixando claro qual objeção está sendo tratada.
            7. consistencyChecks deve listar no mínimo CTA_MATCH, PROMISE_MATCH e GOOGLE_LANDING_BEST_PRACTICES com status PASS/WARN/FAIL e detalhes.
            8. complianceNotes sempre reforça que a entrega é 100% digital (gerada por IA) e sem consultoria ou ligações.
            9. Texto direto, escaneável e sem jargão de tráfego.

            Formato obrigatório (JSON):
            - pageGoal,
            - messageMatchSource,
            - messageMatchNotes
            - primaryCTA
            - hero { eyebrow, headline, subheadline, promise, supportingCopy, proofBadge, microcopy, ctaLabel, ctaUrl, ctaMatchNotes }
            - bodySections[] com sectionId, sectionType, title, summary, bullets, copy, ctaSupport, sectionDependsOn, messageMatchNotes
            - ctaBlocks[] com placement, ctaVariant, ctaLabel, ctaUrl, matchAdCta, ctaSupport, messageMatchNotes
            - faq[] com question, answer, objectionTag
            - consistencyChecks[] com check, status (PASS/WARN/FAIL), details
            - complianceNotes
            """;

    private static final String LANDING_LAYOUT_PROMPT_SUFFIX = """
            Objetivo:
            Converter o copy aprovado em um wireframe textual, mobile-first e com message match obrigatório entre anúncio e landing.

            Insumos garantidos:
            - Promessa central (primaryPromise) + landingMatchLine
            - CTA aprovado (primaryCTA)
            - Hero/headline e seções principais já redigidas

            Regras:
            1. A estrutura deve deixar claro, logo no primeiro bloco, para qual nicho a página foi feita.
            2. pageGoal precisa deixar explícito qual ação a página deve gerar.
            3. variantLayoutId deve ser form-first, proof-first ou story-first.
            4. sectionOrder deve mapear cada bloco com sectionId, sectionName, objective, contentType (hero, form, split, proof, timeline, faq, cta), copySource, uiNotes, messageMatchDependency e sectionDependsOn.
            5. Cada bloco precisa informar mobilePriorityScore (1 a 10) e dropOffRisk (baixo, medio ou alto).
            6. Se houver CTA no bloco, preencher ctaSlot com hasCta=true, ctaLabel, ctaVariant (hero, mid, final, sticky ou inline), matchAdCta e notes.
            7. formPlacementNotes deve informar em quantos scrolls o formulário aparece e se há versão sticky.
            8. ctaPlacementNotes garante repetição literal do CTA aprovado em posições estratégicas.
            9. mobilePriorityNotes destaca o que aparece antes da rolagem.
            10. consistencyChecks precisa incluir CTA_MATCH e EXPERIENCE_CONTINUITY com status PASS/WARN/FAIL e detalhes.
            11. Não usar linguagem de consultoria e não criar estrutura genérica para qualquer mercado.
            12. Se a estrutura puder servir para qualquer nicho, reescreva até ficar específica para o nicho informado.

            Formato obrigatório (JSON):
            - pageGoal,
            - variantLayoutId
            - messageMatchSummary
            - sectionOrder[] conforme regras acima
            - mobilePriorityNotes
            - ctaPlacementNotes
            - formPlacementNotes
            - consistencyChecks[]
            """;
    private static final String LANDING_HTML_PROMPT_SUFFIX = """
            Objetivo:
            Unificar a copy e o wireframe aprovados em uma landing final pronta para uso no formulário do experimento.

            Regras:
            1. Entregar documento HTML completo com CSS e JavaScript embutidos.
            2. O CTA principal deve ser idêntico ao CTA aprovado nas etapas anteriores.
            3. O formulário deve ser mobile-first e conter nome, whatsapp e objetivo principal.
            4. Incluir validação de campos obrigatórios no JavaScript.
            5. Incluir bloco de compliance reforçando entrega digital via IA e sem consultoria.
            6. Não usar bibliotecas externas nem assets remotos.

            Formato obrigatório (JSON):
            - htmlDocument
            - summary
            - consistencyChecks[] com CTA_MATCH, PROMISE_MATCH e FORM_USABILITY
            """;


    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final boolean enabled;

    public ExperimentPipelineOpenAiClient(WebClient.Builder builder,
                                          ObjectMapper objectMapper,
                                          @Value("${openai.api-key:}") String apiKey,
                                          @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.enabled = StringUtils.hasText(apiKey);
        WebClient.Builder clientBuilder = builder.clone().baseUrl(baseUrl);
        if (enabled) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
        } else {
            log.warn("OPENAI_API_KEY não configurada; jobs de pipeline de experimento ficarão pendentes");
        }
        this.webClient = clientBuilder.build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ExperimentPipelineJobCompletionPayload generate(ExperimentPipelineJobDto job) {
        if (!enabled) {
            throw new IllegalStateException("OpenAI API key não configurada");
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(job.requestBodyJson(), new TypeReference<>() {});
            enrichPrompt(payload, job);
            String effectiveModel = enforceRequiredModel(payload, job);
            ensureJsonSchemaCompatibility(payload, job);
            log.info("Sending experiment pipeline job {} to OpenAI (experimentId={}, section={}, model={})",
                    job.id(), job.experimentId(), job.section(), effectiveModel);
            log.info("OpenAI payload completo para job {}: {}", job.id(),
                    objectMapper.writeValueAsString(payload));
            OpenAiResponse response = requestWithTransientRetries(payload, job);
            if (response == null || response.hasError()) {
                throw new IllegalStateException(response != null ? response.errorMessage() : "Resposta vazia da OpenAI");
            }
            log.info("Received OpenAI response for job {} (responseId={}, status={}, inputTokens={}, outputTokens={})",
                    job.id(),
                    response.id(),
                    response.status(),
                    response.usage() != null ? response.usage().effectiveInputTokens() : null,
                    response.usage() != null ? response.usage().effectiveOutputTokens() : null);
            String content = response.firstText();
            if (!StringUtils.hasText(content)) {
                throw new IllegalStateException("Resposta da OpenAI sem conteúdo JSON");
            }
            log.info("OpenAI content for job {}: {}", job.id(), content);
            Map<String, Object> parsed = objectMapper.readValue(content, new TypeReference<>() {});
            String sectionContent = objectMapper.writeValueAsString(parsed);
            Integer inputTokens = response.usage() != null ? response.usage().effectiveInputTokens() : null;
            Integer outputTokens = response.usage() != null ? response.usage().effectiveOutputTokens() : null;
            return new ExperimentPipelineJobCompletionPayload(
                    sectionContent,
                    objectMapper.writeValueAsString(response),
                    objectMapper.writeValueAsString(payload),
                    inputTokens,
                    outputTokens,
                    OpenAiCostEstimator.estimateUsd(effectiveModel, response.usage()));
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao gerar seção " + job.section() + " do experimento " + job.experimentId(), ex);
        }
    }

    private String enforceRequiredModel(Map<String, Object> payload, ExperimentPipelineJobDto job) {
        if (payload == null) {
            return REQUIRED_TEXT_MODEL;
        }
        String previousModel = payload.get("model") instanceof String value ? value : null;
        if (!REQUIRED_TEXT_MODEL.equals(previousModel)) {
            log.warn(
                    "Forçando modelo OpenAI {} para pipeline (jobId={}, experimento={}, seção={}, modeloOriginal={})",
                    REQUIRED_TEXT_MODEL,
                    job != null ? job.id() : null,
                    job != null ? job.experimentId() : null,
                    job != null ? job.section() : null,
                    previousModel);
        }
        payload.put("model", REQUIRED_TEXT_MODEL);
        return REQUIRED_TEXT_MODEL;
    }

    private OpenAiResponse requestWithTransientRetries(Map<String, Object> payload, ExperimentPipelineJobDto job) {
        for (int attempt = 1; attempt <= TRANSIENT_ERROR_MAX_ATTEMPTS; attempt++) {
            try {
                return webClient.post()
                        .uri("/responses")
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(OpenAiResponse.class)
                        .block();
            } catch (WebClientResponseException ex) {
                HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
                boolean transientStatus = status == HttpStatus.BAD_GATEWAY
                        || status == HttpStatus.SERVICE_UNAVAILABLE
                        || status == HttpStatus.GATEWAY_TIMEOUT
                        || status == HttpStatus.TOO_MANY_REQUESTS;
                if (!transientStatus || attempt == TRANSIENT_ERROR_MAX_ATTEMPTS) {
                    log.error(
                            "OpenAI retornou erro não transitório para job {} (experimento={}, seção={}, status={}, responseBody={})",
                            job.id(),
                            job.experimentId(),
                            job.section(),
                            ex.getStatusCode().value(),
                            ex.getResponseBodyAsString());
                    throw ex;
                }
                log.warn("OpenAI retornou status transitório {} para job {} (experimento={}, seção={}). Tentativa {}/{}",
                        ex.getStatusCode().value(),
                        job.id(),
                        job.experimentId(),
                        job.section(),
                        attempt,
                        TRANSIENT_ERROR_MAX_ATTEMPTS);
                sleepBeforeRetry();
            }
        }
        throw new IllegalStateException("Falha inesperada ao chamar OpenAI");
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(TRANSIENT_ERROR_RETRY_DELAY_MS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrompida durante retentativa para OpenAI", interruptedException);
        }
    }

    @SuppressWarnings("unchecked")
    private void enrichPrompt(Map<String, Object> payload, ExperimentPipelineJobDto job) {
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
            Object role = message.get("role");
            if (!(role instanceof String roleValue) || !"user".equalsIgnoreCase(roleValue)) {
                continue;
            }
            Object contentNode = message.get("content");
            if (!(contentNode instanceof String content)) {
                continue;
            }
            message.put("content", withPipelinePrompt(content, job));
        }
    }

    private String withPipelinePrompt(String prompt, ExperimentPipelineJobDto job) {
        String base = prompt != null && prompt.startsWith(PIPELINE_PROMPT_PREFIX)
                ? prompt
                : PIPELINE_PROMPT_PREFIX + (prompt != null ? prompt : "");
        if (isCampaignAngleSection(job) && !base.contains("proofSummary,")) {
            return base + "\n\n" + CAMPAIGN_ANGLE_PROMPT_SUFFIX;
        }
        if (isLandingCopySection(job) && !base.contains("messageMatchSource,")) {
            return base + "\n\n" + LANDING_COPY_PROMPT_SUFFIX;
        }
        if (isLandingLayoutSection(job) && !base.contains("pageGoal,")) {
            return base + "\n\n" + LANDING_LAYOUT_PROMPT_SUFFIX;
        }
        if (isLandingHtmlSection(job) && !base.contains("htmlDocument")) {
            return base + "\n\n" + LANDING_HTML_PROMPT_SUFFIX;
        }
        return base;
    }

    @SuppressWarnings("unchecked")
    private void ensureJsonSchemaCompatibility(Map<String, Object> payload, ExperimentPipelineJobDto job) {
        if (payload == null) {
            return;
        }
        JsonSchemaContext context = JsonSchemaContext.fromPayload(payload);
        if (context == null) {
            return;
        }
        ensureJsonSchemaName(context.nameCarrier(), job);
        Map<String, Object> schema = context.schema();
        if (schema != null) {
            normalizeRequiredForObjectSchemas(schema);
        }
    }

    private void ensureJsonSchemaName(Map<String, Object> container, ExperimentPipelineJobDto job) {
        if (container == null) {
            return;
        }
        Object name = container.get("name");
        if (name instanceof String value && StringUtils.hasText(value)) {
            return;
        }
        String section = job != null && StringUtils.hasText(job.section())
                ? job.section().trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
                : "response";
        container.put("name", "experiment_pipeline_" + section);
    }

    @SuppressWarnings("unchecked")
    private void normalizeRequiredForObjectSchemas(Map<String, Object> schema) {
        if (schema == null) {
            return;
        }
        if (isObjectSchema(schema)) {
            schema.put("additionalProperties", false);
            Object propertiesNode = schema.get("properties");
            if (propertiesNode instanceof Map<?, ?> propertiesRaw) {
                Map<String, Object> properties = (Map<String, Object>) propertiesRaw;
                mergeRequiredWithProperties(schema, properties.keySet());
                for (Object propertySchema : properties.values()) {
                    if (propertySchema instanceof Map<?, ?> nestedSchema) {
                        normalizeRequiredForObjectSchemas((Map<String, Object>) nestedSchema);
                    }
                }
            } else {
                schema.put("properties", Map.of());
                mergeRequiredWithProperties(schema, Set.of());
            }
        }
        Object itemsNode = schema.get("items");
        if (itemsNode instanceof Map<?, ?> itemSchema) {
            normalizeRequiredForObjectSchemas((Map<String, Object>) itemSchema);
        } else if (itemsNode instanceof List<?> itemsList) {
            for (Object item : itemsList) {
                if (item instanceof Map<?, ?> listItemSchema) {
                    normalizeRequiredForObjectSchemas((Map<String, Object>) listItemSchema);
                }
            }
        }
    }

    private boolean isObjectSchema(Map<String, Object> schema) {
        Object typeNode = schema.get("type");
        if (typeNode instanceof String type) {
            return "object".equals(type);
        }
        if (typeNode instanceof List<?> types) {
            return types.stream().anyMatch(candidate -> "object".equals(candidate));
        }
        return false;
    }

    private void mergeRequiredWithProperties(Map<String, Object> schema, Set<?> propertyNames) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (Object candidate : propertyNames) {
            String value = normalizePropertyName(candidate);
            if (StringUtils.hasText(value)) {
                normalized.add(value);
            }
        }
        schema.put("required", new ArrayList<>(normalized));
    }

    private String normalizePropertyName(Object candidate) {
        if (candidate instanceof String text) {
            return text;
        }
        return candidate != null ? String.valueOf(candidate) : null;
    }

    private static final class JsonSchemaContext {
        private final Map<String, Object> nameCarrier;
        private final Map<String, Object> schema;

        private JsonSchemaContext(Map<String, Object> nameCarrier, Map<String, Object> schema) {
            this.nameCarrier = nameCarrier;
            this.schema = schema;
        }

        Map<String, Object> nameCarrier() {
            return nameCarrier;
        }

        Map<String, Object> schema() {
            return schema;
        }

        @SuppressWarnings("unchecked")
        static JsonSchemaContext fromPayload(Map<String, Object> payload) {
            if (payload == null) {
                return null;
            }
            Map<String, Object> textMap = asMap(payload.get("text"));
            Map<String, Object> formatMap = asMap(textMap != null ? textMap.get("format") : null);
            if (isJsonSchemaFormat(formatMap) && formatMap.get("schema") instanceof Map<?, ?> schemaRaw) {
                return new JsonSchemaContext(formatMap, (Map<String, Object>) schemaRaw);
            }
            Map<String, Object> responseFormat = asMap(payload.get("response_format"));
            if (responseFormat == null) {
                return null;
            }
            if (isJsonSchemaFormat(responseFormat) && responseFormat.get("schema") instanceof Map<?, ?> schemaRaw) {
                return new JsonSchemaContext(responseFormat, (Map<String, Object>) schemaRaw);
            }
            Map<String, Object> nestedJsonSchema = asMap(responseFormat.get("json_schema"));
            if (nestedJsonSchema != null && nestedJsonSchema.get("schema") instanceof Map<?, ?> schemaRawNested) {
                return new JsonSchemaContext(nestedJsonSchema, (Map<String, Object>) schemaRawNested);
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> asMap(Object value) {
            if (value instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
            return null;
        }

        private static boolean isJsonSchemaFormat(Map<String, Object> formatMap) {
            if (formatMap == null) {
                return false;
            }
            Object typeNode = formatMap.get("type");
            return typeNode instanceof String type && "json_schema".equals(type);
        }
    }

    private boolean isCampaignAngleSection(ExperimentPipelineJobDto job) {
        if (job == null || !StringUtils.hasText(job.section())) {
            return false;
        }
        String normalized = job.section().trim().toLowerCase(Locale.ROOT);
        return "campaign-angle".equals(normalized) || "campaign_angle".equals(normalized);
    }

    private boolean isLandingCopySection(ExperimentPipelineJobDto job) {
        if (job == null || !StringUtils.hasText(job.section())) {
            return false;
        }
        String normalized = job.section().trim().toLowerCase(Locale.ROOT);
        return "landing-page-copy".equals(normalized)
                || "landing-page_copy".equals(normalized)
                || "landing-copy".equals(normalized)
                || "landing_copy".equals(normalized);
    }

    private boolean isLandingLayoutSection(ExperimentPipelineJobDto job) {
        if (job == null || !StringUtils.hasText(job.section())) {
            return false;
        }
        String normalized = job.section().trim().toLowerCase(Locale.ROOT);
        return "landing-page-wireframe".equals(normalized)
                || "landing-page_wireframe".equals(normalized)
                || "landing-layout".equals(normalized)
                || "landing_layout".equals(normalized);
    }

    private boolean isLandingHtmlSection(ExperimentPipelineJobDto job) {
        if (job == null || !StringUtils.hasText(job.section())) {
            return false;
        }
        String normalized = job.section().trim().toLowerCase(Locale.ROOT);
        return "landing-page-html".equals(normalized)
                || "landing-page_html".equals(normalized)
                || "landing-html".equals(normalized)
                || "landing_html".equals(normalized);
    }
}
