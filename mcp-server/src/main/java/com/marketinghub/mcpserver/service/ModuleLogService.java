package com.marketinghub.mcpserver.service;

import com.marketinghub.mcpserver.config.McpProperties;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class ModuleLogService {

    private static final int DEFAULT_LINES = 200;

    private final McpProperties properties;
    private final HttpClient httpClient;
    private final Duration logFetchTimeout;
    private final long httpTailRangeBytes;
    private final int fetchAttempts;
    private final int fetchRetryDelayMillis;

    public ModuleLogService(McpProperties properties) {
        this.properties = properties;
        this.logFetchTimeout = Duration.ofSeconds(properties.logs().fetchTimeoutSeconds());
        this.httpTailRangeBytes = properties.logs().httpTailRangeBytes();
        this.fetchAttempts = properties.logs().fetchAttempts();
        this.fetchRetryDelayMillis = properties.logs().fetchRetryDelayMillis();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(logFetchTimeout)
                .build();
    }

    public int maxLines() {
        return properties.logs().maxLines();
    }

    public Map<String, Object> readModuleLogs(String module, Integer requestedLines) {
        String normalizedModule = normalizeModule(module);
        int lines = sanitizeLines(requestedLines);
        String configuredPath = modulePath(normalizedModule);

        if (!StringUtils.hasText(configuredPath)) {
            throw new IllegalArgumentException("No log path configured for module: " + normalizedModule);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("module", normalizedModule);

        if (isHttpUrl(configuredPath)) {
            return readLogsFromUrl(configuredPath, lines, response);
        }

        Path path = Path.of(configuredPath);
        response.put("path", path.toString());

        if (!Files.exists(path)) {
            response.put("exists", false);
            response.put("requestedLines", lines);
            response.put("returnedLines", 0);
            response.put("lines", List.of());
            return response;
        }

        try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
            List<String> rows = stream
                    .skip(Math.max(0, countLines(path) - lines))
                    .toList();

            response.put("exists", true);
            response.put("sizeBytes", Files.size(path));
            response.put("requestedLines", lines);
            response.put("returnedLines", rows.size());
            response.put("lines", rows);
            return response;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read log file: " + ex.getMessage());
        }
    }

    private Map<String, Object> readLogsFromUrl(String configuredUrl, int lines, Map<String, Object> response) {
        response.put("path", configuredUrl);
        response.put("source", "http");

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

            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                throw new IllegalArgumentException("Failed to read log stream URL: HTTP " + httpResponse.statusCode()
                        + " | attempts: " + String.join(" | ", errors));
            }

            List<String> allLines = httpResponse.body().lines().toList();
            int startIndex = Math.max(0, allLines.size() - lines);
            List<String> tailLines = allLines.subList(startIndex, allLines.size());

            response.put("exists", true);
            response.put("requestedLines", lines);
            response.put("returnedLines", tailLines.size());
            response.put("lines", tailLines);
            return response;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalArgumentException("Failed to read log stream URL: " + ex.getMessage());
        }
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request, List<String> errors) throws IOException, InterruptedException {
        IOException lastIo = null;
        InterruptedException lastInterrupt = null;

        for (int attempt = 1; attempt <= fetchAttempts; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                errors.add("attempt " + attempt + " status " + response.statusCode());
                if (response.statusCode() >= 500 || response.statusCode() == 429) {
                    if (attempt < fetchAttempts) {
                        Thread.sleep(fetchRetryDelayMillis);
                        continue;
                    }
                }
                return response;
            } catch (IOException ex) {
                lastIo = ex;
                errors.add("attempt " + attempt + " io-error: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            } catch (InterruptedException ex) {
                lastInterrupt = ex;
                errors.add("attempt " + attempt + " interrupted: " + ex.getMessage());
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

    private HttpRequest buildTailRequest(String configuredUrl, boolean withRange) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(configuredUrl))
                .timeout(logFetchTimeout)
                .GET();
        if (withRange) {
            builder.header("Range", "bytes=-" + httpTailRangeBytes);
        }
        return builder.build();
    }

    private boolean isHttpUrl(String value) {
        String normalized = value.trim().toLowerCase();
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    private long countLines(Path path) throws IOException {
        try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
            return stream.count();
        }
    }

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
            case "mois-hotmart" -> properties.logs().moisHotmartPath();
            case "oprm-coletor-receita" -> properties.logs().oprmColetorReceitaPath();
            default -> throw new IllegalArgumentException("Unknown module: " + module);
        };
    }

    private String normalizeModule(String module) {
        if (!StringUtils.hasText(module)) {
            throw new IllegalArgumentException("module is required");
        }

        String normalized = module.trim().toLowerCase();
        return switch (normalized) {
            case "backend", "ai-worker", "lead-portal", "facebook-ads", "email-service", "lead-portal-payment",
                 "mds", "mois", "mois-hotmart", "oprm-coletor-receita" -> normalized;
            default -> throw new IllegalArgumentException(
                    "module must be one of: backend, ai-worker, lead-portal, facebook-ads, email-service, " +
                            "lead-portal-payment, mds, mois, mois-hotmart, oprm-coletor-receita");
        };
    }

    private int sanitizeLines(Integer requestedLines) {
        int lines = requestedLines == null ? DEFAULT_LINES : requestedLines;
        if (lines < 1 || lines > maxLines()) {
            throw new IllegalArgumentException("lines must be between 1 and " + maxLines());
        }
        return lines;
    }
}
