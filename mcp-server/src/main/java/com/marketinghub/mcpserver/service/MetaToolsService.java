package com.marketinghub.mcpserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mcpserver.config.McpProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class MetaToolsService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final int DEFAULT_MAX_RESPONSE_CHARS = 12_000;

    private final McpProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MetaToolsService(McpProperties properties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getDocumentationPage(String url) {
        ensureMetaEnabled();
        URI uri = URI.create(requiredText(url, "url"));
        validateAllowedHost(uri);

        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        String body = Objects.requireNonNullElse(response.getBody(), "");
        String excerpt = toPlainText(body);

        int maxChars = DEFAULT_MAX_RESPONSE_CHARS;
        boolean truncated = excerpt.length() > maxChars;
        String content = truncated ? excerpt.substring(0, maxChars) : excerpt;

        return Map.of(
                "url", uri.toString(),
                "host", Objects.requireNonNullElse(uri.getHost(), ""),
                "excerpt", content,
                "truncated", truncated,
                "excerptLength", content.length()
        );
    }

    public Map<String, Object> graphGet(String path, Map<String, Object> queryParams) {
        ensureMetaEnabled();
        String normalizedPath = normalizePath(path);

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(properties.meta().graphBaseUrl())
                .pathSegment(properties.meta().graphVersion())
                .pathSegment(normalizedPath.split("/"));

        if (queryParams != null) {
            queryParams.forEach((key, value) -> {
                if (value != null && !"access_token".equalsIgnoreCase(key)) {
                    builder.queryParam(key, value);
                }
            });
        }

        builder.queryParam("access_token", requiredText(properties.meta().accessToken(), "mcp.meta.access-token"));
        URI uri = builder.build(true).toUri();

        ResponseEntity<Object> response = restTemplate.getForEntity(uri, Object.class);
        Map<String, Object> payload = objectMapper.convertValue(response.getBody(), MAP_TYPE);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("graphUrl", sanitizeAccessToken(uri.toString()));
        result.put("statusCode", response.getStatusCode().value());
        result.put("payload", payload);
        return result;
    }

    public Map<String, Object> debugToken(String inputToken) {
        ensureMetaEnabled();
        String debugAccessToken = requiredText(
                properties.meta().debugAccessToken(),
                "mcp.meta.debug-access-token"
        );

        URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.meta().graphBaseUrl())
                .pathSegment(properties.meta().graphVersion(), "debug_token")
                .queryParam("input_token", requiredText(inputToken, "input_token"))
                .queryParam("access_token", debugAccessToken)
                .build(true)
                .toUri();

        ResponseEntity<Object> response = restTemplate.getForEntity(uri, Object.class);
        Map<String, Object> payload = objectMapper.convertValue(response.getBody(), MAP_TYPE);

        return Map.of(
                "graphUrl", sanitizeAccessToken(uri.toString()),
                "statusCode", response.getStatusCode().value(),
                "payload", payload
        );
    }

    private void ensureMetaEnabled() {
        if (!properties.meta().enabled()) {
            throw new IllegalArgumentException("meta tools are disabled (set mcp.meta.enabled=true)");
        }
    }

    private void validateAllowedHost(URI uri) {
        String host = Objects.requireNonNullElse(uri.getHost(), "").toLowerCase(Locale.ROOT);
        boolean allowed = properties.meta().docsAllowedHosts().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(allowedHost -> host.equals(allowedHost) || host.endsWith("." + allowedHost));
        if (!allowed) {
            throw new IllegalArgumentException("host not allowed for meta_docs_get: " + host);
        }
    }

    private String normalizePath(String path) {
        String raw = requiredText(path, "path");
        String normalized = raw.startsWith("/") ? raw.substring(1) : raw;
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("path must not be blank");
        }
        if (normalized.contains("?")) {
            throw new IllegalArgumentException("path must not contain query string");
        }
        return normalized;
    }

    private String requiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private String sanitizeAccessToken(String url) {
        return url.replaceAll("access_token=[^&]+", "access_token=***");
    }

    private String toPlainText(String html) {
        String noScript = html.replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<style.*?>.*?</style>", " ");
        String withoutTags = noScript.replaceAll("(?is)<[^>]+>", " ");
        return withoutTags.replaceAll("\\s+", " ").trim();
    }
}
