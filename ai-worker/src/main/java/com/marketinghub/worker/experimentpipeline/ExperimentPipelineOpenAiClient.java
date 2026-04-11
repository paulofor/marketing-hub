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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
            11. Cada bloco deve preencher mediaSlot com: none, image, illustration, chart, icon-set ou video-thumb.
            12. Cada bloco deve preencher compositionNotes detalhando hierarquia, ritmo de leitura e composição mobile-first.
            13. Não transformar o layout em HTML final; esta etapa define apenas ordem, hierarquia e slots de mídia.
            14. Não usar linguagem de consultoria e não criar estrutura genérica para qualquer mercado.
            15. Se a estrutura puder servir para qualquer nicho, reescreva até ficar específica para o nicho informado.
            16. Cada bloco deve preencher surfaceSpec com surfaceToken, style, contrastMode e notes para contrato explícito de superfície visual por seção.
            17. Alternar surfaceToken entre surface-base e surface-alt-* para reforçar escaneabilidade e hierarquia visual entre seções consecutivas.
            18. Garantir variação intencional de cores de fundo entre seções, explicando a estratégia em backgroundColorStrategy.
            19. Garantir equilíbrio visual entre texto e imagem em cada bloco, explicando como mediaSlot e copy dividem atenção sem competir entre si e detalhando em textImageBalanceNotes.
            20. Definir formSpec como contrato único do formulário com:
                - formId: lead-capture-primary
                - title: Receber a prévia do Kit (IA)
                - submitLabel: Desbloquear o Kit (receber a prévia gerada por IA)
                - submitTarget: #desbloquear
                - fields exatamente nesta ordem:
                  1) nome (type=text, required=true, placeholder=Seu nome)
                  2) email (type=email, required=true, placeholder=voce@exemplo.com)
                  3) whatsapp (type=tel, required=false, placeholder=(DDD) 9XXXX-XXXX)
                - consent.enabled=true, consent.required=false e consent.label preenchido
                - successState com title e message.

            Formato obrigatório (JSON):
            - pageGoal,
            - variantLayoutId
            - messageMatchSummary
            - sectionOrder[] conforme regras acima
            - mobilePriorityNotes
            - ctaPlacementNotes
            - formPlacementNotes
            - backgroundColorStrategy
            - textImageBalanceNotes
            - formSpec
            - consistencyChecks[]
            """;
    private static final String LANDING_HTML_PROMPT_SUFFIX = """
            Objetivo:
            Unificar a copy, o wireframe e o planejamento de imagens aprovados em uma landing final pronta para uso no formulário do experimento.

            Regras:
            1. Entregar documento HTML completo com CSS e JavaScript embutidos.
            2. O CTA principal deve ser idêntico ao CTA aprovado nas etapas anteriores.
            3. O formulário deve ser mobile-first e renderizado exatamente a partir de wireframe.formSpec (sem inventar/remover/renomear/trocar required).
            4. Incluir validação de campos obrigatórios no JavaScript.
            5. Incluir bloco de compliance reforçando entrega digital via IA e sem consultoria.
            6. Consumir explicitamente os artefatos anteriores:
               - copy da landing para narrativa e message match;
               - wireframe para ordem/hierarquia e mediaSlot;
               - planejamento de imagens para imageRole, conversionRole, layoutBinding, placement e altText;
               - wireframe.formSpec para contrato de campos e obrigatoriedade do formulário.
            7. Cada seção renderizada deve incluir data-section-id e aplicar exatamente wireframe.sectionOrder[i].surfaceSpec usando data-surface-token, data-surface-style e data-surface-contrast.
            8. Não inventar estrutura visual fora do layout/plano de imagens sem justificar nos consistencyChecks.
            9. Não usar bibliotecas externas.
            10. Toda tag <img> deve usar src absoluto válido (https://... ou data:image/...) e reutilizar altText do planejamento de imagens.
            11. Cada <img> deve declarar data-image-section-id e data-image-binding-key como binding canônico obrigatório do plano de imagens.
            12. Cada <img> também deve declarar data-image-role (semântico), data-conversion-role, data-attention-priority, data-visual-weight, data-distance-to-cta e data-supports-form-conversion.

            Formato obrigatório (JSON):
            - htmlDocument
            - summary
            - consistencyChecks[] com CTA_MATCH, PROMISE_MATCH, IMAGE_PLAN_BINDING, SURFACE_SPEC_BINDING, FORM_SPEC_BINDING e FORM_USABILITY
            """;
    private static final Pattern FORM_FIELD_BLOCK_PATTERN = Pattern.compile(
            "(?is)<div\\b[^>]*class\\s*=\\s*\"[^\"]*field[^\"]*\"[^>]*>.*?</div>");
    private static final String STATIC_FORM_FIELDS_HTML = """
            <div class="field">
              <label for="field_nome">Nome</label>
              <input type="text" id="field_nome" name="nome" placeholder="Seu nome" required aria-required="true" autocomplete="name" />
              <div class="help">Só para personalizar o envio.</div>
              <div class="error" id="field_nome_error" style="display:none" role="alert"></div>
            </div>
            <div class="field">
              <label for="field_email">E-mail</label>
              <input type="email" id="field_email" name="email" placeholder="voce@exemplo.com" required aria-required="true" autocomplete="email" />
              <div class="help">Vamos enviar a prévia diretamente para o seu e-mail.</div>
              <div class="error" id="field_email_error" style="display:none" role="alert"></div>
            </div>
            <div class="field">
              <label for="field_whatsapp">WhatsApp (opcional)</label>
              <input type="tel" id="field_whatsapp" name="whatsapp" placeholder="(DDD) 9XXXX-XXXX" aria-required="false" autocomplete="tel" />
              <div class="help">Opcional. Se preencher, podemos enviar a prévia também por lá.</div>
              <div class="error" id="field_whatsapp_error" style="display:none" role="alert"></div>
            </div>
            """;
    private static final String STATIC_FORM_SUBMIT_SCRIPT = """
            <script id="lead-capture-submit-contract">
            (function () {
              var form = document.getElementById('lead-capture-primary');
              if (!form || form.dataset.staticSubmitContractApplied === 'true') {
                return;
              }
              form.dataset.staticSubmitContractApplied = 'true';
              form.addEventListener('submit', async function (event) {
                event.preventDefault();
                event.stopImmediatePropagation();
                var pathname = window.location.pathname || '';
                var slugMatch = pathname.match(/\\/flows\\/([^/?#]+)/i);
                var slug = slugMatch && slugMatch[1] ? slugMatch[1] : '';
                var endpointTemplate = form.getAttribute('action') || '/api/flows/{slug}/submissions';
                var endpoint = endpointTemplate.indexOf('{slug}') >= 0
                  ? endpointTemplate.replace('{slug}', slug || 'formpersonal')
                  : endpointTemplate;
                var formData = new FormData(form);
                var nome = (formData.get('nome') || '').toString().trim();
                var email = (formData.get('email') || '').toString().trim();
                var whatsapp = (formData.get('whatsapp') || '').toString().trim();
                var answers = {};
                if (whatsapp) {
                  answers.whatsapp = whatsapp;
                }
                var campaignCode = new URLSearchParams(window.location.search).get('campaign')
                  || new URLSearchParams(window.location.search).get('utm_campaign');
                var payload = { name: nome, email: email, answers: answers };
                if (campaignCode) {
                  payload.campaignCode = campaignCode;
                }
                formData.set('payload', JSON.stringify(payload));
                try {
                  var response = await fetch(endpoint, { method: 'POST', body: formData });
                  if (!response.ok) {
                    throw new Error('Falha ao enviar formulário: ' + response.status);
                  }
                } catch (error) {
                  console.error(error);
                }
              }, true);
            })();
            </script>
            """;

    private static final String LANDING_IMAGE_PLANNING_PROMPT_SUFFIX = """
            Objetivo:
            Planejar as imagens da landing antes da geração final do HTML, usando o ângulo da campanha, os textos da landing e o layout já aprovado.

            Insumos garantidos:
            - Ângulo da campanha (dor, promessa, mecanismo, prova)
            - Texto da landing aprovado
            - Wireframe/layout da landing aprovado
            - CTA principal já validado

            Regras:
            1. Entregar images[] com no mínimo 4 itens ligados a sectionId/sectionName reais do wireframe.
            2. Cada item deve incluir imageBindingKey (curto/canônico), objective, placement, priority, hierarchyLevel, imagePrompt, messageMatchNotes, imageRole, conversionRole, emotionalJob e sectionVisualGoal.
            3. imagePrompt deve ser específico para o contexto da seção (não genérico).
            4. Definir dimensions.desktop e dimensions.mobile para orientar implementação responsiva.
            5. Incluir safeMargins e textOverlayGuidance quando houver texto sobre imagem.
            6. Sempre incluir altText descritivo para cada imagem planejada.
            7. Incluir layoutBinding com preferredDesktopPlacement, preferredMobilePlacement, desktopAspectRatio, mobileAspectRatio, allowCrop e safeCropZones(top/right/bottom/left).
            8. Incluir attentionPriority, visualWeight, distanceToCTA, supportsFormConversion e formRelationNotes para ligar a imagem ao objetivo de lead.
            9. Incluir complianceNotes e negativePrompt para evitar ruído visual e promessas indevidas.
            10. ctaIntegrationNotes deve explicar onde o CTA aparece junto das imagens sem competir com o conteúdo.
            11. sequencingNotes deve explicar a ordem narrativa das imagens ao longo da página.
            12. consistencyChecks precisa incluir IMAGE_MESSAGE_MATCH, VISUAL_HIERARCHY e CTA_CONTINUITY.

            Formato obrigatório (JSON):
            - pageGoal
            - visualDirectionSummary
            - sequencingNotes
            - ctaIntegrationNotes
            - images[]
            - consistencyChecks[]
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
            ensureLandingHtmlHasStaticFormContract(parsed, job);
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


    @SuppressWarnings("unchecked")
    private void ensureLandingHtmlHasStaticFormContract(Map<String, Object> parsed, ExperimentPipelineJobDto job) {
        if (parsed == null) {
            return;
        }
        String normalizedSection = normalizeSection(job);
        if (!isSection(job, "landing-page-html", "landing-html")) {
            return;
        }
        log.info("LANDING_PAGE_HTML detectado para normalização no worker (jobId={}, sectionOriginal={}, sectionNormalizada={})",
                job != null ? job.id() : null,
                job != null ? job.section() : null,
                normalizedSection);

        Map<String, Object> payload = parsed;
        if (parsed.get("landingPageHtml") instanceof Map<?, ?> landingPageHtml) {
            payload = (Map<String, Object>) landingPageHtml;
        }

        Object htmlObject = payload.get("htmlDocument");
        if (!(htmlObject instanceof String htmlDocument) || htmlDocument.isBlank()) {
            return;
        }

        String updatedDocument = enforceStaticFormContract(htmlDocument);
        if (!htmlDocument.equals(updatedDocument)) {
            payload.put("htmlDocument", updatedDocument);
            log.info("Normalização de formulário aplicada no worker (jobId={}, fields={})",
                    job != null ? job.id() : null,
                    extractFieldSnapshot(updatedDocument));
        } else {
            log.info("Normalização de formulário não alterou HTML (jobId={}, fields={})",
                    job != null ? job.id() : null,
                    extractFieldSnapshot(htmlDocument));
        }
    }

    private String enforceStaticFormContract(String htmlDocument) {
        String lowerDocument = htmlDocument.toLowerCase(Locale.ROOT);
        int formStart = lowerDocument.indexOf("<form");
        if (formStart < 0) {
            return htmlDocument;
        }
        int formOpenEnd = htmlDocument.indexOf('>', formStart);
        if (formOpenEnd < 0) {
            return htmlDocument;
        }
        int formClose = lowerDocument.indexOf("</form>", formOpenEnd);
        if (formClose < 0) {
            return htmlDocument;
        }

        String beforeForm = htmlDocument.substring(0, formStart);
        String formOpenTag = htmlDocument.substring(formStart, formOpenEnd + 1);
        String formInner = htmlDocument.substring(formOpenEnd + 1, formClose);
        String afterForm = htmlDocument.substring(formClose);

        String sanitizedInner = stripDynamicFormFields(formInner);
        if (StringUtils.hasText(sanitizedInner)) {
            sanitizedInner = sanitizedInner.stripLeading();
        }

        String normalizedFormOpenTag = normalizeFormOpenTag(formOpenTag);
        StringBuilder rebuiltForm = new StringBuilder(normalizedFormOpenTag.length() + STATIC_FORM_FIELDS_HTML.length() + sanitizedInner.length() + 32);
        rebuiltForm.append(normalizedFormOpenTag).append('\n').append(STATIC_FORM_FIELDS_HTML);
        if (StringUtils.hasText(sanitizedInner)) {
            rebuiltForm.append('\n').append(sanitizedInner);
        }
        String normalizedDocument = beforeForm + rebuiltForm + afterForm;
        return ensureStaticSubmitScript(normalizedDocument);
    }

    private String stripDynamicFormFields(String formInner) {
        if (!StringUtils.hasText(formInner)) {
            return "";
        }
        Matcher matcher = FORM_FIELD_BLOCK_PATTERN.matcher(formInner);
        return matcher.replaceAll("");
    }

    private String normalizeFormOpenTag(String formOpenTag) {
        String updated = upsertAttribute(formOpenTag, "id", "lead-capture-primary");
        updated = upsertAttribute(updated, "method", "post");
        updated = upsertAttribute(updated, "enctype", "multipart/form-data");
        return upsertAttribute(updated, "action", "/api/flows/{slug}/submissions");
    }

    private String upsertAttribute(String tag, String attributeName, String value) {
        Pattern pattern = Pattern.compile("(?i)\\s+" + Pattern.quote(attributeName) + "\\s*=\\s*(['\"]).*?\\1");
        Matcher matcher = pattern.matcher(tag);
        String replacement = " " + attributeName + "=\"" + value + "\"";
        if (matcher.find()) {
            return matcher.replaceFirst(replacement);
        }
        int closeIdx = tag.lastIndexOf('>');
        if (closeIdx < 0) {
            return tag;
        }
        return tag.substring(0, closeIdx) + replacement + tag.substring(closeIdx);
    }

    private String ensureStaticSubmitScript(String htmlDocument) {
        if (!StringUtils.hasText(htmlDocument) || htmlDocument.contains("id=\"lead-capture-submit-contract\"")) {
            return htmlDocument;
        }
        String lower = htmlDocument.toLowerCase(Locale.ROOT);
        int bodyClose = lower.lastIndexOf("</body>");
        if (bodyClose >= 0) {
            return htmlDocument.substring(0, bodyClose) + STATIC_FORM_SUBMIT_SCRIPT + "\n" + htmlDocument.substring(bodyClose);
        }
        return htmlDocument + "\n" + STATIC_FORM_SUBMIT_SCRIPT;
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
        if (isLandingImagePlanningSection(job) && !base.contains("visualDirectionSummary")) {
            return base + "\n\n" + LANDING_IMAGE_PLANNING_PROMPT_SUFFIX;
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
        return isSection(job, "campaign-angle", "campaign_angle");
    }

    private boolean isLandingCopySection(ExperimentPipelineJobDto job) {
        return isSection(job, "landing-page-copy", "landing-page_copy", "landing-copy", "landing_copy");
    }

    private boolean isLandingLayoutSection(ExperimentPipelineJobDto job) {
        return isSection(job, "landing-page-wireframe", "landing-page_wireframe", "landing-layout", "landing_layout");
    }

    private boolean isLandingHtmlSection(ExperimentPipelineJobDto job) {
        return isSection(job, "landing-page-html", "landing-page_html", "landing-html", "landing_html");
    }

    private boolean isLandingImagePlanningSection(ExperimentPipelineJobDto job) {
        return isSection(job, "landing-page-image-planning", "landing-page_image_planning", "landing-image-planning", "landing_image_planning");
    }

    private boolean isSection(ExperimentPipelineJobDto job, String... aliases) {
        if (job == null || !StringUtils.hasText(job.section())) {
            return false;
        }
        String normalized = normalizeSection(job);
        for (String alias : aliases) {
            if (normalizeSection(alias).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeSection(ExperimentPipelineJobDto job) {
        return normalizeSection(job != null ? job.section() : null);
    }

    private String normalizeSection(String rawSection) {
        if (!StringUtils.hasText(rawSection)) {
            return "";
        }
        return rawSection.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String extractFieldSnapshot(String htmlDocument) {
        Set<String> fields = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("(?is)<(input|select|textarea)\\b[^>]*\\bname\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>").matcher(htmlDocument);
        while (matcher.find()) {
            fields.add(matcher.group(2).trim().toLowerCase(Locale.ROOT));
        }
        return fields.toString();
    }
}
