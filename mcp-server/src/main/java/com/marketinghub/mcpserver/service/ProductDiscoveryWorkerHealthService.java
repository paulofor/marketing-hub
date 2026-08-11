package com.marketinghub.mcpserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mcpserver.config.McpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Consulta o health operacional do Product Discovery Worker no host real do executor.
 */
@Service
public class ProductDiscoveryWorkerHealthService {
    private static final Logger logger = LoggerFactory.getLogger(ProductDiscoveryWorkerHealthService.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final McpProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * Inicializa o serviço com a configuração do container e parser JSON.
     */
    public ProductDiscoveryWorkerHealthService(McpProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /**
     * Executa a leitura HTTP do endpoint de health publicado pelo worker.
     */
    public Map<String, Object> readHealth() {
        McpProperties.ProductDiscoveryWorker config = properties.productDiscoveryWorker();
        if (!config.enabled()) {
            throw new IllegalArgumentException("product discovery worker health is disabled");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.healthUrl()))
                    .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                throw new IllegalArgumentException(
                        "product discovery worker health failed: HTTP " + httpResponse.statusCode());
            }
            String output = httpResponse.body();
            Map<String, Object> payload = objectMapper.readValue(output, MAP_TYPE);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("container", config.container());
            response.put("healthUrl", config.healthUrl());
            response.put("reachable", true);
            response.put("payload", payload);
            return response;
        } catch (IOException ex) {
            logger.error("mcp-server readHealth failed url={}", config.healthUrl(), ex);
            throw new IllegalArgumentException("failed to read product discovery worker health: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.error("mcp-server readHealth interrupted url={}", config.healthUrl(), ex);
            throw new IllegalArgumentException("product discovery worker health interrupted");
        }
    }
}
