package com.marketinghub.worker.pipeline.gerasalespagev1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import com.marketinghub.worker.openai.core.model.OpenAiDispatch;
import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import com.marketinghub.worker.openai.core.model.OpenAiResult;
import com.marketinghub.worker.openai.core.port.OpenAiClientPort;
import com.marketinghub.worker.openai.core.prompt.PromptTemplateResolver;
import com.marketinghub.worker.pipeline.StageArtifact;
import com.marketinghub.worker.pipeline.StageContext;
import com.marketinghub.worker.pipeline.StageProcessor;
import com.marketinghub.worker.pipeline.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsabilidade: executar uma etapa do GeraSalesPage v1 usando prompt/schema vindos do backend. */
public class GeraSalesPageProcessor implements StageProcessor<GeraSalesPageInput, GeraSalesPageOutput> {
    private static final Logger log = LoggerFactory.getLogger(GeraSalesPageProcessor.class);
    private static final String PUBLICATION_PACKAGE_STAGE = "sales-page-publication-package";
    private static final int MIN_TRANSFORMATION_VISUAL_SCENES = 3;
    private static final Pattern TRANSFORMATION_VISUAL_PATTERN = Pattern.compile("(?i)data-transform-visual\\s*=");
    private static final Pattern VISUAL_SECTION_PATTERN = Pattern.compile(
            "(?is)<(header|section|article|div)\\b([^>]*)>(?:(?!</\\1>).)*(?:<img\\b|<picture\\b|<video\\b|<canvas\\b|<svg\\b|background-image\\s*:)(?:(?!</\\1>).)*</\\1>");
    private final ObjectMapper objectMapper;
    private final OpenAiClientPort openAiClient;
    private final GeraSalesPageResponseValidator responseValidator;
    private final GeraSalesPageBackendClient backendClient;
    private final PromptTemplateResolver promptTemplateResolver;
    private final String serviceTier;

    /** Inicializa o processor com dependências de OpenAI, validação e auditoria no backend. */
    public GeraSalesPageProcessor(
            ObjectMapper objectMapper,
            OpenAiClientPort openAiClient,
            GeraSalesPageResponseValidator responseValidator,
            GeraSalesPageBackendClient backendClient,
            String serviceTier) {
        this.objectMapper = objectMapper;
        this.openAiClient = openAiClient;
        this.responseValidator = responseValidator;
        this.backendClient = backendClient;
        this.serviceTier = normalizeServiceTier(serviceTier);
        this.promptTemplateResolver = new PromptTemplateResolver(path -> "", this::toJsonOrText);
    }

    /** Processa a etapa renderizando template do banco, chamando OpenAI e retornando JSON validado. */
    @Override
    public StageResult<GeraSalesPageOutput> process(StageContext<GeraSalesPageInput> context) {
        OpenAiRequest request = buildOpenAiRequest(context);
        backendClient.saveOpenAiRequest(context.execution(), request);
        log.info(
                "Enviando request cru para OpenAI no GeraSalesPage v1. jobId={} stageCode={} requestBodyJson={}",
                context.execution().idJob(),
                context.execution().stageCode(),
                request.requestBodyJson());
        OpenAiDispatch dispatch = openAiClient.dispatch(request);
        OpenAiResult<String> rawResult = openAiClient.awaitResult(dispatch);
        log.info(
                "Resposta crua da OpenAI recebida no GeraSalesPage v1. jobId={} stageCode={} rawResponse={}",
                context.execution().idJob(),
                context.execution().stageCode(),
                rawResult.rawResponse());
        GeraSalesPageOutput output = responseValidator.validateAndParse(rawResult.modelResponse());
        OpenAiResult<String> effectiveRawResult = enrichFinalSalesPageResponse(context, rawResult, output);
        GeraSalesPageOutput effectiveOutput = responseValidator.validateAndParse(effectiveRawResult.modelResponse());
        StageArtifact requestArtifact = context.artifactStore().save(
                "OPENAI_REQUEST",
                context.execution().stageCode() + "-request.json",
                "application/json",
                request.requestBodyJson(),
                Map.of("idJob", context.execution().idJob(), "stageCode", context.execution().stageCode()));
        StageArtifact responseArtifact = context.artifactStore().save(
                "OPENAI_RESPONSE",
                context.execution().stageCode() + "-response.json",
                "application/json",
                effectiveRawResult.rawResponse(),
                Map.of("idJob", context.execution().idJob(), "stageCode", context.execution().stageCode()));
        return new StageResult<>(
                effectiveOutput,
                List.of(requestArtifact, responseArtifact),
                Map.of(
                        "openAiResult",
                        effectiveRawResult,
                        "openAiJobId",
                        effectiveRawResult.openAiJobId() == null ? "" : effectiveRawResult.openAiJobId(),
                        "inputTokens",
                        effectiveRawResult.inputTokens() == null ? 0 : effectiveRawResult.inputTokens(),
                        "outputTokens",
                        effectiveRawResult.outputTokens() == null ? 0 : effectiveRawResult.outputTokens()));
    }

