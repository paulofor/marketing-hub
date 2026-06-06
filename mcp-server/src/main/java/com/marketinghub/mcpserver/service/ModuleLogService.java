package com.marketinghub.mcpserver.service;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Lê e filtra logs operacionais dos módulos Java expostos ao MCP.
 */
@Service
public class ModuleLogService {
    private static final Logger logger = LoggerFactory.getLogger(ModuleLogService.class);
    private static final int DEFAULT_LINES = 200;
    private final McpProperties properties;
    private final HttpClient httpClient;
    private final Duration logFetchTimeout;
    private final long httpTailRangeBytes;
    private final int fetchAttempts;
    private final int fetchRetryDelayMillis;

    /**
     * Inicializa o leitor de logs com timeouts e limites definidos em configuração.
     */
    public ModuleLogService(McpProperties properties) {
        this.properties = properties;
        this.logFetchTimeout = Duration.ofSeconds(properties.logs().fetchTimeoutSeconds());
        this.httpTailRangeBytes = properties.logs().httpTailRangeBytes();
        this.fetchAttempts = properties.logs().fetchAttempts();
        this.fetchRetryDelayMillis = properties.logs().fetchRetryDelayMillis();
        this.httpClient = HttpClient.newBuilder().connectTimeout(logFetchTimeout).build();
    }

    /**
     * Retorna o limite máximo de linhas permitido por chamada do tool de logs.
     */
    public int maxLines() { return properties.logs().maxLines(); }

