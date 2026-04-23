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
        String htmlDocument = extractHtmlDocument(flow.getCustomFormHtml());
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
        return sourceHtml;
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
            if (root.isObject()) {
                JsonNode landingPageHtmlNode = root.get("landingPageHtml");
                if (landingPageHtmlNode != null) {
                    String extracted = extractFromLandingPageNode(landingPageHtmlNode);
                    if (StringUtils.hasText(extracted)) {
                        return extracted;
                    }
                }
                JsonNode htmlDocumentNode = root.get("htmlDocument");
                if (htmlDocumentNode != null && htmlDocumentNode.isTextual()) {
                    return htmlDocumentNode.asText();
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String extractFromLandingPageNode(JsonNode landingPageHtmlNode) throws java.io.IOException {
        if (landingPageHtmlNode.isTextual()) {
            JsonNode nested = OBJECT_MAPPER.readTree(landingPageHtmlNode.asText());
            JsonNode htmlDocumentNode = nested.get("htmlDocument");
            return htmlDocumentNode != null && htmlDocumentNode.isTextual() ? htmlDocumentNode.asText() : null;
        }
        if (landingPageHtmlNode.isObject()) {
            JsonNode htmlDocumentNode = landingPageHtmlNode.get("htmlDocument");
            return htmlDocumentNode != null && htmlDocumentNode.isTextual() ? htmlDocumentNode.asText() : null;
        }
        return null;
    }
}