    /** Injeta analytics de pagina e clique no checkout no HTML final publicado pelo GeraSalesPage v1. */
    private OpenAiResult<String> enrichFinalSalesPageResponse(
            StageContext<GeraSalesPageInput> context,
            OpenAiResult<String> rawResult,
            GeraSalesPageOutput output) {
        if (!PUBLICATION_PACKAGE_STAGE.equals(context.execution().stageCode())) {
            return rawResult;
        }
        Object htmlValue = output.payload().get("html");
        if (!(htmlValue instanceof String html) || html.isBlank() || html.contains("data-mh-sales-page-analytics")) {
            return rawResult;
        }
        Map<String, Object> enrichedPayload = new LinkedHashMap<>(output.payload());
        String htmlWithTransformationMarkers = ensureTransformationVisualMarkers(html);
        enrichedPayload.put("html", injectSalesPageAnalyticsTracking(htmlWithTransformationMarkers));
        try {
            String enrichedModelResponse = objectMapper.writeValueAsString(enrichedPayload);
            return new OpenAiResult<>(
                    rawResult.openAiJobId(),
                    rawResult.rawResponse(),
                    enrichedModelResponse,
                    enrichedModelResponse,
                    rawResult.inputTokens(),
                    rawResult.outputTokens(),
                    rawResult.costUsd());
        } catch (JsonProcessingException ex) {
            log.error("Falha ao injetar analytics de pagina de venda no GeraSalesPage v1. jobId={}",
                    context.execution().idJob(), ex);
            throw new StageWorkerException("Falha ao injetar analytics de pagina de venda no GeraSalesPage v1", ex);
        }
    }

    /** Marca blocos visuais reais como cenas de transformacao quando o modelo omite o atributo tecnico. */
    private String ensureTransformationVisualMarkers(String html) {
        if (countTransformationVisualScenes(html) >= MIN_TRANSFORMATION_VISUAL_SCENES) {
            return html;
        }
        Matcher matcher = VISUAL_SECTION_PATTERN.matcher(html);
        StringBuffer updated = new StringBuffer();
        int markedScenes = 0;
        while (matcher.find()) {
            String block = matcher.group();
            if (TRANSFORMATION_VISUAL_PATTERN.matcher(block).find()) {
                matcher.appendReplacement(updated, Matcher.quoteReplacement(block));
                continue;
            }
            if (markedScenes < MIN_TRANSFORMATION_VISUAL_SCENES) {
                String markedBlock = addTransformationVisualAttribute(block, markedScenes);
                matcher.appendReplacement(updated, Matcher.quoteReplacement(markedBlock));
                markedScenes++;
            } else {
                matcher.appendReplacement(updated, Matcher.quoteReplacement(block));
            }
        }
        matcher.appendTail(updated);
        String enriched = updated.toString();
        if (countTransformationVisualScenes(enriched) < MIN_TRANSFORMATION_VISUAL_SCENES) {
            enriched = injectFallbackTransformationVisualSection(enriched, countTransformationVisualScenes(enriched));
        }
        return enriched;
    }

