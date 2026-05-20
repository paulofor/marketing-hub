package com.marketinghub.leadportal.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublicationRequest;
import com.marketinghub.leadportal.service.LeadPortalFlowService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Iterator;
import java.util.Locale;

/**
 * Public API used by the portal application to fetch approved lead portal flows by slug.
 */
@RestController
@RequestMapping("/api/flows")
public class LeadPortalPublicFlowController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final LeadPortalFlowService flowService;

    public LeadPortalPublicFlowController(LeadPortalFlowService flowService) {
        this.flowService = flowService;
    }

    @GetMapping("/{slug}")
    public LeadPortalFlowPublicationRequest getBySlug(@PathVariable String slug) {
        LeadPortalFlow flow = flowService.getApprovedBySlug(slug);
        return LeadPortalFlowPublicationRequest.from(flow);
    }

    @GetMapping(value = "/{slug}/page", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getLandingPageBySlug(@PathVariable String slug) {
        LeadPortalFlow flow = flowService.getApprovedBySlug(slug);
        String htmlDocument = injectLandingAnalyticsScript(slug, extractHtmlDocument(flow.getCustomFormHtml()));
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", java.nio.charset.StandardCharsets.UTF_8))
                .body(htmlDocument);
    }

    private String extractHtmlDocument(String sourceHtml) {
        if (!StringUtils.hasText(sourceHtml)) {
            return "";
        }
        String trimmedSource = sourceHtml.trim();
        if (looksLikeJsonPayload(trimmedSource)) {
            String extractedFromJson = tryExtractFromJsonPayload(trimmedSource);
            if (StringUtils.hasText(extractedFromJson)) {
                return extractedFromJson;
            }
        }
        String extractedFromHybrid = tryExtractFromHybridHtml(sourceHtml);
        if (StringUtils.hasText(extractedFromHybrid)) {
            return extractedFromHybrid;
        }
        String extractedFromInlineHtml = tryExtractInlineHtmlDocument(sourceHtml);
        if (StringUtils.hasText(extractedFromInlineHtml)) {
            return extractedFromInlineHtml;
        }
        return sourceHtml;
    }

    private String tryExtractInlineHtmlDocument(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String lowered = value.toLowerCase();
        int doctypeIndex = lowered.indexOf("<!doctype html");
        if (doctypeIndex >= 0) {
            return value.substring(doctypeIndex).trim();
        }
        int htmlIndex = lowered.indexOf("<html");
        if (htmlIndex >= 0) {
            return value.substring(htmlIndex).trim();
        }
        return null;
    }

    private boolean looksLikeJsonPayload(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        char firstChar = value.charAt(0);
        return firstChar == '{' || firstChar == '[';
    }

    private String tryExtractFromHybridHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return null;
        }
        int cursor = html.indexOf('{');
        while (cursor >= 0) {
            String candidate = extractJsonCandidate(html, cursor);
            if (StringUtils.hasText(candidate) && containsLandingPageSignature(candidate)) {
                String extracted = tryExtractFromJsonPayload(candidate);
                if (StringUtils.hasText(extracted)) {
                    return extracted;
                }
            }
            cursor = html.indexOf('{', cursor + 1);
        }
        return null;
    }

    private boolean containsLandingPageSignature(String candidate) {
        String lowered = candidate.toLowerCase();
        return lowered.contains("landingpagehtml") || lowered.contains("htmldocument");
    }

    private String extractJsonCandidate(String source, int startIndex) {
        if (startIndex < 0 || startIndex >= source.length() || source.charAt(startIndex) != '{') {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;
        for (int i = startIndex; i < source.length(); i++) {
            char current = source.charAt(i);
            if (inString) {
                if (escaping) {
                    escaping = false;
                } else if (current == '\\') {
                    escaping = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
            } else if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(startIndex, i + 1);
                }
            }
        }
        return null;
    }

    private String tryExtractFromJsonPayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(payload.trim());
            String extracted = extractHtmlDocumentFromNode(root);
            if (StringUtils.hasText(extracted)) {
                return extracted;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String extractHtmlDocumentFromNode(JsonNode node) throws java.io.IOException {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            JsonNode landingPageHtmlNode = node.get("landingPageHtml");
            if (landingPageHtmlNode != null) {
                String extracted = extractFromLandingPageNode(landingPageHtmlNode);
                if (StringUtils.hasText(extracted)) {
                    return extracted;
                }
            }
            JsonNode htmlDocumentNode = node.get("htmlDocument");
            if (htmlDocumentNode != null) {
                String extracted = extractHtmlDocumentFromNode(htmlDocumentNode);
                if (StringUtils.hasText(extracted)) {
                    return extracted;
                }
            }
            Iterator<JsonNode> children = node.elements();
            while (children.hasNext()) {
                String extracted = extractHtmlDocumentFromNode(children.next());
                if (StringUtils.hasText(extracted)) {
                    return extracted;
                }
            }
            return null;
        }
        if (node.isTextual()) {
            String value = node.asText();
            if (!StringUtils.hasText(value)) {
                return null;
            }
            String trimmedValue = value.trim();
            if (looksLikeJsonPayload(trimmedValue)) {
                return tryExtractFromJsonPayload(trimmedValue);
            }
            if (trimmedValue.contains("<html") || trimmedValue.contains("<!doctype html")) {
                return value;
            }
            return null;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                String extracted = extractHtmlDocumentFromNode(child);
                if (StringUtils.hasText(extracted)) {
                    return extracted;
                }
            }
        }
        return null;
    }

    private String extractFromLandingPageNode(JsonNode landingPageHtmlNode) throws java.io.IOException {
        return extractHtmlDocumentFromNode(landingPageHtmlNode);
    }

    private String injectLandingAnalyticsScript(String slug, String htmlDocument) {
        if (!StringUtils.hasText(htmlDocument)) {
            return htmlDocument;
        }
        if (htmlDocument.toLowerCase(Locale.ROOT).contains("data-mh-landing-analytics")) {
            return htmlDocument;
        }
        String analyticsScript = """
                <script data-mh-landing-analytics="true">
                (function(){
                  const slugValue = %s;
                  const endpoint = '/api/public/lead-portal/flows/' + encodeURIComponent(slugValue) + '/page-analytics';
                  const sessionKey = 'mh_lp_session_' + slugValue;
                  const sessionId = sessionStorage.getItem(sessionKey) || (Date.now().toString(36) + '-' + Math.random().toString(36).slice(2));
                  sessionStorage.setItem(sessionKey, sessionId);
                  const sendEvent = function(eventType, sectionId, elapsedMs){
                    const payload = {
                      eventId: crypto.randomUUID ? crypto.randomUUID() : (Date.now().toString(36) + '-' + Math.random().toString(36).slice(2)),
                      eventType: eventType,
                      sessionId: sessionId,
                      sectionId: sectionId || null,
                      elapsedMs: typeof elapsedMs === 'number' ? Math.round(elapsedMs) : null,
                      visibleMs: typeof elapsedMs === 'number' ? Math.round(elapsedMs) : null,
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
                })();
                </script>
                """.formatted(toJsStringLiteral(slug));
        if (containsIgnoreCase(htmlDocument, "</body>")) {
            return htmlDocument.replaceFirst("(?i)</body>", java.util.regex.Matcher.quoteReplacement(analyticsScript + "\n</body>"));
        }
        return htmlDocument + "\n" + analyticsScript;
    }

    private String toJsStringLiteral(String rawValue) {
        String safeValue = rawValue == null ? "" : rawValue;
        return "\"" + safeValue
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\"";
    }

    private boolean containsIgnoreCase(String source, String token) {
        return source.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
    }
}
