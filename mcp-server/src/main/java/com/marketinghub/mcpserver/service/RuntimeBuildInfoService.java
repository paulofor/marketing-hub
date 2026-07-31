package com.marketinghub.mcpserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mcpserver.config.McpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consulta a identidade de build publicada por módulos Java permitidos em runtime.
 */
@Service
public class RuntimeBuildInfoService {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeBuildInfoService.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final McpProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Inicializa o cliente HTTP com timeout definido para consultas de build info.
     */
    public RuntimeBuildInfoService(McpProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.buildInfo().timeoutSeconds()))
                .build();
    }

    /**
     * Retorna os módulos liberados para consulta de identidade de build.
     */
    public List<String> allowedModules() {
        return properties.buildInfo().allowedModules();
    }

    /**
     * Consulta o endpoint configurado do módulo e resume campos de versão, commit e build.
     */
    public Map<String, Object> readBuildInfo(String module) {
        if (!properties.buildInfo().enabled()) {
            throw new IllegalArgumentException("runtime build info is disabled (set mcp.build-info.enabled=true)");
        }

        String normalizedModule = normalizeModule(module);
        String infoUrl = properties.buildInfo().moduleInfoUrls().get(normalizedModule);
        if (!StringUtils.hasText(infoUrl)) {
            throw new IllegalArgumentException("No build info URL configured for module: " + normalizedModule);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(infoUrl))
                .timeout(Duration.ofSeconds(properties.buildInfo().timeoutSeconds()))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return buildResponse(normalizedModule, infoUrl, response.statusCode(), response.body());
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("mcp-server readBuildInfo failed module={} url={}", normalizedModule, infoUrl, ex);
            throw new IllegalArgumentException("Failed to read runtime build info: " + ex.getMessage());
        }
    }

    /**
     * Monta a resposta estruturada indicando se o módulo publicou identidade rastreável.
     */
    private Map<String, Object> buildResponse(String module, String infoUrl, int httpStatus, String body) {
        Map<String, Object> payload = parsePayload(body);
        Map<String, Object> summary = extractSummary(payload);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("module", module);
        result.put("infoUrl", infoUrl);
        result.put("httpStatus", httpStatus);
        result.put("queriedAt", Instant.now().toString());
        result.put("buildIdentityPublished", hasBuildIdentity(summary));
        result.put("summary", summary);
        result.put("payload", payload);
        if (!hasBuildIdentity(summary)) {
            result.put("diagnostic",
                    "O endpoint respondeu, mas não publicou commit, branch, build time ou version rastreável.");
        }
        return result;
    }

    /**
     * Extrai campos conhecidos de build info de formatos comuns do Spring Boot Actuator.
     */
    private Map<String, Object> extractSummary(Map<String, Object> payload) {
        Map<String, Object> summary = new LinkedHashMap<>();
        putIfPresent(summary, "version", firstValue(payload, "build.version", "version", "app.version"));
        putIfPresent(summary, "buildTime", firstValue(payload, "build.time", "time", "buildTime"));
        putIfPresent(summary, "commitId", firstValue(payload, "git.commit.id", "git.commit.id.full", "commit", "commitId"));
        putIfPresent(summary, "commitAbbrev", firstValue(payload, "git.commit.id.abbrev", "commitAbbrev"));
        putIfPresent(summary, "branch", firstValue(payload, "git.branch", "branch"));
        return summary;
    }

    /**
     * Indica se ao menos um campo rastreável de build foi publicado pelo runtime.
     */
    private boolean hasBuildIdentity(Map<String, Object> summary) {
        return !summary.isEmpty();
    }

    /**
     * Adiciona um campo ao resumo somente quando ele possui texto útil.
     */
    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            target.put(key, value);
        }
    }

    /**
     * Busca o primeiro valor existente considerando chaves aninhadas por ponto.
     */
    private Object firstValue(Map<String, Object> payload, String... paths) {
        for (String path : paths) {
            Object value = valueAtPath(payload, path);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return value;
            }
        }
        return null;
    }

    /**
     * Resolve um valor em mapa aninhado usando caminho separado por ponto.
     */
    @SuppressWarnings("unchecked")
    private Object valueAtPath(Map<String, Object> payload, String path) {
        if (payload.containsKey(path)) {
            return payload.get(path);
        }
        Object current = payload;
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            Map<String, Object> currentMap = (Map<String, Object>) map;
            String remainingPath = String.join(".", java.util.Arrays.copyOfRange(parts, i, parts.length));
            if (currentMap.containsKey(remainingPath)) {
                return currentMap.get(remainingPath);
            }
            current = currentMap.get(parts[i]);
        }
        return current;
    }

    /**
     * Converte o corpo JSON do actuator em mapa estruturado.
     */
    private Map<String, Object> parsePayload(String body) {
        if (!StringUtils.hasText(body)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(body, MAP_TYPE);
        } catch (IOException ex) {
            logger.error("mcp-server parsePayload failed to parse build info body", ex);
            throw new IllegalArgumentException("Runtime build info response must be a JSON object");
        }
    }

    /**
     * Normaliza e valida o módulo solicitado contra a allowlist configurada.
     */
    private String normalizeModule(String module) {
        if (!StringUtils.hasText(module)) {
            throw new IllegalArgumentException("module is required");
        }
        String normalized = module.trim().toLowerCase();
        if (!properties.buildInfo().allowedModules().contains(normalized)) {
            throw new IllegalArgumentException("module must be one of: "
                    + String.join(", ", properties.buildInfo().allowedModules()));
        }
        return normalized;
    }
}
