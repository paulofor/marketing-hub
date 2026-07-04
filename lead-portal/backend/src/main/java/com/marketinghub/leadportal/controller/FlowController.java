package com.marketinghub.leadportal.controller;

import com.marketinghub.leadportal.dto.FlowResponse;
import com.marketinghub.leadportal.dto.UpsertFlowRequest;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowQuestion;
import com.marketinghub.leadportal.model.FlowAccessMetadata;
import com.marketinghub.leadportal.model.SimpleFormStyle;
import com.marketinghub.leadportal.service.FlowService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller que publica e entrega fluxos públicos do Lead Portal, incluindo landings standalone.
 */
@RestController
@RequestMapping("/api/flows")
@CrossOrigin
@Validated
public class FlowController {

    private final FlowService flowService;

    /**
     * Inicializa o controller com o serviço de fluxos públicos.
     */
    public FlowController(FlowService flowService) {
        this.flowService = flowService;
    }

    /**
     * Cria ou atualiza um fluxo público com seu HTML, perguntas e integrações de tracking.
     */
    @PutMapping("/{slug}")
    public FlowResponse upsertFlow(@PathVariable("slug") String slug,
                                   @Valid @RequestBody UpsertFlowRequest request) {
        List<com.marketinghub.leadportal.dto.FlowQuestionRequest> questionRequests =
                request.getQuestions() == null ? List.of() : request.getQuestions();

        Flow flow = new Flow(
                slug,
                request.getName(),
                request.getDescription(),
                request.getCustomFormHtml(),
                request.getModel(),
                request.getPrompt(),
                request.getImagePromptModel(),
                request.getImagePromptTemplate(),
                request.getImageBatchSize(),
                questionRequests.stream().map(this::toQuestion).toList(),
                mapStyle(request.getSimpleFormStyle()),
                request.getFacebookPixelId(),
                request.getFacebookPixelCode(),
                request.getFacebookPixelCreatedAt());
        return FlowResponse.from(flowService.save(flow));
    }

    /**
     * Retorna o contrato público do fluxo e registra o acesso recebido.
     */
    @GetMapping("/{slug}")
    public FlowResponse getFlow(@PathVariable("slug") String slug, HttpServletRequest request) {
        FlowAccessMetadata accessMetadata = FlowAccessMetadata.from(request);
        return FlowResponse.from(flowService.getAndTrackAccess(slug, accessMetadata));
    }

