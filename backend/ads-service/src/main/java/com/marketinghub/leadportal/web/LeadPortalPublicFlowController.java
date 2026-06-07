package com.marketinghub.leadportal.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublicationRequest;
import com.marketinghub.leadportal.service.LeadPortalFlowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(LeadPortalPublicFlowController.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final LeadPortalFlowService flowService;

    /**
     * Creates the controller with the service used to resolve approved public flows.
     */
    public LeadPortalPublicFlowController(LeadPortalFlowService flowService) {
        this.flowService = flowService;
    }

    /**
     * Returns the approved public flow contract for the given slug.
     */
    @GetMapping("/{slug}")
    public LeadPortalFlowPublicationRequest getBySlug(@PathVariable String slug) {
        LeadPortalFlow flow = flowService.getApprovedBySlug(slug);
        return LeadPortalFlowPublicationRequest.from(flow);
    }

    /**
     * Returns the approved landing page HTML with runtime analytics instrumentation.
     */
    @GetMapping(value = "/{slug}/page", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getLandingPageBySlug(@PathVariable String slug) {
        LeadPortalFlow flow = flowService.getApprovedBySlug(slug);
        String htmlDocument = injectLandingAnalyticsScript(slug, extractHtmlDocument(flow.getCustomFormHtml()));
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", java.nio.charset.StandardCharsets.UTF_8))
                .body(htmlDocument);
    }

    /**
     * Resolves the publishable HTML document from raw, JSON or hybrid stored flow content.
     */
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

    /**
     * Extracts an inline HTML document that starts at a doctype or html tag.
     */
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

    /**
     * Indicates whether the stored value appears to be a JSON object or array payload.
     */
    private boolean looksLikeJsonPayload(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        char firstChar = value.charAt(0);
        return firstChar == '{' || firstChar == '[';
    }

    /**
     * Searches hybrid HTML/text content for an embedded landing JSON payload.
     */
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

    /**
     * Checks whether a JSON candidate contains fields that may hold landing HTML.
     */
    private boolean containsLandingPageSignature(String candidate) {
        String lowered = candidate.toLowerCase();
        return lowered.contains("landingpagehtml") || lowered.contains("htmldocument");
    }

    /**
     * Extracts a balanced JSON object candidate starting at the provided index.
     */
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

    /**
     * Attempts to extract the HTML document from a JSON payload stored in the flow.
     */
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
        } catch (Exception ex) {
            log.warn("module=lead-portal operation=extract-public-flow-html errorClass={} message={} payloadLength={}",
                    ex.getClass().getName(), ex.getMessage(), payload.length(), ex);
            return null;
        }
        return null;
    }

    /**
     * Recursively reads a JSON node tree to locate the first embedded HTML document.
     */
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

    /**
     * Delegates landingPageHtml node extraction through the generic HTML node resolver.
     */
    private String extractFromLandingPageNode(JsonNode landingPageHtmlNode) throws java.io.IOException {
        return extractHtmlDocumentFromNode(landingPageHtmlNode);
    }

    /**
     * Injects the public landing analytics script into the rendered HTML without altering the pure source artifact.
     */
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
                  var slugValue = %s;
                  var endpoint = '/api/public/lead-portal/flows/' + encodeURIComponent(slugValue) + '/page-analytics';
                  var memoryStore = {};
                  var safeGet = function(storageName, key){
                    try { return window[storageName] ? window[storageName].getItem(key) : null; } catch (e) { return null; }
                  };
                  var safeSet = function(storageName, key, value){
                    try { if (window[storageName]) window[storageName].setItem(key, value); } catch (e) {}
                  };
                  var cookieName = function(key){ return key.replace(/[^a-zA-Z0-9_\\-]/g, '_'); };
                  var readCookie = function(name){
                    try {
                      var parts = ('; ' + document.cookie).split('; ' + name + '=');
                      if (parts.length === 2) return decodeURIComponent(parts.pop().split(';').shift());
                    } catch (e) {}
                    return null;
                  };
                  var writeCookie = function(name, value){
                    try {
                      document.cookie = name + '=' + encodeURIComponent(value) + '; Max-Age=31536000; Path=/; SameSite=Lax';
                    } catch (e) {}
                  };
                  var randomId = function(prefix){
                    try {
                      if (window.crypto && typeof window.crypto.randomUUID === 'function') return prefix + '-' + window.crypto.randomUUID();
                      if (window.crypto && typeof window.crypto.getRandomValues === 'function') {
                        var bytes = new Uint8Array(16);
                        window.crypto.getRandomValues(bytes);
                        return prefix + '-' + Array.prototype.map.call(bytes, function(byte){ return byte.toString(16).padStart(2, '0'); }).join('');
                      }
                    } catch (e) {}
                    return prefix + '-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2);
                  };
                  var resolvePersistentId = function(key){
                    var cookieKey = cookieName(key);
                    var existing = safeGet('localStorage', key) || readCookie(cookieKey) || memoryStore[key];
                    if (existing) return existing;
                    var created = randomId('visitor');
                    memoryStore[key] = created;
                    safeSet('localStorage', key, created);
                    writeCookie(cookieKey, created);
                    return created;
                  };
                  var resolveSessionId = function(key){
                    var existing = safeGet('sessionStorage', key) || memoryStore[key];
                    if (existing) return existing;
                    var created = randomId('session');
                    memoryStore[key] = created;
                    safeSet('sessionStorage', key, created);
                    return created;
                  };
                  var visitorId = resolvePersistentId('mh_lp_visitor_' + slugValue);
                  var sessionId = resolveSessionId('mh_lp_session_' + slugValue);
                  var resolveDeviceType = function(){
                    var userAgent = navigator.userAgent || '';
                    var isTablet = /ipad|tablet/i.test(userAgent) || (/android/i.test(userAgent) && !/mobile/i.test(userAgent));
                    if (isTablet) return 'tablet';
                    var isMobile = /mobi|iphone|ipod|android/i.test(userAgent);
                    return isMobile ? 'mobile' : 'desktop';
                  };
                  var nowMs = function(){ return window.performance && typeof window.performance.now === 'function' ? window.performance.now() : Date.now(); };
                  var deviceType = resolveDeviceType();
                  var sendEvent = function(eventType, sectionId, elapsedMs){
                    var payload = {
                      eventId: randomId('event'),
                      eventType: eventType,
                      visitorId: visitorId,
                      sessionId: sessionId,
                      sectionId: sectionId || null,
                      elapsedMs: typeof elapsedMs === 'number' ? Math.round(elapsedMs) : null,
                      visibleMs: typeof elapsedMs === 'number' ? Math.round(elapsedMs) : null,
                      pageUrl: window.location.href,
                      occurredAt: new Date().toISOString(),
                      userAgent: navigator.userAgent || '',
                      deviceType: deviceType
                    };
                    if (navigator.sendBeacon && window.Blob) {
                      try {
                        navigator.sendBeacon(endpoint, new Blob([JSON.stringify(payload)], {type: 'application/json'}));
                        return;
                      } catch (e) {}
                    }
                    if (window.fetch) fetch(endpoint, {method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload), keepalive: true}).catch(function(){});
                  };
                  sendEvent('page_view', null, null);
                  if (!('IntersectionObserver' in window) || !window.Map) return;
                  var visibleSince = new Map();
                  var observer = new IntersectionObserver(function(entries){
                    var now = nowMs();
                    entries.forEach(function(entry){
                      var sectionId = entry.target.id || entry.target.getAttribute('data-section-id') || entry.target.getAttribute('data-track-section');
                      if (!sectionId) return;
                      if (entry.isIntersecting && entry.intersectionRatio >= 0.5) {
                        if (!visibleSince.has(sectionId)) visibleSince.set(sectionId, now);
                      } else if (visibleSince.has(sectionId)) {
                        var startedAt = visibleSince.get(sectionId);
                        visibleSince.delete(sectionId);
                        sendEvent('section_view_time', sectionId, now - startedAt);
                      }
                    });
                  }, {threshold:[0.5]});
                  document.querySelectorAll('section[id], [data-section-id], [data-track-section]').forEach(function(el){ observer.observe(el); });
                  window.addEventListener('beforeunload', function(){
                    var now = nowMs();
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

    /**
     * Escapes a Java value for safe insertion as a JavaScript string literal.
     */
    private String toJsStringLiteral(String rawValue) {
        String safeValue = rawValue == null ? "" : rawValue;
        return "\"" + safeValue
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\"";
    }

    /**
     * Checks whether a source string contains a token without case sensitivity.
     */
    private boolean containsIgnoreCase(String source, String token) {
        return source.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
    }
}