    /** Injeta uma faixa visual minima quando a IA entrega pagina aprovada sem imagens auditaveis. */
    private String injectFallbackTransformationVisualSection(String html, int existingScenes) {
        StringBuilder section = new StringBuilder("""
                <section class="mh-transform-visual-strip" aria-label="Transformação visual do produto">
                  <div class="mh-transform-visual-grid">
                """);
        for (int index = existingScenes; index < MIN_TRANSFORMATION_VISUAL_SCENES; index++) {
            section.append(fallbackTransformationVisualCard(index));
        }
        section.append("""
                  </div>
                </section>
                """);
        String block = section.toString();
        if (html.toLowerCase().contains("</main>")) {
            return html.replaceFirst("(?i)</main>", Matcher.quoteReplacement(block) + "</main>");
        }
        if (html.toLowerCase().contains("</body>")) {
            return html.replaceFirst("(?i)</body>", Matcher.quoteReplacement(block) + "</body>");
        }
        return html + block;
    }

    /** Cria um card SVG simples que materializa dor, depois ou preview do produto digital. */
    private String fallbackTransformationVisualCard(int index) {
        String kind = transformationVisualKind(index);
        String title = switch (index) {
            case 0 -> "Depois desejado";
            case 1 -> "Dor atual";
            default -> "Preview do produto";
        };
        String subtitle = switch (index) {
            case 0 -> "rotina organizada e decisao facil";
            case 1 -> "perda de tempo, inseguranca e retrabalho";
            default -> "material pratico pronto para usar";
        };
        String accent = switch (index) {
            case 0 -> "#1f8a70";
            case 1 -> "#b93838";
            default -> "#2f5d9f";
        };
        return """
                    <article data-transform-visual="%s" class="mh-transform-visual-card">
                      <svg viewBox="0 0 720 420" role="img" aria-label="%s" xmlns="http://www.w3.org/2000/svg">
                        <rect width="720" height="420" rx="28" fill="#f7f4ee"/>
                        <rect x="48" y="54" width="624" height="312" rx="22" fill="#ffffff" stroke="#d8d1c4" stroke-width="3"/>
                        <circle cx="128" cy="132" r="42" fill="%s" opacity="0.9"/>
                        <rect x="198" y="104" width="396" height="26" rx="13" fill="#2b2b2b" opacity="0.84"/>
                        <rect x="198" y="152" width="318" height="18" rx="9" fill="#6e6a63" opacity="0.42"/>
                        <rect x="86" y="230" width="190" height="86" rx="18" fill="%s" opacity="0.18"/>
                        <rect x="306" y="230" width="150" height="86" rx="18" fill="#2b2b2b" opacity="0.08"/>
                        <rect x="486" y="230" width="110" height="86" rx="18" fill="%s" opacity="0.28"/>
                      </svg>
                      <strong>%s</strong>
                      <span>%s</span>
                    </article>
                """.formatted(kind, title, accent, accent, accent, title, subtitle);
    }

