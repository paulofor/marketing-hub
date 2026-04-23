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
        String lowered = trimmedSource.toLowerCase();
        if (lowered.startsWith("<html") || lowered.startsWith("<!doctype")) {
            return sourceHtml;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(trimmedSource);
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
            return sourceHtml;
        }
        return sourceHtml;
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
