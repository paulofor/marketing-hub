package com.marketinghub.worker.experimentpipeline;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.OpenAiCostEstimator;
import com.marketinghub.worker.openai.OpenAiResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class ExperimentPipelineOpenAiClient {
    private static final Logger log = LoggerFactory.getLogger(ExperimentPipelineOpenAiClient.class);
    private static final String REQUIRED_TEXT_MODEL = "gpt-5.2";
    private static final String REQUIRED_LANDING_HTML_MODEL = "gpt-5.1-codex";
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
    private static final String CAMPAIGN_ANGLE_TEMPLATE_PATH = "prompts/experiment/campaign-angle.md";
    private static final String LANDING_COPY_TEMPLATE_PATH = "prompts/experiment/landing-copy.md";
    private static final String LANDING_WIREFRAME_TEMPLATE_PATH = "prompts/experiment/landing-wireframe.md";
    private static final String LANDING_IMAGE_PLANNING_TEMPLATE_PATH = "prompts/experiment/landing-image-planning.md";
    private static final String LANDING_HTML_TEMPLATE_PATH = "prompts/experiment/landing-html.md";

    private static final String CAMPAIGN_ANGLE_MARKER = "- visualAngle";
    private static final String LANDING_COPY_MARKER = "- messageMatchSource";
    private static final String LANDING_WIREFRAME_MARKER = "variantLayoutId";
    private static final String LANDING_IMAGE_PLANNING_MARKER = "visualDirectionSummary";
    private static final String LANDING_HTML_MARKER = "htmlDocument";

    private static final List<String> TEMPLATE_VARIABLE_KEYS = List.of(
            "NICHE_NAME",
            "PERSONA_NAME",
            "HYPOTHESIS_TITLE",
            "OFFER_COMMERCIAL_SUMMARY",
            "PROOF_SUMMARY",
            "MECHANISM_SUMMARY",
            "RESULT_SUMMARY",
            "PAIN_SUMMARY",
            "PRIMARY_PAIN_SUMMARY",
            "PRIMARY_PROMISE_SUMMARY",
            "OFFER_NAME",
            "PRIMARY_CTA_ACTION",
            "PRIMARY_CTA_LABEL",
            "PRODUCT_ENVELOPE",
            "DELIVERABLES_JSON",
            "PROOF_ASSET_JSON",
            "CASE_NOTES");
    private static final String CASE_DATA_BLOCK_KEY = "CASE_DATA_BLOCK";

    private static final Pattern FORM_FIELD_BLOCK_PATTERN = Pattern.compile(
            "(?is)<div\\b[^>]*class\\s*=\\s*\"[^\"]*field[^\"]*\"[^>]*>.*?</div>");
    private static final Pattern HTML_SECTION_PATTERN = Pattern.compile("(?is)<section\\b[^>]*>");
    private static final Pattern HTML_OPENING_TAG_PATTERN = Pattern.compile("(?is)<[a-z][^>]*>");
    private static final Pattern HTML_ATTRIBUTE_PATTERN = Pattern.compile("(?i)(data-surface-token|class)\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern HTML_GENERIC_ATTRIBUTE_PATTERN = Pattern.compile("(?i)([a-z0-9:-]+)\\s*=\\s*(['\"])(.*?)\\2");
    private static final Pattern MARKDOWN_CODE_BLOCK_PATTERN = Pattern.compile("(?is)^```(?:html|htm)?\\s*(.*?)\\s*```$");
    private static final List<String> GENERIC_DELIVERABLE_TERMS = List.of("kit", "prévia", "previa", "ativos digitais", "material", "pacote");
    private static final List<String> CONCRETE_DELIVERABLE_TERMS = List.of(
            "checklist", "template", "roteiro", "guia", "planilha", "pdf", "video", "vídeo", "análise", "analise", "exemplo", "script", "modelo");
    private static final List<String> OUTCOME_TERMS = List.of(
            "resultado", "reduz", "reduzir", "aumenta", "aumentar", "convers", "conversão", "conversao",
            "econom", "evita", "ganha", "melhor", "cresce", "escala");
    private static final List<String> LINK_TERMS = List.of(
            "para", "assim", "isso", "porque", "com isso", "de forma", "ajuda");
    private static final Set<String> VAGUE_CTA_LABELS = Set.of(
            "saiba mais", "clique aqui", "continuar", "ver mais", "acessar", "prosseguir", "enviar");
    private static final List<String> CTA_NEXT_STEP_TERMS = List.of(
            "prévia", "previa", "kit", "plano", "checklist", "acesso", "amostra", "análise", "analise", "diagnóstico", "diagnostico", "template");
    private static final List<String> PREVIEW_TERMS = List.of(
            "preview", "prévia", "previa", "prova", "amostra", "entreg", "antes", "depois");
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
                var submitButton = form.querySelector('[type="submit"]');
                var originalButtonText = submitButton ? submitButton.textContent : '';
                if (submitButton) {
                  submitButton.disabled = true;
                  submitButton.textContent = 'Enviando...';
                }
                var pathname = window.location.pathname || '';
                var slugMatch = pathname.match(/\\/flows\\/([^/?#]+)/i);
                var slug = slugMatch && slugMatch[1] ? slugMatch[1] : '';
                var endpointTemplate = form.getAttribute('action') || '/api/flows/{slug}/submissions';
                var slugToken = /\\{slug\\}|%7Bslug%7D/i;
                var endpoint = slugToken.test(endpointTemplate)
                  ? endpointTemplate.replace(slugToken, slug || 'formpersonal')
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
                formData.set(
                  'payload',
                  new Blob([JSON.stringify(payload)], { type: 'application/json' })
                );
                try {
                  var response = await fetch(endpoint, { method: 'POST', body: formData });
                  if (!response.ok) {
                    throw new Error('Falha ao enviar formulário: ' + response.status);
                  }
                  var successMessage = document.getElementById('lead-capture-success-message');
                  if (!successMessage) {
                    successMessage = document.createElement('div');
                    successMessage.id = 'lead-capture-success-message';
                    successMessage.setAttribute('role', 'status');
                    successMessage.style.marginTop = '12px';
                    successMessage.style.padding = '12px';
                    successMessage.style.borderRadius = '10px';
                    successMessage.style.background = '#ecfdf3';
                    successMessage.style.color = '#065f46';
                    successMessage.style.fontWeight = '600';
                    form.appendChild(successMessage);
                  }
                  successMessage.textContent = 'Recebemos seus dados com sucesso! Aguarde: em instantes você receberá a prévia no e-mail informado.';
                  successMessage.style.display = 'block';
                  form.reset();
                } catch (error) {
                  console.error(error);
                  var fallbackError = document.getElementById('lead-capture-error-message');
                  if (!fallbackError) {
                    fallbackError = document.createElement('div');
                    fallbackError.id = 'lead-capture-error-message';
                    fallbackError.setAttribute('role', 'alert');
                    fallbackError.style.marginTop = '12px';
                    fallbackError.style.color = '#b91c1c';
                    fallbackError.textContent = 'Não foi possível enviar agora. Tente novamente em alguns instantes.';
                    form.appendChild(fallbackError);
                  }
                  fallbackError.style.display = 'block';
                } finally {
                  if (submitButton) {
                    submitButton.disabled = false;
                    submitButton.textContent = originalButtonText || 'Enviar';
                  }
                }
              }, true);
            })();
            </script>
            """;

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final boolean enabled;
    private final Map<String, PromptTemplate> promptTemplates;

    public ExperimentPipelineOpenAiClient(WebClient.Builder builder,
                                          ObjectMapper objectMapper,
                                          @Value("${openai.api-key:}") String apiKey,
                                          @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.enabled = StringUtils.hasText(apiKey);
        this.promptTemplates = loadPromptTemplates();
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
            Map<String, Object> parsed = parseResponseContent(content, job);
            enforceLandingHtmlSurfaceBinding(parsed, payload, job, content);
            validateExpectedResponseContract(parsed, content, job);
            enrichConsistencyChecks(parsed, job);
            String sectionContent = objectMapper.writeValueAsString(parsed);
            Integer inputTokens = response.usage() != null ? response.usage().effectiveInputTokens() : null;
            Integer outputTokens = response.usage() != null ? response.usage().effectiveOutputTokens() : null;
            TemplateTrace templateTrace = resolveTemplateTrace(job, effectiveModel);
            return new ExperimentPipelineJobCompletionPayload(
                    sectionContent,
                    objectMapper.writeValueAsString(response),
                    buildTrackedRequestBodyJson(payload, templateTrace),
                    inputTokens,
                    outputTokens,
                    OpenAiCostEstimator.estimateUsd(effectiveModel, response.usage()));
        } catch (Exception ex) {
            String message = "Falha ao gerar seção " + job.section() + " do experimento " + job.experimentId();
            if (StringUtils.hasText(ex.getMessage())) {
                message = message + ". " + ex.getMessage();
            }
            throw new IllegalStateException(message, ex);
        }
    }


    private Map<String, Object> parseResponseContent(String content, ExperimentPipelineJobDto job) throws JsonProcessingException {
        String trimmedContent = content != null ? content.trim() : "";
        if (isSection(job, "landing-page-html", "landing-html")) {
            String htmlDocument = extractLandingHtmlDocument(trimmedContent, job);
            if (StringUtils.hasText(htmlDocument)) {
                Map<String, Object> landingPageHtml = new LinkedHashMap<>();
                landingPageHtml.put("htmlDocument", htmlDocument);
                return new LinkedHashMap<>(Map.of("landingPageHtml", landingPageHtml));
            }
        }
        return objectMapper.readValue(content, new TypeReference<>() {});
    }

    @SuppressWarnings("unchecked")
    private void validateExpectedResponseContract(Map<String, Object> parsed,
                                                  String rawContent,
                                                  ExperimentPipelineJobDto job) {
        if (!isSection(job, "landing-page-html", "landing-html")) {
            return;
        }
        Object payload = parsed != null ? parsed.get("landingPageHtml") : null;
        Map<String, Object> landingPayload =
                payload instanceof Map<?, ?> ? (Map<String, Object>) payload : parsed;
        String htmlDocument = landingPayload != null ? readString(landingPayload, LANDING_HTML_MARKER) : "";
        if (StringUtils.hasText(htmlDocument)) {
            return;
        }
        String reason = "Quebra de contrato: LANDING_PAGE_HTML sem campo htmlDocument válido.";
        logContractBreak(job, reason, rawContent, null);
        throw new IllegalStateException(reason);
    }

    private String extractLandingHtmlDocument(String trimmedContent, ExperimentPipelineJobDto job) {
        if (!StringUtils.hasText(trimmedContent)) {
            return null;
        }
        if (trimmedContent.startsWith("<")) {
            return trimmedContent;
        }
        Matcher codeBlockMatcher = MARKDOWN_CODE_BLOCK_PATTERN.matcher(trimmedContent);
        if (codeBlockMatcher.matches()) {
            String codeBlockPayload = codeBlockMatcher.group(1);
            if (StringUtils.hasText(codeBlockPayload) && codeBlockPayload.trim().startsWith("<")) {
                return codeBlockPayload.trim();
            }
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(trimmedContent, new TypeReference<>() {});
            String jsonHtml = extractHtmlDocumentFromPayload(parsed);
            if (StringUtils.hasText(jsonHtml)) {
                return jsonHtml.trim();
            }
        } catch (JsonProcessingException ignored) {
            logContractBreak(
                    job,
                    "Quebra de contrato: LANDING_PAGE_HTML não veio em HTML puro e também não é JSON válido.",
                    trimmedContent,
                    ignored);
            // fallback: conteúdo seguirá para parser JSON original
        }
        return null;
    }

    private void logContractBreak(ExperimentPipelineJobDto job, String reason, String modelResponse, Exception ex) {
        String section = job != null ? String.valueOf(job.section()) : "unknown";
        Long experimentId = job != null ? job.experimentId() : null;
        String preview = modelResponse == null ? "" : modelResponse.trim();
        if (preview.length() > 4000) {
            preview = preview.substring(0, 4000) + "...(truncated)";
        }
        if (ex != null) {
            log.error(
                    "Contrato de resposta quebrado (experimento={}, seção={}): {}. Resposta do modelo: {}",
                    experimentId,
                    section,
                    reason,
                    preview,
                    ex);
            return;
        }
        log.error(
                "Contrato de resposta quebrado (experimento={}, seção={}): {}. Resposta do modelo: {}",
                experimentId,
                section,
                reason,
                preview);
    }

    @SuppressWarnings("unchecked")
    private String extractHtmlDocumentFromPayload(Map<String, Object> parsed) {
        if (parsed == null || parsed.isEmpty()) {
            return null;
        }
        Object directHtml = parsed.get(LANDING_HTML_MARKER);
        if (directHtml instanceof String htmlDocument && StringUtils.hasText(htmlDocument)) {
            return htmlDocument;
        }
        Object nestedPayload = parsed.get("landingPageHtml");
        if (nestedPayload instanceof Map<?, ?> nestedMap) {
            Object nestedHtml = ((Map<String, Object>) nestedMap).get(LANDING_HTML_MARKER);
            if (nestedHtml instanceof String htmlDocument && StringUtils.hasText(htmlDocument)) {
                return htmlDocument;
            }
        }
        return null;
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

    @SuppressWarnings("unchecked")
    private void enforceLandingHtmlSurfaceBinding(Map<String, Object> parsed,
                                                  Map<String, Object> requestPayload,
                                                  ExperimentPipelineJobDto job,
                                                  String rawContent) {
        if (!isLandingHtmlSection(job) || parsed == null) {
            return;
        }
        Object nestedPayload = parsed.get("landingPageHtml");
        Map<String, Object> landingPayload = nestedPayload instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : parsed;
        String htmlDocument = readString(landingPayload, LANDING_HTML_MARKER);
        if (!StringUtils.hasText(htmlDocument)) {
            return;
        }
        List<SectionSurfaceContract> expectedSurfaces = extractExpectedSurfacesFromRequestPayload(requestPayload);
        if (expectedSurfaces.isEmpty()) {
            return;
        }
        String synchronizedHtml = synchronizeHtmlSurfaceBinding(htmlDocument, expectedSurfaces);
        List<SectionSurfaceContract> expectedSorted = expectedSurfaces.stream()
                .sorted(java.util.Comparator.comparing(SectionSurfaceContract::sectionId))
                .toList();
        List<SectionSurfaceContract> actualSorted = extractSurfacesFromHtml(synchronizedHtml).stream()
                .sorted(java.util.Comparator.comparing(SectionSurfaceContract::sectionId))
                .toList();
        if (!expectedSorted.equals(actualSorted)) {
            String reason = "Quebra de contrato: LANDING_PAGE_HTML divergente de landing-page-wireframe.sectionOrder.surfaceSpec.";
            logContractBreak(job, reason, rawContent, null);
            throw new IllegalStateException(reason);
        }
        landingPayload.put(LANDING_HTML_MARKER, synchronizedHtml);
    }

    @SuppressWarnings("unchecked")
    private List<SectionSurfaceContract> extractExpectedSurfacesFromRequestPayload(Map<String, Object> requestPayload) {
        String userPrompt = extractUserPromptFromRequestPayload(requestPayload);
        if (!StringUtils.hasText(userPrompt)) {
            return List.of();
        }
        int markerIndex = userPrompt.indexOf("Wireframe da landing:");
        if (markerIndex < 0) {
            return List.of();
        }
        int jsonStart = userPrompt.indexOf('{', markerIndex);
        if (jsonStart < 0) {
            return List.of();
        }
        String wireframeJson = extractBalancedJsonObject(userPrompt, jsonStart);
        if (!StringUtils.hasText(wireframeJson)) {
            return List.of();
        }
        try {
            Map<String, Object> root = objectMapper.readValue(wireframeJson, new TypeReference<>() {});
            Object wireframeNode = root.get("landingPageWireframe");
            Map<String, Object> wireframe = wireframeNode instanceof Map<?, ?> map
                    ? (Map<String, Object>) map
                    : root;
            Object sectionsNode = wireframe.get("sectionOrder");
            if (!(sectionsNode instanceof List<?> sections)) {
                return List.of();
            }
            List<SectionSurfaceContract> expected = new ArrayList<>();
            for (Object section : sections) {
                if (!(section instanceof Map<?, ?> sectionMap)) {
                    continue;
                }
                Map<String, Object> sectionData = (Map<String, Object>) sectionMap;
                String sectionId = normalizeHtmlAttr(readString(sectionData, "sectionId"));
                Object surfaceNode = sectionData.get("surfaceSpec");
                if (!(surfaceNode instanceof Map<?, ?> surfaceMap)) {
                    continue;
                }
                Map<String, Object> surface = (Map<String, Object>) surfaceMap;
                String surfaceToken = normalizeHtmlAttr(readString(surface, "surfaceToken"));
                String style = normalizeHtmlAttr(readString(surface, "style"));
                String contrastMode = normalizeHtmlAttr(readString(surface, "contrastMode"));
                if (!StringUtils.hasText(sectionId) || !StringUtils.hasText(surfaceToken)
                        || !StringUtils.hasText(style) || !StringUtils.hasText(contrastMode)) {
                    continue;
                }
                expected.add(new SectionSurfaceContract(sectionId, surfaceToken, style, contrastMode));
            }
            return expected;
        } catch (Exception ex) {
            log.warn("Falha ao extrair wireframe do prompt para validar surfaceSpec: {}", ex.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractUserPromptFromRequestPayload(Map<String, Object> requestPayload) {
        if (requestPayload == null) {
            return "";
        }
        Object inputNode = requestPayload.get("input");
        if (!(inputNode instanceof List<?> inputList)) {
            return "";
        }
        for (Object node : inputList) {
            if (!(node instanceof Map<?, ?> message)) {
                continue;
            }
            Object role = ((Map<String, Object>) message).get("role");
            if (!(role instanceof String roleValue) || !"user".equalsIgnoreCase(roleValue)) {
                continue;
            }
            Object content = ((Map<String, Object>) message).get("content");
            if (content instanceof String text) {
                return text;
            }
        }
        return "";
    }

    private String extractBalancedJsonObject(String source, int startIndex) {
        if (!StringUtils.hasText(source) || startIndex < 0 || startIndex >= source.length() || source.charAt(startIndex) != '{') {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;
        for (int idx = startIndex; idx < source.length(); idx++) {
            char current = source.charAt(idx);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (current == '\\') {
                escaping = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                depth++;
                continue;
            }
            if (current == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(startIndex, idx + 1);
                }
            }
        }
        return null;
    }

    private String synchronizeHtmlSurfaceBinding(String htmlDocument, List<SectionSurfaceContract> expectedSurfaces) {
        if (!StringUtils.hasText(htmlDocument) || expectedSurfaces == null || expectedSurfaces.isEmpty()) {
            return htmlDocument;
        }
        Map<String, SectionSurfaceContract> expectedBySection = new LinkedHashMap<>();
        for (SectionSurfaceContract contract : expectedSurfaces) {
            expectedBySection.put(contract.sectionId(), contract);
        }
        StringBuffer rebuilt = new StringBuffer();
        Matcher tagMatcher = HTML_OPENING_TAG_PATTERN.matcher(htmlDocument);
        while (tagMatcher.find()) {
            String tag = tagMatcher.group();
            String sectionId = normalizeHtmlAttr(readAttribute(tag, "data-section-id"));
            SectionSurfaceContract expected = expectedBySection.get(sectionId);
            if (expected == null) {
                tagMatcher.appendReplacement(rebuilt, Matcher.quoteReplacement(tag));
                continue;
            }
            String updatedTag = upsertTagAttribute(tag, "data-surface-token", expected.surfaceToken());
            updatedTag = upsertTagAttribute(updatedTag, "data-surface-style", expected.style());
            updatedTag = upsertTagAttribute(updatedTag, "data-surface-contrast", expected.contrastMode());
            tagMatcher.appendReplacement(rebuilt, Matcher.quoteReplacement(updatedTag));
        }
        tagMatcher.appendTail(rebuilt);
        return rebuilt.toString();
    }

    private List<SectionSurfaceContract> extractSurfacesFromHtml(String htmlDocument) {
        if (!StringUtils.hasText(htmlDocument)) {
            return List.of();
        }
        Map<String, SectionSurfaceContract> contractsBySectionId = new LinkedHashMap<>();
        Matcher tagMatcher = HTML_OPENING_TAG_PATTERN.matcher(htmlDocument);
        while (tagMatcher.find()) {
            String tag = tagMatcher.group();
            String sectionId = normalizeHtmlAttr(readAttribute(tag, "data-section-id"));
            String surfaceToken = normalizeHtmlAttr(readAttribute(tag, "data-surface-token"));
            String style = normalizeHtmlAttr(readAttribute(tag, "data-surface-style"));
            String contrastMode = normalizeHtmlAttr(readAttribute(tag, "data-surface-contrast"));
            if (!StringUtils.hasText(sectionId) || !StringUtils.hasText(surfaceToken)
                    || !StringUtils.hasText(style) || !StringUtils.hasText(contrastMode)) {
                continue;
            }
            contractsBySectionId.put(sectionId, new SectionSurfaceContract(sectionId, surfaceToken, style, contrastMode));
        }
        return new ArrayList<>(contractsBySectionId.values());
    }

    private String readAttribute(String tag, String attributeName) {
        if (!StringUtils.hasText(tag) || !StringUtils.hasText(attributeName)) {
            return "";
        }
        Matcher matcher = HTML_GENERIC_ATTRIBUTE_PATTERN.matcher(tag);
        while (matcher.find()) {
            if (attributeName.equalsIgnoreCase(matcher.group(1))) {
                return matcher.group(3);
            }
        }
        return "";
    }

    private String upsertTagAttribute(String tag, String attributeName, String value) {
        Pattern pattern = Pattern.compile("(?i)(\\s+" + Pattern.quote(attributeName) + "\\s*=\\s*)(['\"])(.*?)\\2");
        Matcher matcher = pattern.matcher(tag);
        String escapedValue = value == null ? "" : value.trim();
        if (matcher.find()) {
            return matcher.replaceFirst("$1\"" + Matcher.quoteReplacement(escapedValue) + "\"");
        }
        int closeTag = tag.lastIndexOf('>');
        if (closeTag < 0) {
            return tag;
        }
        return tag.substring(0, closeTag) + " " + attributeName + "=\"" + escapedValue + "\"" + tag.substring(closeTag);
    }

    private String normalizeHtmlAttr(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        normalized = normalized
                .replace("\\&quot;", "\"")
                .replace("\\&#34;", "\"")
                .replace("\\&#x22;", "\"")
                .replace("&quot;", "\"")
                .replace("&#34;", "\"")
                .replace("&#x22;", "\"")
                .replace("\\n", "")
                .replace("\\r", "")
                .replace("\\t", "");
        if (normalized.length() >= 2) {
            char first = normalized.charAt(0);
            char last = normalized.charAt(normalized.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                normalized = normalized.substring(1, normalized.length() - 1).trim();
            }
        }
        return normalized;
    }

    private record SectionSurfaceContract(String sectionId, String surfaceToken, String style, String contrastMode) {
    }

    @SuppressWarnings("unchecked")
    private void enrichConsistencyChecks(Map<String, Object> parsed, ExperimentPipelineJobDto job) {
        if (parsed == null || job == null) {
            return;
        }
        if (isLandingCopySection(job)) {
            Map<String, Object> copy = parsed.get("landingPageCopy") instanceof Map<?, ?> nested
                    ? (Map<String, Object>) nested
                    : parsed;
            List<Map<String, Object>> checks = readConsistencyChecks(copy);
            upsertCheck(checks, evaluateDeliverableClarity(copy));
            upsertCheck(checks, evaluateDeliverableToOutcomeLink(copy));
            upsertCheck(checks, evaluateCtaSpecificity(copy));
            upsertCheck(checks, evaluateArtifactSchemaCompatibility("landingPageCopy", copy,
                    List.of("pageGoal", "primaryCTA", "hero", "bodySections", "ctaBlocks")));
            copy.put("consistencyChecks", checks);
            return;
        }
        if (isLandingLayoutSection(job)) {
            Map<String, Object> wireframe = parsed.get("landingPageWireframe") instanceof Map<?, ?> nested
                    ? (Map<String, Object>) nested
                    : parsed;
            List<Map<String, Object>> checks = readConsistencyChecks(wireframe);
            upsertCheck(checks, evaluateSectionThemeVariationFromWireframe(wireframe));
            upsertCheck(checks, evaluateArtifactSchemaCompatibility("landingPageWireframe", wireframe,
                    List.of("pageGoal", "variantLayoutId", "sectionOrder")));
            wireframe.put("consistencyChecks", checks);
            return;
        }
        if (isLandingImagePlanningSection(job)) {
            Map<String, Object> imagePlanning = parsed.get("landingPageImagePlanning") instanceof Map<?, ?> nested
                    ? (Map<String, Object>) nested
                    : parsed;
            List<Map<String, Object>> checks = readConsistencyChecks(imagePlanning);
            upsertCheck(checks, evaluatePreviewConcreteness(imagePlanning));
            upsertCheck(checks, evaluateArtifactSchemaCompatibility("landingPageImagePlanning", imagePlanning,
                    List.of("pageGoal", "images")));
            imagePlanning.put("consistencyChecks", checks);
            return;
        }
        if (isLandingHtmlSection(job)) {
            Map<String, Object> html = parsed.get("landingPageHtml") instanceof Map<?, ?> nested
                    ? (Map<String, Object>) nested
                    : parsed;
            List<Map<String, Object>> checks = readConsistencyChecks(html);
            upsertCheck(checks, evaluateSectionThemeVariationFromHtml(html));
            upsertCheck(checks, evaluatePreviewConcretenessFromHtml(html));
            upsertCheck(checks, evaluateArtifactSchemaCompatibility("landingPageHtml", html,
                    List.of("htmlDocument", "formSpec", "summary", "imagePlacementContract")));
            html.put("consistencyChecks", checks);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readConsistencyChecks(Map<String, Object> artifact) {
        if (artifact == null) {
            return new ArrayList<>();
        }
        Object checksNode = artifact.get("consistencyChecks");
        if (!(checksNode instanceof List<?> checks)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Object check : checks) {
            if (check instanceof Map<?, ?> map) {
                normalized.add(new LinkedHashMap<>((Map<String, Object>) map));
            }
        }
        return normalized;
    }

    private void upsertCheck(List<Map<String, Object>> checks, Map<String, Object> computed) {
        if (checks == null || computed == null || !StringUtils.hasText((String) computed.get("check"))) {
            return;
        }
        String target = String.valueOf(computed.get("check"));
        checks.removeIf(item -> Objects.equals(String.valueOf(item.get("check")), target));
        checks.add(computed);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> evaluateDeliverableClarity(Map<String, Object> copy) {
        String corpus = collectCopyText(copy);
        int genericHits = countHits(corpus, GENERIC_DELIVERABLE_TERMS);
        int concreteHits = countHits(corpus, CONCRETE_DELIVERABLE_TERMS);
        if (genericHits > 0 && concreteHits == 0) {
            return check("DELIVERABLE_CLARITY", "FAIL",
                    "Oferta usa termos genéricos sem composição concreta suficiente.");
        }
        if (genericHits > 0 && concreteHits < 2) {
            return check("DELIVERABLE_CLARITY", "WARN",
                    "Oferta ainda depende de termos genéricos; detalhe melhor os entregáveis.");
        }
        return check("DELIVERABLE_CLARITY", "PASS", "Oferta descreve entregáveis com detalhes concretos.");
    }

    private Map<String, Object> evaluateDeliverableToOutcomeLink(Map<String, Object> copy) {
        String corpus = collectCopyText(copy);
        int deliverableHits = countHits(corpus, GENERIC_DELIVERABLE_TERMS) + countHits(corpus, CONCRETE_DELIVERABLE_TERMS);
        int outcomeHits = countHits(corpus, OUTCOME_TERMS);
        int linkHits = countHits(corpus, LINK_TERMS);
        if (deliverableHits > 0 && outcomeHits == 0) {
            return check("DELIVERABLE_TO_OUTCOME_LINK", "FAIL",
                    "A landing lista entregáveis sem conectar explicitamente com resultado esperado.");
        }
        if (deliverableHits > 0 && (outcomeHits < 2 || linkHits == 0)) {
            return check("DELIVERABLE_TO_OUTCOME_LINK", "WARN",
                    "A conexão entre entregáveis e resultado existe, mas ainda está fraca.");
        }
        return check("DELIVERABLE_TO_OUTCOME_LINK", "PASS", "Entregáveis estão conectados à dor/resultado prometido.");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> evaluateCtaSpecificity(Map<String, Object> copy) {
        List<String> labels = new ArrayList<>();
        labels.add(readString(copy, "primaryCTA"));
        Object heroNode = copy != null ? copy.get("hero") : null;
        if (heroNode instanceof Map<?, ?> hero) {
            labels.add(readString((Map<String, Object>) hero, "ctaLabel"));
        }
        Object ctaBlocksNode = copy != null ? copy.get("ctaBlocks") : null;
        if (ctaBlocksNode instanceof List<?> blocks) {
            for (Object block : blocks) {
                if (block instanceof Map<?, ?> map) {
                    labels.add(readString((Map<String, Object>) map, "ctaLabel"));
                }
            }
        }
        labels = labels.stream().filter(StringUtils::hasText).map(String::trim).toList();
        if (labels.isEmpty()) {
            return check("CTA_SPECIFICITY", "FAIL", "CTA principal ausente.");
        }
        long vagueCount = labels.stream().filter(this::isVagueCta).count();
        long explicitNextStep = labels.stream().filter(this::hasExplicitNextStep).count();
        if (vagueCount == labels.size() && explicitNextStep == 0) {
            return check("CTA_SPECIFICITY", "FAIL", "CTA está vaga e não explicita próximo passo.");
        }
        if (vagueCount > 0 || explicitNextStep == 0) {
            return check("CTA_SPECIFICITY", "WARN", "CTA precisa deixar mais claro o próximo passo.");
        }
        return check("CTA_SPECIFICITY", "PASS", "CTA principal está específica e orientada à ação.");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> evaluatePreviewConcreteness(Map<String, Object> imagePlanning) {
        Object imagesNode = imagePlanning != null ? imagePlanning.get("images") : null;
        if (!(imagesNode instanceof List<?> images) || images.isEmpty()) {
            return check("PREVIEW_CONCRETENESS", "FAIL", "Plano de imagens sem itens para provar preview visual.");
        }
        int previewCandidates = 0;
        int concretePreviews = 0;
        for (Object image : images) {
            if (!(image instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> item = (Map<String, Object>) map;
            String scope = (readString(item, "imageRole") + " " + readString(item, "conversionRole") + " " + readString(item, "sectionName")).toLowerCase(Locale.ROOT);
            boolean isPreviewCandidate = containsAny(scope, PREVIEW_TERMS);
            if (isPreviewCandidate) {
                previewCandidates++;
                String prompt = readString(item, "imagePrompt");
                int supportingCount = readStringList(item, "supportingElements").size();
                if (prompt.length() >= 40 && supportingCount > 0) {
                    concretePreviews++;
                }
            }
        }
        if (previewCandidates == 0) {
            return check("PREVIEW_CONCRETENESS", "WARN", "Não há imagem explicitamente ligada à prova/preview.");
        }
        if (concretePreviews == 0) {
            return check("PREVIEW_CONCRETENESS", "FAIL", "Preview prometido sem descrição visual concreta.");
        }
        if (concretePreviews < previewCandidates) {
            return check("PREVIEW_CONCRETENESS", "WARN", "Parte das imagens de preview ainda está genérica.");
        }
        return check("PREVIEW_CONCRETENESS", "PASS", "Preview visual descrito de forma concreta.");
    }

    private Map<String, Object> evaluatePreviewConcretenessFromHtml(Map<String, Object> html) {
        String document = readString(html, "htmlDocument").toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(document)) {
            return check("PREVIEW_CONCRETENESS", "FAIL", "HTML sem conteúdo para comprovar preview.");
        }
        boolean hasPreviewHints = containsAny(document, PREVIEW_TERMS);
        boolean hasBoundImage = document.contains("data-image-section-id=") || document.contains("data-image-binding-key=");
        boolean hasImageNode = document.contains("<img") || document.contains("<picture")
                || document.contains("background-image");
        if (hasPreviewHints && !hasBoundImage) {
            if (hasImageNode) {
                return check("PREVIEW_CONCRETENESS", "FAIL", "Landing promete preview, mas não vincula imagem concreta.");
            }
            return check("PREVIEW_CONCRETENESS", "WARN", "HTML cita preview, porém sem bloco visual verificável.");
        }
        if (!hasPreviewHints && !hasBoundImage) {
            return check("PREVIEW_CONCRETENESS", "WARN", "HTML não evidencia preview visual concreto.");
        }
        return check("PREVIEW_CONCRETENESS", "PASS", "HTML contém sinais concretos de preview visual.");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> evaluateSectionThemeVariationFromWireframe(Map<String, Object> wireframe) {
        Object sectionsNode = wireframe != null ? wireframe.get("sectionOrder") : null;
        if (!(sectionsNode instanceof List<?> sections) || sections.isEmpty()) {
            return check("SECTION_THEME_VARIATION", "FAIL", "Wireframe sem seções para validar variação visual.");
        }
        Set<String> surfaceSignatures = new LinkedHashSet<>();
        for (Object section : sections) {
            if (!(section instanceof Map<?, ?> map)) {
                continue;
            }
            Object surfaceNode = ((Map<String, Object>) map).get("surfaceSpec");
            if (surfaceNode instanceof Map<?, ?> surface) {
                String signature = readString((Map<String, Object>) surface, "surfaceToken") + "|"
                        + readString((Map<String, Object>) surface, "style") + "|"
                        + readString((Map<String, Object>) surface, "contrastMode");
                if (StringUtils.hasText(signature.replace("|", ""))) {
                    surfaceSignatures.add(signature);
                }
            }
        }
        if (surfaceSignatures.size() <= 1 && sections.size() >= 3) {
            return check("SECTION_THEME_VARIATION", "FAIL", "Wireframe homogêneo: pouca variação de surfaceSpec por seção.");
        }
        if (surfaceSignatures.size() <= 2 && sections.size() >= 4) {
            return check("SECTION_THEME_VARIATION", "WARN", "Wireframe com variação visual limitada entre seções.");
        }
        return check("SECTION_THEME_VARIATION", "PASS", "Wireframe apresenta variação visual razoável por seção.");
    }

    private Map<String, Object> evaluateSectionThemeVariationFromHtml(Map<String, Object> html) {
        String document = readString(html, "htmlDocument");
        if (!StringUtils.hasText(document)) {
            return check("SECTION_THEME_VARIATION", "WARN", "HTML vazio; não foi possível avaliar variação visual.");
        }
        Set<String> signatures = new LinkedHashSet<>();
        Matcher sectionMatcher = HTML_SECTION_PATTERN.matcher(document);
        while (sectionMatcher.find()) {
            String openTag = sectionMatcher.group();
            Matcher attrMatcher = HTML_ATTRIBUTE_PATTERN.matcher(openTag);
            StringBuilder signature = new StringBuilder();
            while (attrMatcher.find()) {
                signature.append(attrMatcher.group(1).toLowerCase(Locale.ROOT))
                        .append("=")
                        .append(attrMatcher.group(2).toLowerCase(Locale.ROOT))
                        .append(";");
            }
            if (signature.length() > 0) {
                signatures.add(signature.toString());
            }
        }
        if (signatures.size() <= 1 && signatures.size() > 0) {
            return check("SECTION_THEME_VARIATION", "FAIL", "HTML com binding visual homogêneo entre seções.");
        }
        if (signatures.isEmpty()) {
            return check("SECTION_THEME_VARIATION", "WARN", "HTML sem marcações suficientes para avaliar variação visual.");
        }
        return check("SECTION_THEME_VARIATION", "PASS", "HTML preserva variação visual entre seções.");
    }

    private Map<String, Object> evaluateArtifactSchemaCompatibility(String artifactName,
                                                                    Map<String, Object> artifact,
                                                                    List<String> requiredFields) {
        if (artifact == null) {
            return check("ARTIFACT_SCHEMA_COMPATIBILITY", "FAIL",
                    "Artefato " + artifactName + " ausente no payload.");
        }
        List<String> missing = new ArrayList<>();
        for (String field : requiredFields) {
            Object value = artifact.get(field);
            if (value == null || (value instanceof String text && !StringUtils.hasText(text))) {
                missing.add(field);
            }
        }
        if (!missing.isEmpty()) {
            return check("ARTIFACT_SCHEMA_COMPATIBILITY", "FAIL",
                    "Artefato " + artifactName + " incompatível com campos obrigatórios: " + String.join(", ", missing) + ".");
        }
        boolean hasEmptyCollection = artifact.values().stream()
                .anyMatch(value -> value instanceof List<?> list && list.isEmpty());
        if (hasEmptyCollection) {
            return check("ARTIFACT_SCHEMA_COMPATIBILITY", "WARN",
                    "Artefato " + artifactName + " respeita schema mínimo, mas possui coleções vazias.");
        }
        return check("ARTIFACT_SCHEMA_COMPATIBILITY", "PASS",
                "Artefato " + artifactName + " compatível com o modelo canônico.");
    }

    private Map<String, Object> check(String name, String status, String details) {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("check", name);
        check.put("status", status);
        check.put("details", details);
        return check;
    }

    @SuppressWarnings("unchecked")
    private String collectCopyText(Map<String, Object> copy) {
        if (copy == null) {
            return "";
        }
        StringBuilder buffer = new StringBuilder();
        appendIfText(buffer, readString(copy, "pageGoal"));
        appendIfText(buffer, readString(copy, "messageMatchNotes"));
        appendIfText(buffer, readString(copy, "complianceNotes"));
        appendIfText(buffer, readString(copy, "primaryCTA"));
        Object heroNode = copy.get("hero");
        if (heroNode instanceof Map<?, ?> hero) {
            for (String key : List.of("headline", "subheadline", "promise", "supportingCopy", "proofBadge", "ctaLabel")) {
                appendIfText(buffer, readString((Map<String, Object>) hero, key));
            }
        }
        Object bodySectionsNode = copy.get("bodySections");
        if (bodySectionsNode instanceof List<?> sections) {
            for (Object section : sections) {
                if (section instanceof Map<?, ?> map) {
                    Map<String, Object> sectionMap = (Map<String, Object>) map;
                    for (String key : List.of("title", "summary", "copy", "ctaSupport", "messageMatchNotes")) {
                        appendIfText(buffer, readString(sectionMap, key));
                    }
                    for (String bullet : readStringList(sectionMap, "bullets")) {
                        appendIfText(buffer, bullet);
                    }
                }
            }
        }
        return buffer.toString().toLowerCase(Locale.ROOT);
    }

    private void appendIfText(StringBuilder buffer, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        buffer.append(value.trim()).append(' ');
    }

    private int countHits(String text, List<String> terms) {
        if (!StringUtils.hasText(text) || terms == null || terms.isEmpty()) {
            return 0;
        }
        int hits = 0;
        String normalizedText = text.toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (containsWord(normalizedText, term.toLowerCase(Locale.ROOT))) {
                hits++;
            }
        }
        return hits;
    }

    private boolean containsAny(String text, List<String> terms) {
        return countHits(text, terms) > 0;
    }

    private boolean containsWord(String text, String word) {
        Pattern pattern = Pattern.compile("(?iu)(^|\\W)" + Pattern.quote(word) + "(\\W|$)");
        return pattern.matcher(text).find();
    }

    private boolean isVagueCta(String label) {
        String normalized = label == null ? "" : label.trim().toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            return true;
        }
        if (VAGUE_CTA_LABELS.contains(normalized)) {
            return true;
        }
        return normalized.split("\\s+").length <= 2 && !hasExplicitNextStep(normalized);
    }

    private boolean hasExplicitNextStep(String label) {
        if (!StringUtils.hasText(label)) {
            return false;
        }
        String normalized = label.toLowerCase(Locale.ROOT);
        return containsAny(normalized, CTA_NEXT_STEP_TERMS) || normalized.split("\\s+").length >= 3;
    }

    private String readString(Map<String, Object> source, String key) {
        if (source == null || !StringUtils.hasText(key)) {
            return "";
        }
        Object value = source.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    @SuppressWarnings("unchecked")
    private List<String> readStringList(Map<String, Object> source, String key) {
        if (source == null || !StringUtils.hasText(key)) {
            return Collections.emptyList();
        }
        Object node = source.get(key);
        if (!(node instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (Object item : list) {
            if (item != null && StringUtils.hasText(String.valueOf(item))) {
                values.add(String.valueOf(item).trim());
            }
        }
        return values;
    }


    private String enforceRequiredModel(Map<String, Object> payload, ExperimentPipelineJobDto job) {
        String requiredModel = isLandingHtmlSection(job) ? REQUIRED_LANDING_HTML_MODEL : REQUIRED_TEXT_MODEL;
        if (payload == null) {
            return requiredModel;
        }
        String previousModel = payload.get("model") instanceof String value ? value : null;
        if (!requiredModel.equals(previousModel)) {
            log.warn(
                    "Forçando modelo OpenAI {} para pipeline (jobId={}, experimento={}, seção={}, modeloOriginal={})",
                    requiredModel,
                    job != null ? job.id() : null,
                    job != null ? job.experimentId() : null,
                    job != null ? job.section() : null,
                    previousModel);
        }
        payload.put("model", requiredModel);
        return requiredModel;
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
        if (isCampaignAngleSection(job) && !base.contains(CAMPAIGN_ANGLE_MARKER)) {
            return appendSectionTemplate(base, CAMPAIGN_ANGLE_TEMPLATE_PATH, job);
        }
        if (isLandingCopySection(job) && !base.contains(LANDING_COPY_MARKER)) {
            return appendSectionTemplate(base, LANDING_COPY_TEMPLATE_PATH, job);
        }
        if (isLandingLayoutSection(job) && !base.contains(LANDING_WIREFRAME_MARKER)) {
            return appendSectionTemplate(base, LANDING_WIREFRAME_TEMPLATE_PATH, job);
        }
        if (isLandingImagePlanningSection(job) && !base.contains(LANDING_IMAGE_PLANNING_MARKER)) {
            return appendSectionTemplate(base, LANDING_IMAGE_PLANNING_TEMPLATE_PATH, job);
        }
        if (isLandingHtmlSection(job) && !base.contains(LANDING_HTML_MARKER)) {
            return appendSectionTemplate(base, LANDING_HTML_TEMPLATE_PATH, job);
        }
        return base;
    }

    private String appendSectionTemplate(String basePrompt, String templatePath, ExperimentPipelineJobDto job) {
        PromptTemplate template = promptTemplates.get(templatePath);
        if (template == null || !StringUtils.hasText(template.body())) {
            return basePrompt;
        }
        return basePrompt + "\n\n" + applyTemplateVariables(template.body(), templateVariables(job));
    }

    private Map<String, String> templateVariables(ExperimentPipelineJobDto job) {
        Map<String, String> variables = new LinkedHashMap<>();
        for (String key : TEMPLATE_VARIABLE_KEYS) {
            variables.put(key, readTemplateVariable(job, key));
        }
        variables.put(CASE_DATA_BLOCK_KEY, buildCaseDataBlock(variables));
        return variables;
    }

    private String buildCaseDataBlock(Map<String, String> variables) {
        StringBuilder builder = new StringBuilder("[CASE_DATA_BEGIN]\n");
        for (String key : TEMPLATE_VARIABLE_KEYS) {
            String value = variables.get(key);
            builder.append(key)
                    .append(": ")
                    .append(StringUtils.hasText(value) ? value.trim() : "")
                    .append('\n');
        }
        builder.append("[CASE_DATA_END]");
        return builder.toString();
    }

    private String readTemplateVariable(ExperimentPipelineJobDto job, String key) {
        if (job == null || !StringUtils.hasText(job.prompt())) {
            return "";
        }
        Pattern pattern = Pattern.compile("(?im)^\\s*" + Pattern.quote(key) + "\\s*:\\s*(.+)$");
        Matcher matcher = pattern.matcher(job.prompt());
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private String applyTemplateVariables(String template, Map<String, String> variables) {
        if (!StringUtils.hasText(template) || variables == null || variables.isEmpty()) {
            return template;
        }
        String resolved = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = StringUtils.hasText(entry.getValue()) ? entry.getValue().trim() : "";
            resolved = resolved.replace("{{" + entry.getKey() + "}}", value);
        }
        return resolved;
    }

    private Map<String, PromptTemplate> loadPromptTemplates() {
        Map<String, PromptTemplate> templates = new LinkedHashMap<>();
        List<String> paths = List.of(
                CAMPAIGN_ANGLE_TEMPLATE_PATH,
                LANDING_COPY_TEMPLATE_PATH,
                LANDING_WIREFRAME_TEMPLATE_PATH,
                LANDING_IMAGE_PLANNING_TEMPLATE_PATH,
                LANDING_HTML_TEMPLATE_PATH);
        for (String path : paths) {
            templates.put(path, readPromptTemplate(path));
        }
        return Map.copyOf(templates);
    }

    private PromptTemplate readPromptTemplate(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        try {
            if (!resource.exists()) {
                log.warn("Template de prompt não encontrado no classpath: {}", path);
                return PromptTemplate.empty(path);
            }
            return parsePromptTemplate(path, StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            log.warn("Falha ao carregar template de prompt {}: {}", path, ex.getMessage());
            return PromptTemplate.empty(path);
        }
    }

    private PromptTemplate parsePromptTemplate(String path, String rawTemplate) {
        if (!StringUtils.hasText(rawTemplate)) {
            return PromptTemplate.empty(path);
        }
        String normalized = rawTemplate.replace("\r\n", "\n");
        String[] lines = normalized.split("\n", -1);
        Map<String, String> headerValues = new LinkedHashMap<>();
        int bodyStartIndex = 0;
        for (int index = 0; index < lines.length; index++) {
            String currentLine = lines[index];
            if (currentLine == null || currentLine.isBlank()) {
                bodyStartIndex = index + 1;
                break;
            }
            Matcher matcher = Pattern.compile("^\\s*([a-zA-Z_]+)\\s*:\\s*(.+?)\\s*$").matcher(currentLine);
            if (!matcher.matches()) {
                bodyStartIndex = index;
                break;
            }
            String key = matcher.group(1).trim().toLowerCase(Locale.ROOT);
            if (!"template_id".equals(key) && !"template_version".equals(key) && !"artifact_target".equals(key)) {
                bodyStartIndex = index;
                break;
            }
            headerValues.put(key, matcher.group(2).trim());
            bodyStartIndex = index + 1;
        }

        String body = String.join("\n", java.util.Arrays.copyOfRange(lines, bodyStartIndex, lines.length)).trim();
        String inferredTemplateId = inferTemplateId(path);
        String templateId = headerValues.getOrDefault("template_id", inferredTemplateId);
        String templateVersion = headerValues.getOrDefault("template_version", "unknown");
        String artifactTarget = headerValues.getOrDefault("artifact_target", inferArtifactTarget(path));
        return new PromptTemplate(path, body, templateId, templateVersion, artifactTarget);
    }

    private TemplateTrace resolveTemplateTrace(ExperimentPipelineJobDto job, String model) {
        String path = resolveTemplatePath(job);
        PromptTemplate template = path != null ? promptTemplates.get(path) : null;
        if (template == null) {
            return new TemplateTrace("unknown", "unknown", "unknown", model);
        }
        return new TemplateTrace(
                StringUtils.hasText(template.templateId()) ? template.templateId() : inferTemplateId(path),
                StringUtils.hasText(template.templateVersion()) ? template.templateVersion() : "unknown",
                StringUtils.hasText(template.artifactTarget()) ? template.artifactTarget() : inferArtifactTarget(path),
                model);
    }

    private String buildTrackedRequestBodyJson(Map<String, Object> payload, TemplateTrace trace) throws IOException {
        Map<String, Object> tracked = new LinkedHashMap<>();
        if (payload != null) {
            tracked.putAll(payload);
        }
        tracked.put("templateTrace", Map.of(
                "template_id", trace.templateId(),
                "template_version", trace.templateVersion(),
                "artifact_target", trace.artifactTarget(),
                "model", trace.model()));
        return objectMapper.writeValueAsString(tracked);
    }

    private String resolveTemplatePath(ExperimentPipelineJobDto job) {
        if (isCampaignAngleSection(job)) {
            return CAMPAIGN_ANGLE_TEMPLATE_PATH;
        }
        if (isLandingCopySection(job)) {
            return LANDING_COPY_TEMPLATE_PATH;
        }
        if (isLandingLayoutSection(job)) {
            return LANDING_WIREFRAME_TEMPLATE_PATH;
        }
        if (isLandingImagePlanningSection(job)) {
            return LANDING_IMAGE_PLANNING_TEMPLATE_PATH;
        }
        if (isLandingHtmlSection(job)) {
            return LANDING_HTML_TEMPLATE_PATH;
        }
        return null;
    }

    private String inferTemplateId(String path) {
        if (!StringUtils.hasText(path)) {
            return "unknown";
        }
        int slash = path.lastIndexOf('/');
        String fileName = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private String inferArtifactTarget(String path) {
        return switch (path) {
            case CAMPAIGN_ANGLE_TEMPLATE_PATH -> "campaignAngle";
            case LANDING_COPY_TEMPLATE_PATH -> "landingPageCopy";
            case LANDING_WIREFRAME_TEMPLATE_PATH -> "landingPageWireframe";
            case LANDING_IMAGE_PLANNING_TEMPLATE_PATH -> "landingPageImagePlanning";
            case LANDING_HTML_TEMPLATE_PATH -> "landingPageHtml";
            default -> "unknown";
        };
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

    private record PromptTemplate(
            String path,
            String body,
            String templateId,
            String templateVersion,
            String artifactTarget) {
        private static PromptTemplate empty(String path) {
            return new PromptTemplate(path, "", "unknown", "unknown", "unknown");
        }
    }

    private record TemplateTrace(
            String templateId,
            String templateVersion,
            String artifactTarget,
            String model) {
    }
}
