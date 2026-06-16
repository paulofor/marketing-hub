package com.marketinghub.mcpserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mcpserver.config.McpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(MetaDiagnosticsService.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final String ACTIVE_FACEBOOK_ACCESS_TOKEN_SQL = """
            SELECT access_token
            FROM fb_account
            WHERE worker_enabled = 1
              AND access_token IS NOT NULL
              AND TRIM(access_token) <> ''
            ORDER BY id
            LIMIT 1
            """;

    private final McpProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Inicializa o serviço usando um cliente HTTP padrão para documentação e Graph API.
     */
    public MetaDiagnosticsService(McpProperties properties, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
        this(properties, objectMapper, jdbcTemplate, new RestTemplate());
    }

    /**
     * Inicializa o serviço com cliente HTTP injetado para permitir testes isolados.
     */
    MetaDiagnosticsService(McpProperties properties, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate,
                           RestTemplate restTemplate) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplate;
    }

    /**
     * Busca uma página pública da documentação Meta em hosts permitidos.
     */
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

    /**
     * Executa uma chamada GET na Graph API usando o token ativo no banco como fonte primária.
     */
    public Map<String, Object> graphGet(String path, Map<String, Object> query) {
        ensureEnabled();
        String normalizedPath = normalizePath(path);
        String token = resolveGraphAccessToken();

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

    /**
     * Executa debug_token na Graph API com o token de debug configurado.
     */
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

    /**
     * Valida se as ferramentas Meta estão habilitadas.
     */
    public void ensureEnabled() {
        if (!properties.meta().enabled()) {
            throw new IllegalArgumentException("meta tools are disabled (set mcp.meta.enabled=true)");
        }
    }

    /**
     * Recupera o token de acesso da mesma conta habilitada para o Facebook Ads Worker.
     */
    private String resolveGraphAccessToken() {
        String databaseToken = fetchDatabaseWorkerAccessToken();
        if (StringUtils.hasText(databaseToken)) {
            return databaseToken.trim();
        }
        String configuredToken = properties.meta().accessToken();
        if (StringUtils.hasText(configuredToken)) {
            return configuredToken.trim();
        }
        throw new IllegalArgumentException(
                "meta access token is not configured in fb_account or MCP_META_GRAPH_ACCESS_TOKEN"
        );
    }

    /**
     * Consulta diretamente o banco para reutilizar o token ativo da conta habilitada para o worker Facebook Ads.
     */
    private String fetchDatabaseWorkerAccessToken() {
        try {
            List<String> tokens = jdbcTemplate.queryForList(ACTIVE_FACEBOOK_ACCESS_TOKEN_SQL, String.class);
            if (tokens.isEmpty()) {
                LOGGER.warn("Nenhum token Meta ativo encontrado no banco para meta_graph_get: tabela=fb_account");
                return null;
            }
            return tokens.getFirst();
        } catch (DataAccessException ex) {
            LOGGER.warn("Falha ao buscar token Meta no banco para meta_graph_get: tabela=fb_account", ex);
            return null;
        }
    }

    /**
     * Verifica se o host de documentação pertence à lista permitida.
     */
    private boolean isAllowedHost(String host) {
        if (!StringUtils.hasText(host)) {
            return false;
        }
        List<String> allowed = properties.meta().docsAllowedHosts().stream()
                .map(item -> item.toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toList());
        return allowed.contains(host.toLowerCase(Locale.ROOT));
    }

    /**
     * Normaliza o path solicitado para a Graph API.
     */
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

    /**
     * Converte uma resposta JSON em mapa simples.
     */
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

    /**
     * Simplifica o HTML da documentação para texto curto.
     */
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