    /**
     * Entrega a landing standalone com analytics dinâmico para alimentar o funil do experimento.
     */
    @GetMapping(value = "/{slug}/page", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getStandaloneFlowPage(
            @PathVariable("slug") String slug,
            HttpServletRequest request) {
        FlowAccessMetadata accessMetadata = FlowAccessMetadata.from(request);
        Flow flow = flowService.getAndTrackAccess(slug, accessMetadata);
        String html = flow.customFormHtml();
        if (!isStandaloneHtmlDocument(html)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Fluxo não possui HTML standalone.");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(injectLandingAnalyticsScript(slug, html));
    }

    /**
     * Remove um fluxo publicado pelo slug informado.
     */
    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> deleteFlow(@PathVariable("slug") String slug) {
        flowService.delete(slug);
        return ResponseEntity.noContent().build();
    }

    /**
     * Converte o payload de estilo do contrato HTTP para o modelo do domínio.
     */
    private SimpleFormStyle mapStyle(UpsertFlowRequest.SimpleFormStylePayload payload) {
        if (payload == null) {
            return null;
        }
        return new SimpleFormStyle(payload.getSlug(), payload.getName(), payload.getDefinition());
    }

    /**
     * Converte uma pergunta recebida na API para o modelo interno do fluxo.
     */
    private FlowQuestion toQuestion(com.marketinghub.leadportal.dto.FlowQuestionRequest request) {
        List<String> options = request.getOptions() == null
                ? List.of()
                : request.getOptions().stream().map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.toList());
        return new FlowQuestion(
                request.getTitle(),
                request.getDataKey(),
                request.getType(),
                request.isRequired(),
                request.getDescription(),
                request.getPlaceholder(),
                options);
    }

    /**
     * Injeta ou atualiza o script de analytics do Lead Portal com diagnóstico opcional no browser.
     */
    private String injectLandingAnalyticsScript(String slug, String html) {
        if (html == null) {
            return null;
        }
        if (html.toLowerCase(Locale.ROOT).contains("data-mh-landing-analytics")) {
            return refreshLandingAnalyticsScriptWhenMissingDebug(slug, html);
        }

        String analyticsScript = buildLandingAnalyticsScript(slug);
        if (html.toLowerCase(Locale.ROOT).contains("</body>")) {
            return html.replaceFirst("(?i)</body>", java.util.regex.Matcher.quoteReplacement(analyticsScript + "\n</body>"));
        }
        return html + "\n" + analyticsScript;
    }

    /**
     * Atualiza a instrumentação legada já existente para incluir logs de diagnóstico sem duplicar scripts.
     */
    private String refreshLandingAnalyticsScriptWhenMissingDebug(String slug, String html) {
        if (html.contains("mhAnalyticsDebug")) {
            return html;
        }
        String analyticsScript = buildLandingAnalyticsScript(slug);
        return java.util.regex.Pattern
                .compile("<script\\b(?=[^>]*data-mh-landing-analytics)[\\s\\S]*?</script>",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(html)
                .replaceFirst(java.util.regex.Matcher.quoteReplacement(analyticsScript));
    }

    /**
     * Monta o script de analytics que envia page_view e expõe logs de console somente quando o debug é ativado.
     */
    private String buildLandingAnalyticsScript(String slug) {
        return """
                <script data-mh-landing-analytics="true">
                (function(){
                  const slugValue = %s;
                  const endpoint = '/api/flows/' + encodeURIComponent(slugValue) + '/page-analytics';
                  const debugParam = new URLSearchParams(window.location.search).get('mhAnalyticsDebug') === '1';
                  const debugStorage = localStorage.getItem('mhLandingAnalyticsDebug') === 'true';
                  const debugEnabled = debugParam || debugStorage;
                  const debugLog = function(message, details){
                    if (!debugEnabled || !window.console || !window.console.log) return;
                    window.console.log('[MH Landing Analytics] ' + message, details || {});
                  };
                  const sessionKey = 'mh_lp_session_' + slugValue;
                  const sessionId = sessionStorage.getItem(sessionKey) || (Date.now().toString(36) + '-' + Math.random().toString(36).slice(2));
                  sessionStorage.setItem(sessionKey, sessionId);
                  debugLog('script carregado', {slug: slugValue, endpoint: endpoint, sessionId: sessionId, readyState: document.readyState});
                  const buildEventId = function(){
                    return crypto.randomUUID ? crypto.randomUUID() : (Date.now().toString(36) + '-' + Math.random().toString(36).slice(2));
                  };
                  const resolveDeviceType = function(){
                    const userAgent = navigator.userAgent || '';
                    const isTablet = /ipad|tablet/i.test(userAgent) || (/android/i.test(userAgent) && !/mobile/i.test(userAgent));
                    if (isTablet) return 'tablet';
                    const isMobile = /mobi|iphone|ipod|android/i.test(userAgent);
                    return isMobile ? 'mobile' : 'desktop';
                  };
                  const resolveOperatingSystem = function(){
                    const userAgent = navigator.userAgent || '';
                    if (/iphone|ipad|ipod/i.test(userAgent)) return 'ios';
                    if (/android/i.test(userAgent)) return 'android';
                    return 'other';
                  };
                  const resolveScreenSize = function(){
                    const width = window.innerWidth || document.documentElement.clientWidth || screen.width || null;
                    const height = window.innerHeight || document.documentElement.clientHeight || screen.height || null;
                    return {
                      width: typeof width === 'number' ? Math.round(width) : null,
                      height: typeof height === 'number' ? Math.round(height) : null
                    };
                  };
                  const deviceType = resolveDeviceType();
                  const operatingSystem = resolveOperatingSystem();
                  let resourceErrorCount = 0;
                  window.addEventListener('error', function(event){
                    const target = event && event.target;
                    if (target && target !== window && (target.src || target.href)) {
                      resourceErrorCount += 1;
                    }
                  }, true);
                  const buildPerformanceMetrics = function(){
                    const navigation = performance && performance.getEntriesByType ? performance.getEntriesByType('navigation')[0] : null;
                    const timing = navigation || (performance && performance.timing ? performance.timing : null);
                    const origin = navigation ? 0 : (timing && timing.navigationStart ? timing.navigationStart : 0);
                    const metric = function(name){
                      if (!timing || typeof timing[name] !== 'number') return null;
                      const value = timing[name] - origin;
                      return value > 0 ? Math.round(value) : null;
                    };
                    let firstContentfulPaintMs = null;
                    if (performance && performance.getEntriesByName) {
                      const paints = performance.getEntriesByName('first-contentful-paint');
                      if (paints && paints.length) firstContentfulPaintMs = Math.round(paints[0].startTime);
                    }
                    return {
                      loadDurationMs: metric('loadEventEnd') || metric('duration'),
                      domContentLoadedMs: metric('domContentLoadedEventEnd'),
                      firstContentfulPaintMs: firstContentfulPaintMs,
                      resourceErrorCount: resourceErrorCount,
                      connectionType: navigator.connection && navigator.connection.effectiveType ? navigator.connection.effectiveType : null
                    };
                  };
                  const sendEvent = function(eventType, sectionId, elapsedMs, extra){
                    const roundedElapsed = typeof elapsedMs === 'number' ? Math.round(elapsedMs) : null;
                    const payload = Object.assign({
                      eventId: buildEventId(),
                      eventType: eventType,
                      sessionId: sessionId,
                      sectionId: sectionId || null,
                      elapsedMs: roundedElapsed,
                      visibleMs: roundedElapsed,
                      pageUrl: window.location.href,
                      occurredAt: new Date().toISOString(),
                      userAgent: navigator.userAgent,
                      deviceType: deviceType,
                      operatingSystem: operatingSystem,
                      screenWidth: resolveScreenSize().width,
                      screenHeight: resolveScreenSize().height
                    }, extra || {});
                    debugLog('enviando evento', {endpoint: endpoint, payload: payload});
                    if (navigator.sendBeacon) {
                      const accepted = navigator.sendBeacon(endpoint, new Blob([JSON.stringify(payload)], {type: 'application/json'}));
                      debugLog('sendBeacon executado', {eventId: payload.eventId, eventType: eventType, accepted: accepted});
                      return;
                    }
                    fetch(endpoint, {method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload), keepalive: true})
                      .then(function(response){ debugLog('fetch concluído', {eventId: payload.eventId, eventType: eventType, status: response.status}); })
                      .catch(function(error){ debugLog('fetch falhou', {eventId: payload.eventId, eventType: eventType, error: error && error.message ? error.message : String(error)}); });
                  };
                  const resolveFormTarget = function(){
                    return document.querySelector('form');
                  };
                  const isSelfReferentialLink = function(anchor){
                    if (!anchor || !anchor.href) return false;
                    try {
                      const targetUrl = new URL(anchor.href, window.location.href);
                      const currentUrl = new URL(window.location.href);
                      const targetPath = targetUrl.pathname.replace(/\\/$/, '');
                      const currentPath = currentUrl.pathname.replace(/\\/$/, '');
                      if (targetUrl.origin === currentUrl.origin && targetPath === currentPath) return true;
                      return targetPath.endsWith('/flows/' + slugValue) || targetPath.endsWith('/flows/' + slugValue + '/page');
                    } catch (error) {
                      return false;
                    }
                  };
                  const startTracking = function(){
                    debugLog('tracking iniciado', {slug: slugValue});
                    sendEvent('page_view', null, null);
                    const sendLoadMetric = function(){
                      setTimeout(function(){
                        sendEvent('page_load_metric', null, null, buildPerformanceMetrics());
                      }, 0);
                    };
                    if (document.readyState === 'complete') {
                      sendLoadMetric();
                    } else {
                      window.addEventListener('load', sendLoadMetric, {once:true});
                    }
                    const visibleSince = new Map();
                    const observer = new IntersectionObserver(function(entries){
                      const now = performance.now();
                      entries.forEach(function(entry){
                        const sectionId = entry.target.id || entry.target.getAttribute('data-section-id') || entry.target.getAttribute('data-track-section');
                        if (!sectionId) return;
                        if (entry.isIntersecting && entry.intersectionRatio >= 0.5) {
                          if (!visibleSince.has(sectionId)) {
                            visibleSince.set(sectionId, now);
                            debugLog('seção visível', {sectionId: sectionId});
                          }
                        } else if (visibleSince.has(sectionId)) {
                          const startedAt = visibleSince.get(sectionId);
                          visibleSince.delete(sectionId);
                          sendEvent('section_view_time', sectionId, now - startedAt);
                        }
                      });
                    }, {threshold:[0.5]});
                    const trackedSections = document.querySelectorAll('section[id], [data-section-id], [data-track-section]');
                    debugLog('seções monitoradas', {count: trackedSections.length});
                    trackedSections.forEach(function(el){ observer.observe(el); });
                    document.addEventListener('click', function(event){
                      const anchor = event.target && event.target.closest ? event.target.closest('a[href]') : null;
                      if (!anchor || !isSelfReferentialLink(anchor)) return;
                      const formTarget = resolveFormTarget();
                      if (!formTarget) return;
                      event.preventDefault();
                      formTarget.scrollIntoView({behavior:'smooth', block:'start'});
                    }, true);
                    const startedForms = new WeakSet();
                    const trackFormStart = function(event){
                      const form = event.target && event.target.closest ? event.target.closest('form') : null;
                      if (!form || startedForms.has(form)) return;
                      startedForms.add(form);
                      sendEvent('form_start', null, null);
                    };
                    document.addEventListener('input', trackFormStart, true);
                    document.addEventListener('change', trackFormStart, true);
                    document.addEventListener('submit', function(event){
                      const form = event.target && event.target.tagName && event.target.tagName.toLowerCase() === 'form' ? event.target : null;
                      if (!form) return;
                      sendEvent('form_submit', null, null);
                    }, true);
                    window.addEventListener('beforeunload', function(){
                      const now = performance.now();
                      debugLog('beforeunload processando seções visíveis', {count: visibleSince.size});
                      visibleSince.forEach(function(startedAt, sectionId){ sendEvent('section_view_time', sectionId, now - startedAt); });
                    });
                  };
                  if (document.readyState === 'loading') {
                    debugLog('aguardando DOMContentLoaded', {readyState: document.readyState});
                    document.addEventListener('DOMContentLoaded', startTracking);
                  } else {
                    startTracking();
                  }
                })();
                </script>
                """.formatted(toJsStringLiteral(slug));
    }

    /**
     * Escapa um valor Java para uso seguro como literal de string em JavaScript.
     */
    private String toJsStringLiteral(String rawValue) {
        String safeValue = rawValue == null ? "" : rawValue;
        return "\"" + safeValue
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\"";
    }

    /**
     * Verifica se o HTML salvo representa um documento standalone completo.
     */
    private boolean isStandaloneHtmlDocument(String html) {
        if (html == null) {
            return false;
        }
        String normalized = html.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("<!doctype")
                || normalized.startsWith("<html")
                || normalized.startsWith("<body");
    }
}
