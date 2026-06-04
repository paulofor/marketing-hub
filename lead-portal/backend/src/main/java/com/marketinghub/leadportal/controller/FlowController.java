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
     * Injeta o script de analytics do Lead Portal sem alterar landings que já possuem a instrumentação.
     */
    private String injectLandingAnalyticsScript(String slug, String html) {
        if (html == null || html.toLowerCase(Locale.ROOT).contains("data-mh-landing-analytics")) {
            return html;
        }
        String analyticsScript = """
                <script data-mh-landing-analytics="true">
                (function(){
                  const slugValue = %s;
                  const endpoint = '/api/flows/' + encodeURIComponent(slugValue) + '/page-analytics';
                  const sessionKey = 'mh_lp_session_' + slugValue;
                  const sessionId = sessionStorage.getItem(sessionKey) || (Date.now().toString(36) + '-' + Math.random().toString(36).slice(2));
                  sessionStorage.setItem(sessionKey, sessionId);
                  const buildEventId = function(){
                    return crypto.randomUUID ? crypto.randomUUID() : (Date.now().toString(36) + '-' + Math.random().toString(36).slice(2));
                  };
                  const sendEvent = function(eventType, sectionId, elapsedMs){
                    const roundedElapsed = typeof elapsedMs === 'number' ? Math.round(elapsedMs) : null;
                    const payload = {
                      eventId: buildEventId(),
                      eventType: eventType,
                      sessionId: sessionId,
                      sectionId: sectionId || null,
                      elapsedMs: roundedElapsed,
                      visibleMs: roundedElapsed,
                      pageUrl: window.location.href,
                      occurredAt: new Date().toISOString(),
                      userAgent: navigator.userAgent
                    };
                    if (navigator.sendBeacon) {
                      navigator.sendBeacon(endpoint, new Blob([JSON.stringify(payload)], {type: 'application/json'}));
                      return;
                    }
                    fetch(endpoint, {method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload), keepalive: true}).catch(function(){});
                  };
                  const startTracking = function(){
                    sendEvent('page_view', null, null);
                    const visibleSince = new Map();
                    const observer = new IntersectionObserver(function(entries){
                      const now = performance.now();
                      entries.forEach(function(entry){
                        const sectionId = entry.target.id || entry.target.getAttribute('data-section-id') || entry.target.getAttribute('data-track-section');
                        if (!sectionId) return;
                        if (entry.isIntersecting && entry.intersectionRatio >= 0.5) {
                          if (!visibleSince.has(sectionId)) visibleSince.set(sectionId, now);
                        } else if (visibleSince.has(sectionId)) {
                          const startedAt = visibleSince.get(sectionId);
                          visibleSince.delete(sectionId);
                          sendEvent('section_view_time', sectionId, now - startedAt);
                        }
                      });
                    }, {threshold:[0.5]});
                    document.querySelectorAll('section[id], [data-section-id], [data-track-section]').forEach(function(el){ observer.observe(el); });
                    window.addEventListener('beforeunload', function(){
                      const now = performance.now();
                      visibleSince.forEach(function(startedAt, sectionId){ sendEvent('section_view_time', sectionId, now - startedAt); });
                    });
                  };
                  if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', startTracking);
                  } else {
                    startTracking();
                  }
                })();
                </script>
                """.formatted(toJsStringLiteral(slug));
        if (html.toLowerCase(Locale.ROOT).contains("</body>")) {
            return html.replaceFirst("(?i)</body>", java.util.regex.Matcher.quoteReplacement(analyticsScript + "\n</body>"));
        }
        return html + "\n" + analyticsScript;
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
