package com.marketinghub.mcpserver.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Protege os destinos padrão usados pelo MCP para distinguir os logs do backend e do AI Worker.
 */
class ModuleLogDefaultsContractTest {
    private static final String BACKEND_LOG_URL =
            "http://191.252.181.168/ops-mh-observability-v2/backend-log-stream-x9k";
    private static final String AI_WORKER_LOG_URL =
            "http://191.252.210.83:4567/worker-observability/logfile";

    /**
     * Garante que a configuração Spring não direcione o alias backend ao log do AI Worker.
     */
    @Test
    void shouldKeepBackendAndAiWorkerLogDefaultsDistinctInApplicationConfiguration() throws IOException {
        String configuration = Files.readString(Path.of("src/main/resources/application.yml"));

        assertTrue(configuration.contains("MCP_LOG_BACKEND_PATH:" + BACKEND_LOG_URL));
        assertTrue(configuration.contains("MCP_LOG_AI_WORKER_PATH:" + AI_WORKER_LOG_URL));
        assertFalse(configuration.contains("MCP_LOG_BACKEND_PATH:" + AI_WORKER_LOG_URL));
    }

    /**
     * Garante que o Compose preserve os destinos corretos quando não houver override no host.
     */
    @Test
    void shouldKeepBackendAndAiWorkerLogDefaultsDistinctInCompose() throws IOException {
        String compose = Files.readString(Path.of("docker-compose.yml"));

        assertTrue(compose.contains("MCP_LOG_BACKEND_PATH:-" + BACKEND_LOG_URL));
        assertTrue(compose.contains("MCP_LOG_AI_WORKER_PATH:-" + AI_WORKER_LOG_URL));
        assertFalse(compose.contains("MCP_LOG_BACKEND_PATH:-" + AI_WORKER_LOG_URL));
    }

    /**
     * Garante que o descritor efetivamente publicado fixe o endpoint canônico do backend.
     */
    @Test
    void shouldPublishCanonicalBackendLogEndpointInDeploymentCompose() throws IOException {
        String compose = Files.readString(Path.of("../deploy/docker-compose.mcp.yml"));

        assertTrue(compose.contains("MCP_LOG_BACKEND_PATH:-" + BACKEND_LOG_URL));
        assertFalse(compose.contains("MCP_LOG_BACKEND_PATH:-" + AI_WORKER_LOG_URL));
    }
}
