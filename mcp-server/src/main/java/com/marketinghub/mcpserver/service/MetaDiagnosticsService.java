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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MetaDiagnosticsService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final McpProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MetaDiagnosticsService(McpProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public Map<String, Object> getDocumentationPage(String rawUrl) {
        ensureEnabled();
        if (!StringUtils.hasText(rawUrl)) {
            throw new IllegalArgumentException("url is required");
        }

        URI uri = URI.create(rawUrl.trim());
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("url must use https");
        }

        String host = uri.getHost();
        if (!isAllowedHost(host)) {
            throw new IllegalArgumentException("url host is not allowed: " + host);
        }

        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        String body = response.getBody() == null ? "" : response.getBody();
        String simplified = simplifyHtml(body);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", rawUrl);
        result.put("host", host);
        result.put("status", response.getStatusCode().value());
        result.put("contentLength", body.length());
        result.put("text", simplified);
        return result;
    }

    public Map<String, Object> graphGet(String path, Map<String, Object> query) {
        ensureEnabled();
        String normalizedPath = normalizePath(path);
        String token = properties.meta().accessToken();
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("meta access token is not configured (MCP_META_GRAPH_ACCESS_TOKEN)");
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(properties.meta().graphBaseUrl())
                .pathSegment(properties.meta().graphVersion())
                .pathSegment(normalizedPath.split("/"));

        if (query != null) {
            query.forEach((key, value) -> builder.queryParam(key, String.valueOf(value)));
        }
        builder.queryParam("access_token", token);

        URI uri = builder.build(true).toUri();
        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        Map<String, Object> payload = parseJsonBody(response.getBody());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", uri.toString().replace(token, "***"));
        result.put("path", normalizedPath);
        result.put("status", response.getStatusCode().value());
        result.put("response", payload);
        return result;
    }

    public Map<String, Object> debugToken(String inputToken) {
        ensureEnabled();
        if (!StringUtils.hasText(inputToken)) {
            throw new IllegalArgumentException("input_token is required");
        }

        String debugToken = properties.meta().debugAccessToken();
        if (!StringUtils.hasText(debugToken)) {
            throw new IllegalArgumentException("meta debug token is not configured (MCP_META_GRAPH_DEBUG_ACCESS_TOKEN)");
        }

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("input_token", inputToken);
        query.put("access_token", debugToken);

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(properties.meta().graphBaseUrl())
                .pathSegment(properties.meta().graphVersion(), "debug_token");
        query.forEach((key, value) -> builder.queryParam(key, value));

        URI uri = builder.build(true).toUri();
        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        Map<String, Object> payload = parseJsonBody(response.getBody());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", uri.toString().replace(debugToken, "***").replace(inputToken, "***"));
        result.put("status", response.getStatusCode().value());
        result.put("response", payload);
        return result;
    }

    public void ensureEnabled() {
        if (!properties.meta().enabled()) {
            throw new IllegalArgumentException("meta tools are disabled (set mcp.meta.enabled=true)");
        }
    }

    private boolean isAllowedHost(String host) {
        if (!StringUtils.hasText(host)) {
            return false;
        }
        List<String> allowed = properties.meta().docsAllowedHosts().stream()
                .map(item -> item.toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toList());
        return allowed.contains(host.toLowerCase(Locale.ROOT));
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("path is required");
        }
        String normalized = path.trim().replaceAll("^/+", "").replaceAll("/+$", "");
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("path is required");
        }
        return normalized;
    }

    private Map<String, Object> parseJsonBody(String body) {
        if (!StringUtils.hasText(body)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(body, MAP_TYPE);
        } catch (Exception ex) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("raw", body);
            return fallback;
        }
    }

    private String simplifyHtml(String html) {
        String withoutScripts = html
                .replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<style.*?>.*?</style>", " ");
        String noTags = withoutScripts.replaceAll("(?is)<[^>]+>", " ");
        String normalized = noTags.replaceAll("\\s+", " ").trim();
        if (normalized.length() > 30_000) {
            return normalized.substring(0, 30_000);
        }
        return normalized;
    }
}
