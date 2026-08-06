package com.marketinghub.mcpserver.service;

import com.marketinghub.mcpserver.config.McpProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Valida a consulta SSH restrita de inventário físico dos VPS.
 */
class VpsHostInventoryServiceTest {

    @TempDir
    private Path tempDir;

    /**
     * Garante que a tool coleta inventário somente do host permitido.
     */
    @Test
    void shouldInspectAllowedHost() throws Exception {
        VpsHostInventoryService service = new VpsHostInventoryService(buildProperties(fakeSshCommand(), true));

        Map<String, Object> result = service.inspect("191.252.210.83");

        assertEquals("191.252.210.83", result.get("host"));
        @SuppressWarnings("unchecked")
        Map<String, List<String>> sections = (Map<String, List<String>>) result.get("sections");
        assertEquals(List.of("ads-vps"), sections.get("hostname"));
        assertEquals(List.of("4"), sections.get("cpu"));
        assertEquals(List.of("tiktok-ads-worker|Up 1 minute|ghcr.io/acme/tiktok:sha"),
                sections.get("docker"));
    }

    /**
     * Garante que host fora da allowlist não pode ser consultado.
     */
    @Test
    void shouldRejectHostOutsideAllowList() throws Exception {
        VpsHostInventoryService service = new VpsHostInventoryService(buildProperties(fakeSshCommand(), true));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.inspect("10.0.0.1"));

        assertEquals("host must be one of: 191.252.210.83, 191.252.120.96", exception.getMessage());
    }

    /**
     * Garante que a tool fica bloqueada até ativação explícita por configuração.
     */
    @Test
    void shouldRejectWhenDisabled() throws Exception {
        VpsHostInventoryService service = new VpsHostInventoryService(buildProperties(fakeSshCommand(), false));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.inspect("191.252.210.83"));

        assertEquals("vps host inventory is disabled (set mcp.vps-host-inventory.enabled=true)",
                exception.getMessage());
    }

    /**
     * Garante que a leitura remota retorna somente logs do proxy permitido.
     */
    @Test
    void shouldReadAllowedRemoteDockerLogs() throws Exception {
        VpsHostInventoryService service = new VpsHostInventoryService(buildProperties(fakeSshCommand(), true));

        Map<String, Object> result = service.readDockerLogs(
                "191.252.210.83", "lead-portal-payments-proxy", 50, "certificate");

        assertEquals("lead-portal-payments-proxy", result.get("target"));
        assertEquals(1, result.get("returnedLines"));
        assertEquals(List.of("2026-08-03 certificate file missing"), result.get("lines"));
    }

    /**
     * Garante que nomes arbitrários de container não podem alcançar o SSH remoto.
     */
    @Test
    void shouldRejectRemoteDockerTargetOutsideAllowList() throws Exception {
        VpsHostInventoryService service = new VpsHostInventoryService(buildProperties(fakeSshCommand(), true));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.readDockerLogs("191.252.210.83", "mysql", 50, null));

        assertEquals("target must be one of: lead-portal-payments-proxy", exception.getMessage());
    }

    /**
     * Cria um executável falso para simular o SSH durante os testes.
     */
    private String fakeSshCommand() throws Exception {
        Path script = tempDir.resolve("ssh-fake.sh");
        Files.writeString(script, """
                #!/usr/bin/env sh
                case "$*" in
                  *"docker logs"*)
                    echo "__MCP_CONTAINER__"
                    echo "lead-portal-payments-service-proxy-1"
                    echo "__MCP_STATUS__"
                    echo "restarting|true|12|1|"
                    echo "__MCP_LOGS__"
                    echo "2026-08-03 certificate file missing"
                    echo "2026-08-03 nginx exited"
                    exit 0
                    ;;
                esac
                echo "__MCP_HOSTNAME__"
                echo "ads-vps"
                echo "__MCP_CPU__"
                echo "4"
                echo "__MCP_MEMORY__"
                echo "Mem: 7900 3200 4700"
                echo "__MCP_DOCKER__"
                echo "tiktok-ads-worker|Up 1 minute|ghcr.io/acme/tiktok:sha"
                """, StandardCharsets.UTF_8);
        script.toFile().setExecutable(true);
        return script.toString();
    }

    /**
     * Monta propriedades MCP mínimas para testar inventário de VPS.
     */
    private McpProperties buildProperties(String sshCommand, boolean enabled) {
        McpProperties.Logs logs = new McpProperties.Logs(
                "/tmp/backend.log",
                "/tmp/ai-worker.log",
                "/tmp/lead-portal.log",
                "/tmp/facebook-ads.log",
                "/tmp/email-service.log",
                "/tmp/lead-portal-payment.log",
                "/tmp/mds.log",
                "/tmp/mois.log",
                "/tmp/mois-sales-library-worker.log",
                "/tmp/mois-hotmart.log",
                "/tmp/clickbank-coletor-mois.log",
                "/tmp/oprm-coletor-receita.log",
                "/tmp/ops-monitor-worker.log",
                "/tmp/pde-platform-backend.log",
                "/tmp/video-management-service.log",
                "/tmp/customer-agent-worker.log",
                2,
                3,
                1,
                500,
                1024
        );
        McpProperties.ChatLogs chatLogs = new McpProperties.ChatLogs(
                true,
                List.of("marketinghub-fashion-chat"),
                "docker",
                500,
                5
        );
        McpProperties.DockerOps dockerOps = new McpProperties.DockerOps(
                true,
                List.of("marketinghub-backend"),
                "docker",
                500,
                5,
                false
        );
        McpProperties.BuildInfo buildInfo = new McpProperties.BuildInfo(
                true,
                List.of("pde-platform-backend"),
                Map.of("pde-platform-backend", "http://127.0.0.1:8096/actuator/info"),
                5
        );
        McpProperties.VpsHostInventory vpsHostInventory = new McpProperties.VpsHostInventory(
                enabled,
                List.of("191.252.210.83", "191.252.120.96"),
                sshCommand,
                "root",
                tempDir.resolve("id_ed25519").toString(),
                tempDir.resolve("known_hosts").toString(),
                5
        );
        McpProperties.ProductDiscoveryWorker productDiscoveryWorker = new McpProperties.ProductDiscoveryWorker(
                true,
                "product-discovery-worker",
                "docker",
                "http://127.0.0.1:8080/healthz",
                5
        );
        McpProperties.Meta meta = new McpProperties.Meta(
                true,
                "https://graph.facebook.com",
                "v23.0",
                "",
                "",
                List.of("developers.facebook.com")
        );
        McpProperties.Github github = new McpProperties.Github(
                false,
                "https://api.github.com",
                "owner",
                "repo",
                ""
        );
        return new McpProperties("marketing-hub-mcp", "1.0.0", logs, chatLogs, dockerOps,
                buildInfo, vpsHostInventory, productDiscoveryWorker, meta, github);
    }
}
