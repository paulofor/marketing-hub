package com.marketinghub.leadportal.service;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Normalizes and validates the custom HTML payload stored in flows.
 */
@Component
public class CustomFormHtmlResolver {

    private static final Pattern HTML_HINT = Pattern.compile("(?is)^\\s*(?:<!doctype|<html|<body|<main|<section|<div|<header|<footer)");

    /**
     * Accepts only pure HTML payloads (no mixed JSON wrappers).
     */
    public String normalize(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        String trimmed = rawValue.trim();
        if (looksLikeHtml(trimmed)) {
            return trimmed;
        }
        throw new IllegalArgumentException(
                "customFormHtml deve ser HTML puro. Payload JSON/misto não é mais aceito.");
    }

    private boolean looksLikeHtml(String value) {
        return HTML_HINT.matcher(value).find();
    }

}
