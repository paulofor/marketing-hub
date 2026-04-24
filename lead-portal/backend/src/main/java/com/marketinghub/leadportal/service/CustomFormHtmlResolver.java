package com.marketinghub.leadportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Normalizes and validates the custom HTML payload stored in flows.
 */
@Component
public class CustomFormHtmlResolver {

    private static final Pattern HTML_HINT = Pattern.compile("(?is)^\\s*(?:<!doctype|<html|<body|<main|<section|<div|<header|<footer)");
    private static final Pattern WRAPPED_JSON_IN_BODY = Pattern.compile("(?is)<body[^>]*>\\s*(\\{[\\s\\S]*?})\\s*</body>");
    private static final Pattern HTML_DOCUMENT_FIELD = Pattern.compile("(?is)\"htmlDocument\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Accepts only pure HTML payloads (no mixed JSON wrappers).
     */
    public String normalize(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        String trimmed = rawValue.trim();
        if (looksLikeHtml(trimmed)) {
            return rescueLegacyWrappedPayload(trimmed).orElse(trimmed);
        }
        throw new IllegalArgumentException(
                "customFormHtml deve ser HTML puro. Payload JSON/misto não é mais aceito.");
    }

    private boolean looksLikeHtml(String value) {
        return HTML_HINT.matcher(value).find();
    }

    private Optional<String> rescueLegacyWrappedPayload(String htmlDocument) {
        String lowered = htmlDocument.toLowerCase();
        if (!lowered.contains("landingpagehtml") || !lowered.contains("htmldocument")) {
            return Optional.empty();
        }

        String jsonCandidate = extractJsonCandidate(htmlDocument);
        if (!StringUtils.hasText(jsonCandidate)) {
            return Optional.empty();
        }

        try {
            var rawHtmlDocumentMatcher = HTML_DOCUMENT_FIELD.matcher(jsonCandidate);
            if (rawHtmlDocumentMatcher.find()) {
                String decodedRaw = rawHtmlDocumentMatcher.group(1)
                        .replace("\\\\", "\\")
                        .replace("\\n", "\n")
                        .replace("\\t", "\t")
                        .replace("\\r", "\r")
                        .replace("\\/", "/");
                String normalizedFromField = unescapeLegacyArtifacts(decodedRaw).trim();
                if (looksLikeHtml(normalizedFromField)) {
                    return Optional.of(normalizedFromField);
                }
            }

            JsonNode root = objectMapper.readTree(unescapeLegacyArtifacts(jsonCandidate));
            JsonNode htmlNode = root.path("landingPageHtml").path("htmlDocument");
            String normalized = unescapeLegacyArtifacts(htmlNode.asText()).trim();
            if (!looksLikeHtml(normalized)) {
                return Optional.empty();
            }
            return Optional.of(normalized);
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private String extractJsonCandidate(String htmlDocument) {
        var matcher = WRAPPED_JSON_IN_BODY.matcher(htmlDocument);
        if (matcher.find()) {
            return matcher.group(1);
        }
        int marker = htmlDocument.indexOf("\"landingPageHtml\"");
        if (marker < 0) {
            marker = htmlDocument.indexOf("landingPageHtml");
        }
        if (marker < 0) {
            return htmlDocument;
        }
        int firstBrace = htmlDocument.lastIndexOf('{', marker);
        if (firstBrace < 0) {
            return htmlDocument;
        }
        int depth = 0;
        for (int i = firstBrace; i < htmlDocument.length(); i++) {
            char ch = htmlDocument.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return htmlDocument.substring(firstBrace, i + 1);
                }
            }
        }
        return htmlDocument;
    }

    private String unescapeLegacyArtifacts(String value) {
        return value
                .replace("\\&quot;", "\"")
                .replace("&quot;", "\"")
                .replace("\\&apos;", "'")
                .replace("&apos;", "'")
                .replace("\\&#39;", "'")
                .replace("&#39;", "'")
                .replace("\\&amp;", "&")
                .replace("&amp;", "&")
                .replace("\\>", ">")
                .replace("\\<", "<")
                .replace("\\/", "/")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r")
                .replace("\u00a0", " ")
                .replace("\u201c", "\"")
                .replace("\u201d", "\"")
                .replace("\u2018", "'")
                .replace("\u2019", "'")
                .trim();
    }

}