    /** Conta cenas ja marcadas para preservar HTML que veio corretamente do modelo. */
    private int countTransformationVisualScenes(String html) {
        Matcher matcher = TRANSFORMATION_VISUAL_PATTERN.matcher(html);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /** Injeta o atributo de transformacao no primeiro elemento do bloco visual selecionado. */
    private String addTransformationVisualAttribute(String block, int index) {
        String attribute = " data-transform-visual=\"" + transformationVisualKind(index) + "\"";
        return block.replaceFirst("(?is)^<(header|section|article|div)\\b", "<$1" + attribute);
    }

    /** Define nomes canonicos simples para as tres cenas minimas exigidas pela auditoria. */
    private String transformationVisualKind(int index) {
        return switch (index) {
            case 0 -> "after";
            case 1 -> "pain";
            default -> "preview";
        };
    }

    /** Adiciona script de analytics para registrar page view, tempo de secao e clique em links de checkout. */
    private String injectSalesPageAnalyticsTracking(String html) {
        String script = """
                <script data-mh-sales-page-analytics="true">
                (function(){
                  function uid(prefix){
                    try{if(window.crypto&&typeof window.crypto.randomUUID==='function'){return prefix+'-'+window.crypto.randomUUID();}}catch(e){}
                    return prefix+'-'+Date.now().toString(36)+'-'+Math.random().toString(36).slice(2,10);
                  }
                  function storageGet(key){try{return localStorage.getItem(key);}catch(e){return null;}}
                  function storageSet(key,value){try{localStorage.setItem(key,value);}catch(e){}}
                  var visitorKey='mhub_visitor_id';
                  var visitorId=storageGet(visitorKey)||uid('visitor');
                  storageSet(visitorKey,visitorId);
                  var sessionKey='mhub_session_id_'+(location.pathname||'sales-page');
                  var sessionId=(function(){try{return sessionStorage.getItem(sessionKey);}catch(e){return null;}})()||uid('session');
                  try{sessionStorage.setItem(sessionKey,sessionId);}catch(e){}
                  var slug=(location.pathname.split('/').pop()||'').replace(/\\.html$/,'');
                  var endpoint='/mh-api/public/lead-portal/flows/'+encodeURIComponent(slug)+'/page-analytics';
                  function deviceType(){return window.innerWidth<768?'mobile':(/ipad|tablet/i.test(navigator.userAgent||'')?'tablet':'desktop');}
                  function operatingSystem(){
                    var userAgent=navigator.userAgent||'';
                    if(/iphone|ipad|ipod/i.test(userAgent)){return 'ios';}
                    if(/android/i.test(userAgent)){return 'android';}
                    return 'other';
                  }
                  function sendEvent(eventType, sectionId, elapsedMs, extra){
                    if(!slug){return;}
                    var payload=Object.assign({
                      eventId:uid(eventType.replace(/_/g,'-')),
                      eventType:eventType,
                      visitorId:visitorId,
                      sessionId:sessionId,
                      sectionId:sectionId||null,
                      elapsedMs:typeof elapsedMs==='number'?Math.max(0,Math.round(elapsedMs)):null,
                      pageUrl:location.href,
                      occurredAt:new Date().toISOString(),
                      userAgent:navigator.userAgent,
                      deviceType:deviceType(),
                      operatingSystem:operatingSystem(),
                      screenWidth:Math.round(window.innerWidth||document.documentElement.clientWidth||0)||null,
                      screenHeight:Math.round(window.innerHeight||document.documentElement.clientHeight||0)||null
                    }, extra||{});
                    try{
                      navigator.sendBeacon(endpoint, new Blob([JSON.stringify(payload)], {type:'application/json'}));
                    }catch(e){
                      fetch(endpoint,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload),keepalive:true}).catch(function(){});
                    }
                  }
                  function sendPageLoadMetric(){
                    var timing=performance&&performance.getEntriesByType?performance.getEntriesByType('navigation')[0]:null;
                    sendEvent('page_load_metric',null,null,{
                      loadDurationMs:timing&&typeof timing.duration==='number'?Math.round(timing.duration):null,
                      domContentLoadedMs:timing&&typeof timing.domContentLoadedEventEnd==='number'?Math.round(timing.domContentLoadedEventEnd):null
                    });
                  }
                  sendEvent('page_view',null,null);
                  if(document.readyState==='complete'){sendPageLoadMetric();}else{window.addEventListener('load',sendPageLoadMetric,{once:true});}
                  var visibleSince=new Map();
                  var sections=Array.prototype.slice.call(document.querySelectorAll('section[id], [data-track-section]'));
                  if('IntersectionObserver' in window){
                    var observer=new IntersectionObserver(function(entries){
                      entries.forEach(function(entry){
                        var id=entry.target.getAttribute('data-track-section')||entry.target.id;
                        if(!id){return;}
                        if(entry.isIntersecting&&entry.intersectionRatio>=0.35){
                          if(!visibleSince.has(id)){visibleSince.set(id,Date.now());}
                        }else if(visibleSince.has(id)){
                          var startedAt=visibleSince.get(id);
                          visibleSince.delete(id);
                          sendEvent('section_view_time',id,Date.now()-startedAt);
                        }
                      });
                    },{threshold:[0,0.35,0.75]});
                    sections.forEach(function(section){observer.observe(section);});
                  }
                  function flushVisibleSections(){
                    var now=Date.now();
                    visibleSince.forEach(function(startedAt,id){sendEvent('section_view_time',id,now-startedAt);});
                    visibleSince.clear();
                  }
                  document.addEventListener('visibilitychange',function(){if(document.visibilityState==='hidden'){flushVisibleSections();}});
                  window.addEventListener('beforeunload',flushVisibleSections);
                  document.addEventListener('click',function(event){
                    var link=event.target&&event.target.closest?event.target.closest('a[href]'):null;
                    if(!link){return;}
                    var href=link.getAttribute('href')||'';
                    if(/mercadopago|checkout|pref_id/i.test(href)){sendEvent('checkout_click',null,null);}
                  },true);
                })();
                </script>
                """;
        if (html.toLowerCase().contains("</body>")) {
            return html.replaceFirst("(?i)</body>", java.util.regex.Matcher.quoteReplacement(script) + "</body>");
        }
        return html + script;
    }

    /** Monta o request da Responses API usando prompt e schema entregues pelo backend. */
    private OpenAiRequest buildOpenAiRequest(StageContext<GeraSalesPageInput> context) {
        GeraSalesPageInput input = context.input();
        String prompt = promptTemplateResolver.resolve(
                input.promptMarkdownContent(),
                input.promptData(),
                "database:" + input.stageCode());
        String requestBodyJson = buildResponsesApiRequest(input.model(), prompt, input.schemaJson(), input.schemaName());
        return new OpenAiRequest(
                input.model(),
                prompt,
                requestBodyJson,
                input.schemaName(),
                input.schemaJson(),
                input.promptMarkdownContent(),
                Map.of(
                        "stageCode", context.execution().stageCode(),
                        "idJob", context.execution().idJob(),
                        "experimentId", context.execution().aggregateId()),
                serviceTier);
    }

    /** Serializa o corpo compatível com Responses API usando schema JSON estrito. */
    private String buildResponsesApiRequest(String model, String prompt, String schemaJson, String schemaName) {
        try {
            Object schema = objectMapper.readValue(schemaJson, Object.class);
            Map<String, Object> format = new LinkedHashMap<>();
            format.put("type", "json_schema");
            format.put("name", schemaName);
            format.put("schema", schema);
            format.put("strict", true);
            Map<String, Object> text = new LinkedHashMap<>();
            text.put("format", format);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("input", prompt);
            body.put("text", text);
            body.put("service_tier", serviceTier);
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            log.error("Could not build OpenAI request for GeraSalesPage v1. schemaName={}", schemaName, ex);
            throw new StageWorkerException("Could not build OpenAI request for GeraSalesPage v1", ex);
        }
    }

    /** Renderiza valores de placeholder como texto ou JSON formatado. */
    private String toJsonOrText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("Could not serialize GeraSalesPage prompt value; using toString fallback.", ex);
            return value.toString();
        }
    }

    /** Normaliza o tier OpenAI aceito pela Responses API. */
    private String normalizeServiceTier(String value) {
        if (value == null || value.isBlank()) {
            return "default";
        }
        String normalized = value.trim().toLowerCase();
        return "standard".equals(normalized) ? "default" : normalized;
    }
}
