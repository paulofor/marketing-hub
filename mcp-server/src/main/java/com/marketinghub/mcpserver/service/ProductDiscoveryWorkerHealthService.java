package com.marketinghub.mcpserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mcpserver.config.McpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Consulta o health operacional do Product Discovery Worker por Docker com comando restrito.
 */
@Service
public class ProductDiscoveryWorkerHealthService {
    private static final Logger logger = LoggerFactory.getLogger(ProductDiscoveryWorkerHealthService.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final McpProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Inicializa o serviço com a configuração do container e parser JSON.
     */
    public ProductDiscoveryWorkerHealthService(McpProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Executa a leitura do endpoint de health dentro do container operacional do worker.
     */
    public Map<String, Object> readHealth() {
        McpProperties.ProductDiscoveryWorker config = properties.productDiscoveryWorker();
        if (!config.enabled()) {
            throw new IllegalArgumentException("product discovery worker health is disabled");
        }

        String output = executeDockerHealthCheck(config);
        try {
            Map<String, Object> payload = objectMapper.readValue(output, MAP_TYPE);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("container", config.container());
            response.put("healthUrl", config.healthUrl());
            response.put("reachable", true);
            response.put("payload", payload);
            return response;
        } catch (IOException ex) {
            logger.error("mcp-server readHealth failed to parse product-discovery-worker health payload", ex);
            throw new IllegalArgumentException("invalid product discovery worker health payload: " + ex.getMessage());
        }
    }

    /**
     * Chama o Node do próprio container para consultar o endpoint HTTP local do worker.
     */
    private String executeDockerHealthCheck(McpProperties.ProductDiscoveryWorker config) {
        String script = """
                fetch(process.argv[1])
                  .then(async (response) => {
                    const body = await response.text();
                    process.stdout.write(body);
                    if (!response.ok) process.exit(2);
                  })
                  .catch((error) => {
                    console.error(error.message);
                    process.exit(1);
                  });
                """;
        List<String> command = List.of(
                config.dockerCommand(),
                "exec",
                config.container(),
                "node",
                "-e",
                script,
                config.healthUrl()
        );
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(config.timeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalArgumentException("product discovery worker health timed out after "
                        + Duration.ofSeconds(config.timeoutSeconds()).toSeconds() + " seconds");
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IllegalArgumentException("product discovery worker health failed: " + output.strip());
            }
            return output;
        } catch (IOException ex) {
            logger.error("mcp-server executeDockerHealthCheck failed to start docker command for container={}",
                    config.container(), ex);
            throw new IllegalArgumentException("failed to execute product discovery worker health: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.error("mcp-server executeDockerHealthCheck interrupted for container={}", config.container(), ex);
            throw new IllegalArgumentException("product discovery worker health interrupted");
        }
    }
}
