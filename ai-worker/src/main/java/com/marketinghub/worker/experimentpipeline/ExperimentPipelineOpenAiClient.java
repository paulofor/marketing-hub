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
            Contexto do nicho: {nicho}

            Ângulo da campanha: {campaignAngle}
            Headline do anúncio clicado: {adHeadline}
            Dor principal: {primaryPain}
            Promessa principal: {primaryPromise}
            Mecanismo resumido: {mechanismSummary}
            Prova resumida: {proofSummary}

            Objetivo da landing:
            Converter o clique em:
            - preenchimento de briefing
            - geração de amostra
            - pedido de prévia

            Regras:
            1. A landing deve continuar exatamente a promessa do anúncio.
            2. Entregue dois modos de copy da landing:
               - landingCurta: versão enxuta para leitura rápida
               - landingCompleta: versão aprofundada para leitura detalhada
            3. Inclua messageMatchSource informando qual headline do anúncio esta landing está espelhando.
            4. Separe heroPromise de offerPromise, sem misturar proposta de valor com detalhes de oferta.
            5. O hero deve deixar claro:
               - para quem é
               - qual transformação entrega
               - qual próximo passo
            6. O mecanismo deve ser explicado de forma simples.
            7. A prova deve reduzir o medo de “isso é genérico” ou “isso não serve para mim”.
            8. O CTA principal deve aparecer no topo e se repetir ao longo da página.
            9. O texto deve ser escaneável.
            10. Não usar linguagem de consultoria humana.
            11. Toda a oferta deve caber no envelope do produto.
            12. O formulário deve pedir apenas dados necessários para gerar a amostra.
            13. Crie bloco próprio formMicrocopy (headline, suporte e instruções curtas).
            14. Crie objectionHandlingSection cobrindo explicitamente:
                - "não é consultoria"
                - "é gerado por IA"
                - "serve para meu caso?"
            15. Mantenha alinhamento total entre expectativa do clique e conteúdo entregue na landing.

            Formato esperado:
            JSON com:
            messageMatchSource,
            landingCurta {
              heroPromise,
              offerPromise,
              heroTitle,
              heroSubtitle,
              heroBullets,
              primaryCTA,
              formMicrocopy,
              formFields,
              benefitsSection,
              howItWorksSection,
              proofSection,
              offerSection,
              objectionHandlingSection,
              faqSection,
              closingCTA
            },
            landingCompleta {
              heroPromise,
              offerPromise,
              heroTitle,
              heroSubtitle,
              heroBullets,
              primaryCTA,
              formMicrocopy,
              formFields,
              benefitsSection,
              howItWorksSection,
              proofSection,
              offerSection,
              objectionHandlingSection,
              faqSection,
              closingCTA
            }
            """;
    private static final String LANDING_LAYOUT_PROMPT_SUFFIX = """
            Contexto do nicho: {nicho}
            Persona: {persona}
            Promessa principal: {primaryPromise}
            CTA principal: {cta}

            Textos da landing já definidos:
            - Hero: {heroTitle}
            - Subtítulo: {heroSubtitle}
            - Benefícios: {benefitsSection}
            - Como funciona: {howItWorksSection}
            - Prova: {proofSection}
            - Oferta: {offerSection}
            - FAQ: {faqSection}

            Objetivo:
            Criar o wireframe textual da landing page.

            Regras:
            1. A página deve ser mobile-first.
            2. O hero e o formulário devem aparecer sem exigir muito scroll.
            3. O wireframe deve ser experimental, não apenas estrutural.
            4. Adicione variantLayoutId para cada proposta com um valor entre:
               - form-first
               - proof-first
            5. O layout base deve preservar:
               - hero + formulário acima da dobra
               - CTA recorrente
               - FAQ e compliance no footer
            6. Cada seção deve ter uma função clara.
            7. Adicione mobilePriorityScore por seção (inteiro de 1 a 10) para priorização em telas pequenas.
            8. Adicione dropOffRisk por bloco com um valor entre: baixo, médio, alto.
            9. Adicione sectionDependsOn para amarrar cada bloco ao dado de campanha:
               - hero ← primaryPromise
               - prova ← proofSummary
               - CTA ← primaryCTA
            10. O CTA principal deve reaparecer em pontos estratégicos.
            11. O layout deve minimizar atrito e reforçar continuidade com o anúncio.
            12. Não criar seções desnecessárias.

            Formato esperado:
            JSON com:
            pageGoal,
            variantLayoutId,
            sectionOrder [
              {
                "sectionName": "",
                "objective": "",
                "contentType": "",
                "uiNotes": "",
                "mobilePriorityScore": 0,
                "dropOffRisk": "baixo|médio|alto",
                "sectionDependsOn": ""
              }
            ],
            mobilePriorityNotes,
            ctaPlacementNotes,
            formPlacementNotes
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
            log.info("OpenAI payload preview for job {}: {}", job.id(),
                    truncateForLog(objectMapper.writeValueAsString(payload), 1200));
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
            log.info("OpenAI content preview for job {}: {}", job.id(), truncateForLog(content, 1200));
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
                            truncateForLog(ex.getResponseBodyAsString(), 1200));
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

    private String truncateForLog(String text, int maxLength) {
        if (!StringUtils.hasText(text) || maxLength <= 0) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... [truncated]";
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
        return base;
    }

    @SuppressWarnings("unchecked")
    private void ensureJsonSchemaCompatibility(Map<String, Object> payload, ExperimentPipelineJobDto job) {
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
        Object typeNode = formatMap.get("type");
        if (!(typeNode instanceof String type) || !"json_schema".equals(type)) {
            return;
        }
        ensureJsonSchemaName(formatMap, job);
        Object schemaNode = formatMap.get("schema");
        if (schemaNode instanceof Map<?, ?> schemaRaw) {
            normalizeRequiredForObjectSchemas((Map<String, Object>) schemaRaw);
        }
    }

    private void ensureJsonSchemaName(Map<String, Object> formatMap, ExperimentPipelineJobDto job) {
        Object name = formatMap.get("name");
        if (name instanceof String value && StringUtils.hasText(value)) {
            return;
        }
        String section = job != null && StringUtils.hasText(job.section())
                ? job.section().trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
                : "response";
        formatMap.put("name", "experiment_pipeline_" + section);
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
                    if (propertySchema instanceof Map<?, ?> nestedSchema) {
                        normalizeRequiredForObjectSchemas((Map<String, Object>) nestedSchema);
                    }
                }
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

    private void mergeRequiredWithProperties(Map<String, Object> schema, Set<String> propertyNames) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        Object requiredNode = schema.get("required");
        if (requiredNode instanceof List<?> requiredList) {
            for (Object value : requiredList) {
                if (value instanceof String requiredName && StringUtils.hasText(requiredName)) {
                    merged.add(requiredName);
                }
            }
        }
        merged.addAll(propertyNames);
        schema.put("required", new ArrayList<>(merged));
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
}