    /**
     * Lê os logs do módulo solicitado aplicando filtros de texto, período e paginação.
     */
    public Map<String, Object> readModuleLogs(String module, Integer requestedLines, String contains, String from, String to, Integer offset, String cursor) {
        String normalizedModule = normalizeModule(module);
        int lines = sanitizeLines(requestedLines);
        String configuredPath = modulePath(normalizedModule);
        if (!StringUtils.hasText(configuredPath)) throw new IllegalArgumentException("No log path configured for module: " + normalizedModule);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("module", normalizedModule);
        if (isHttpUrl(configuredPath)) return readLogsFromUrl(configuredPath, lines, contains, from, to, offset, cursor, response);

        Path path = Path.of(configuredPath);
        response.put("path", path.toString());
        if (!Files.exists(path)) {
            response.put("exists", false);response.put("requestedLines", lines);response.put("returnedLines", 0);response.put("lines", List.of());response.put("nextCursor", "");
            return response;
        }
        try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
            return buildFilteredResponse(stream.toList(), lines, contains, from, to, offset, cursor, response, Files.size(path));
        } catch (IOException ex) {
            logger.error("mcp-server readModuleLogs failed to read local log file for module={} path={}", normalizedModule, path, ex);
            throw new IllegalArgumentException("Failed to read log file: " + ex.getMessage());
        }
    }

    /**
     * Busca logs em uma URL HTTP/HTTPS e monta a resposta filtrada.
     */
    private Map<String, Object> readLogsFromUrl(String configuredUrl, int lines, String contains, String from, String to, Integer offset, String cursor, Map<String, Object> response) {
        response.put("path", configuredUrl); response.put("source", "http");
        HttpRequest request = buildTailRequest(configuredUrl, true);
        List<String> errors = new ArrayList<>();
        try {
            HttpResponse<String> httpResponse = sendWithRetry(request, errors);
            response.put("httpStatus", httpResponse.statusCode());
            if (httpResponse.statusCode() == 416) {
                HttpResponse<String> fullResponse = sendWithRetry(buildTailRequest(configuredUrl, false), errors);
                response.put("httpStatus", fullResponse.statusCode());
                httpResponse = fullResponse;
            }
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) throw new IllegalArgumentException("Failed to read log stream URL: HTTP " + httpResponse.statusCode() + " | attempts: " + String.join(" | ", errors));
            return buildFilteredResponse(httpResponse.body().lines().toList(), lines, contains, from, to, offset, cursor, response, null);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            logger.error("mcp-server readLogsFromUrl failed to read log stream url={} attempts={}", configuredUrl, errors, ex);
            throw new IllegalArgumentException("Failed to read log stream URL: " + ex.getMessage());
        }
    }

    /**
     * Constrói a resposta final após aplicar filtros e resolver a janela de paginação.
     */
    private Map<String, Object> buildFilteredResponse(List<String> allLines, int lines, String contains, String from, String to, Integer offset, String cursor, Map<String, Object> response, Long sizeBytes) {
        List<String> filtered = applyFilters(allLines, contains, from, to);
        boolean defaultTailMode = !StringUtils.hasText(contains) && !StringUtils.hasText(from) && !StringUtils.hasText(to) && offset == null && !StringUtils.hasText(cursor);
        int resolvedOffset = defaultTailMode ? Math.max(0, filtered.size() - lines) : resolveOffset(offset, cursor);
        if (resolvedOffset < 0 || resolvedOffset > filtered.size()) throw new IllegalArgumentException("offset must be between 0 and " + filtered.size());
        int end = Math.min(filtered.size(), resolvedOffset + lines);
        List<String> page = filtered.subList(resolvedOffset, end);
        response.put("exists", true);
        if (sizeBytes != null) response.put("sizeBytes", sizeBytes);
        response.put("requestedLines", lines); response.put("returnedLines", page.size()); response.put("totalFilteredLines", filtered.size()); response.put("offset", resolvedOffset);
        response.put("contains", contains == null ? "" : contains); response.put("from", from == null ? "" : from); response.put("to", to == null ? "" : to);
        response.put("lines", page);
        response.put("nextCursor", end < filtered.size() ? Base64.getEncoder().encodeToString(("offset:" + end).getBytes(StandardCharsets.UTF_8)) : "");
        return response;
    }

    /**
     * Aplica filtros opcionais por texto literal e intervalo ISO-8601.
     */
    private List<String> applyFilters(List<String> lines, String contains, String from, String to) {
        Instant fromTs = parseInstant(from, "from"); Instant toTs = parseInstant(to, "to");
        return lines.stream().filter(line -> {
            if (StringUtils.hasText(contains) && !line.contains(contains)) return false;
            if (fromTs == null && toTs == null) return true;
            Instant lineTs = extractFirstInstant(line);
            if (lineTs == null) return false;
            if (fromTs != null && lineTs.isBefore(fromTs)) return false;
            return toTs == null || !lineTs.isAfter(toTs);
        }).toList();
    }

    /**
     * Resolve o deslocamento de paginação a partir de offset explícito ou cursor.
     */
    private int resolveOffset(Integer offset, String cursor) {
        if (StringUtils.hasText(cursor)) {
            try {
                String decoded = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
                if (!decoded.startsWith("offset:")) throw new IllegalArgumentException("invalid cursor");
                return Integer.parseInt(decoded.substring(7));
            } catch (Exception ex) {
                logger.error("mcp-server resolveOffset failed to decode pagination cursor={}", cursor, ex);
                throw new IllegalArgumentException("invalid cursor");
            }
        }
        return offset == null ? 0 : offset;
    }

    /**
     * Converte uma string ISO-8601 para Instant validando o nome do campo informado.
     */
    private Instant parseInstant(String value, String fieldName) {
        if (!StringUtils.hasText(value)) return null;
        try { return Instant.parse(value); } catch (DateTimeParseException ex) {
            logger.error("mcp-server parseInstant failed for field={} value={}", fieldName, value, ex);
            throw new IllegalArgumentException(fieldName + " must be in ISO-8601 UTC format, e.g. 2026-05-21T04:35:00Z");
        }
    }

    /**
     * Extrai o primeiro timestamp ISO-8601 encontrado em uma linha de log.
     */
    private Instant extractFirstInstant(String line) {
        for (String token : line.split("[\\s\\[\\]]+")) {
            try { return Instant.parse(token); } catch (DateTimeParseException ignored) { }
        }
        return null;
    }

    /**
     * Executa a requisição HTTP de logs com retentativas configuradas.
     */
    private HttpResponse<String> sendWithRetry(HttpRequest request, List<String> errors)
            throws IOException, InterruptedException {
        IOException lastIo = null;
        InterruptedException lastInterrupt = null;
        for (int attempt = 1; attempt <= fetchAttempts; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                errors.add("attempt " + attempt + " status " + response.statusCode());
                if ((response.statusCode() >= 500 || response.statusCode() == 429) && attempt < fetchAttempts) {
                    Thread.sleep(fetchRetryDelayMillis);
                    continue;
                }
                return response;
            } catch (IOException ex) {
                lastIo = ex;
                errors.add("attempt " + attempt + " io-error: " + ex.getClass().getSimpleName() + " - "
                        + ex.getMessage());
                logger.error("mcp-server sendWithRetry failed attempt={} uri={}", attempt, request.uri(), ex);
            } catch (InterruptedException ex) {
                lastInterrupt = ex;
                errors.add("attempt " + attempt + " interrupted: " + ex.getMessage());
                logger.error("mcp-server sendWithRetry interrupted attempt={} uri={}", attempt, request.uri(), ex);
                break;
            }
            if (attempt < fetchAttempts) {
                Thread.sleep(fetchRetryDelayMillis);
            }
        }
        if (lastInterrupt != null) {
            throw lastInterrupt;
        }
        if (lastIo != null) {
            throw lastIo;
        }
        throw new IOException("Failed to read log stream URL after retries");
    }

    /**
     * Monta a requisição HTTP para leitura completa ou parcial do arquivo de logs.
     */
    private HttpRequest buildTailRequest(String configuredUrl, boolean withRange) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(configuredUrl)).timeout(logFetchTimeout).GET();
        if (withRange) {
            builder.header("Range", "bytes=-" + httpTailRangeBytes);
        }
        return builder.build();
    }

    /**
     * Indica se o caminho configurado é uma URL HTTP/HTTPS.
     */
    private boolean isHttpUrl(String value) {
        String normalized = value.trim().toLowerCase();
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    /**
     * Resolve o caminho de logs configurado para o módulo normalizado.
     */
    private String modulePath(String module) {
        return switch (module) {
            case "backend" -> properties.logs().backendPath();
            case "ai-worker" -> properties.logs().aiWorkerPath();
            case "lead-portal" -> properties.logs().leadPortalPath();
            case "facebook-ads" -> properties.logs().facebookAdsPath();
            case "email-service" -> properties.logs().emailServicePath();
            case "lead-portal-payment" -> properties.logs().leadPortalPaymentPath();
            case "mds" -> properties.logs().mdsPath();
            case "mois" -> properties.logs().moisPath();
            case "mois-sales-library-worker" -> properties.logs().moisSalesLibraryWorkerPath();
            case "mois-hotmart" -> properties.logs().moisHotmartPath();
            case "clickbank-coletor-mois" -> properties.logs().clickbankColetorMoisPath();
            case "oprm-coletor-receita" -> properties.logs().oprmColetorReceitaPath();
            default -> throw new IllegalArgumentException("Unknown module: " + module);
        };
    }

    /**
     * Normaliza e valida o identificador do módulo solicitado pelo tool MCP.
     */
    private String normalizeModule(String module) {
        if (!StringUtils.hasText(module)) {
            throw new IllegalArgumentException("module is required");
        }
        String normalized = module.trim().toLowerCase();
        return switch (normalized) {
            case "backend", "ai-worker", "lead-portal", "facebook-ads", "email-service", "lead-portal-payment",
                    "mds", "mois", "mois-sales-library-worker", "mois-hotmart", "clickbank-coletor-mois",
                    "oprm-coletor-receita" -> normalized;
            default -> throw new IllegalArgumentException("module must be one of: backend, ai-worker, lead-portal, "
                    + "facebook-ads, email-service, lead-portal-payment, mds, mois, mois-sales-library-worker, "
                    + "mois-hotmart, clickbank-coletor-mois, oprm-coletor-receita");
        };
    }

    /**
     * Valida a quantidade de linhas solicitada contra os limites configurados.
     */
    private int sanitizeLines(Integer requestedLines) {
        int lines = requestedLines == null ? DEFAULT_LINES : requestedLines;
        if (lines < 1 || lines > maxLines()) {
            throw new IllegalArgumentException("lines must be between 1 and " + maxLines());
        }
        return lines;
    }
}
