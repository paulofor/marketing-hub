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
            "http://191.252.181.168:8099/ops-mh-observability-v2/backend-log-stream-x9k";
    private static final String AI_WORKER_LOG_URL =
            "http://191.252.210.83:4567/worker-observability/logfile";
    private static final String CUSTOMER_AGENT_WORKER_LOG_URL =
            "http://163.245.202.80:8099/ops-customer-agent-observability-v1/customer-agent-worker-log";
    private static final String FINANCIAL_AGENT_WORKER_LOG_URL =
            "http://163.245.202.80:8095/ops-financial-agent-observability-v1/financial-agent-worker-log";
    private static final String EXPERIMENT_STRATEGIST_WORKER_LOG_URL =
            "http://163.245.202.80:8096/ops-experiment-strategist-observability-v1/logfile";
    private static final String META_AD_APPROVER_WORKER_LOG_URL =
            "http://163.245.202.80:8097/ops-meta-ad-approver-observability-v1/logfile";
    private static final String PRODUCT_DISCOVERY_WORKER_LOG_URL =
            "http://191.252.120.96:18081/ops-product-discovery-observability-v1/logfile";
    private static final String PRODUCT_DISCOVERY_WORKER_HEALTH_URL =
            "http://191.252.120.96:18081/healthz";

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

    /**
     * Garante que os logs do backend continuem legíveis quando o processo Java estiver indisponível.
     */
    @Test
    void shouldKeepBackendLogReaderIndependentFromBackendRuntime() throws IOException {
        String appCompose = Files.readString(Path.of("../deploy/docker-compose.yml"));
        String nginxConfiguration = Files.readString(Path.of("../deploy/nginx/backend-logs/default.conf"));
        String applyScript = Files.readString(Path.of("../deploy/bin/apply.sh"));

        assertTrue(appCompose.contains("backend-log-reader:"));
        assertTrue(appCompose.contains("./volumes/backend/logs:/var/log/marketinghub/backend:ro"));
        assertTrue(nginxConfiguration.contains("alias /var/log/marketinghub/backend/marketinghub-backend.log;"));
        assertTrue(applyScript.contains("backend backend-log-reader frontend"));
    }

    /**
     * Garante que aplicação e descritores de deploy publiquem o destino do Agente Cliente.
     */
    @Test
    void shouldPublishCustomerAgentWorkerLogEndpoint() throws IOException {
        String configuration = Files.readString(Path.of("src/main/resources/application.yml"));
        String localCompose = Files.readString(Path.of("docker-compose.yml"));
        String deploymentCompose = Files.readString(Path.of("../deploy/docker-compose.mcp.yml"));

        assertTrue(configuration.contains("MCP_LOG_CUSTOMER_AGENT_WORKER_PATH:" + CUSTOMER_AGENT_WORKER_LOG_URL));
        assertTrue(localCompose.contains("MCP_LOG_CUSTOMER_AGENT_WORKER_PATH:-" + CUSTOMER_AGENT_WORKER_LOG_URL));
        assertTrue(deploymentCompose.contains("MCP_LOG_CUSTOMER_AGENT_WORKER_PATH:-" + CUSTOMER_AGENT_WORKER_LOG_URL));
    }

    /**
     * Garante que aplicação e descritores de deploy publiquem o destino do Agente Financeiro.
     */
    @Test
    void shouldPublishFinancialAgentWorkerLogEndpoint() throws IOException {
        String configuration = Files.readString(Path.of("src/main/resources/application.yml"));
        String localCompose = Files.readString(Path.of("docker-compose.yml"));
        String deploymentCompose = Files.readString(Path.of("../deploy/docker-compose.mcp.yml"));

        assertTrue(configuration.contains("MCP_LOG_FINANCIAL_AGENT_WORKER_PATH:" + FINANCIAL_AGENT_WORKER_LOG_URL));
        assertTrue(localCompose.contains("MCP_LOG_FINANCIAL_AGENT_WORKER_PATH:-" + FINANCIAL_AGENT_WORKER_LOG_URL));
        assertTrue(deploymentCompose.contains("MCP_LOG_FINANCIAL_AGENT_WORKER_PATH:-" + FINANCIAL_AGENT_WORKER_LOG_URL));
    }

    /**
     * Garante que aplicação e descritores de deploy consultem o logfile real da Atena.
     */
    @Test
    void shouldPublishExperimentStrategistWorkerLogEndpoint() throws IOException {
        String configuration = Files.readString(Path.of("src/main/resources/application.yml"));
        String localCompose = Files.readString(Path.of("docker-compose.yml"));
        String deploymentCompose = Files.readString(Path.of("../deploy/docker-compose.yml"));

        assertTrue(configuration.contains(
                "MCP_LOG_EXPERIMENT_STRATEGIST_WORKER_PATH:" + EXPERIMENT_STRATEGIST_WORKER_LOG_URL));
        assertTrue(localCompose.contains(
                "MCP_LOG_EXPERIMENT_STRATEGIST_WORKER_PATH:-" + EXPERIMENT_STRATEGIST_WORKER_LOG_URL));
        assertTrue(deploymentCompose.contains(
                "MCP_LOG_EXPERIMENT_STRATEGIST_WORKER_PATH:-" + EXPERIMENT_STRATEGIST_WORKER_LOG_URL));
    }

    /**
     * Garante que aplicação e descritores de deploy publiquem o destino do Aprovador Meta.
     */
    @Test
    void shouldPublishMetaAdApproverWorkerLogEndpoint() throws IOException {
        String configuration = Files.readString(Path.of("src/main/resources/application.yml"));
        String localCompose = Files.readString(Path.of("docker-compose.yml"));
        String deploymentCompose = Files.readString(Path.of("../deploy/docker-compose.mcp.yml"));

        assertTrue(configuration.contains("MCP_LOG_META_AD_APPROVER_WORKER_PATH:" + META_AD_APPROVER_WORKER_LOG_URL));
        assertTrue(localCompose.contains("MCP_LOG_META_AD_APPROVER_WORKER_PATH:-" + META_AD_APPROVER_WORKER_LOG_URL));
        assertTrue(deploymentCompose.contains("MCP_LOG_META_AD_APPROVER_WORKER_PATH:-" + META_AD_APPROVER_WORKER_LOG_URL));
    }

    /**
     * Garante que o MCP publicado observe Argos por HTTP no host real do executor.
     */
    @Test
    void shouldPublishProductDiscoveryWorkerObservabilityEndpoints() throws IOException {
        String configuration = Files.readString(Path.of("src/main/resources/application.yml"));
        String localCompose = Files.readString(Path.of("docker-compose.yml"));
        String isolatedDeploymentCompose = Files.readString(Path.of("../deploy/docker-compose.mcp.yml"));
        String deploymentCompose = Files.readString(Path.of("../deploy/docker-compose.yml"));

        assertTrue(configuration.contains(
                "MCP_LOG_PRODUCT_DISCOVERY_WORKER_PATH:" + PRODUCT_DISCOVERY_WORKER_LOG_URL));
        assertTrue(configuration.contains(
                "MCP_PRODUCT_DISCOVERY_WORKER_HEALTH_URL:" + PRODUCT_DISCOVERY_WORKER_HEALTH_URL));
        assertTrue(localCompose.contains(
                "MCP_LOG_PRODUCT_DISCOVERY_WORKER_PATH:-" + PRODUCT_DISCOVERY_WORKER_LOG_URL));
        assertTrue(localCompose.contains(
                "MCP_PRODUCT_DISCOVERY_WORKER_HEALTH_URL:-" + PRODUCT_DISCOVERY_WORKER_HEALTH_URL));
        assertTrue(isolatedDeploymentCompose.contains(
                "MCP_LOG_PRODUCT_DISCOVERY_WORKER_PATH:-" + PRODUCT_DISCOVERY_WORKER_LOG_URL));
        assertTrue(isolatedDeploymentCompose.contains(
                "MCP_PRODUCT_DISCOVERY_WORKER_HEALTH_URL:-" + PRODUCT_DISCOVERY_WORKER_HEALTH_URL));
        assertTrue(deploymentCompose.contains(
                "MCP_LOG_PRODUCT_DISCOVERY_WORKER_PATH:-" + PRODUCT_DISCOVERY_WORKER_LOG_URL));
        assertTrue(deploymentCompose.contains(
                "MCP_PRODUCT_DISCOVERY_WORKER_HEALTH_URL:-" + PRODUCT_DISCOVERY_WORKER_HEALTH_URL));
    }
}
