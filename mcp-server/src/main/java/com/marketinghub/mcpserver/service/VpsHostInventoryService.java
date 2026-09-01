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
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Consulta inventário físico e operacional dos VPS permitidos usando SSH restrito.
 */
@Service
public class VpsHostInventoryService {
    private static final Logger logger = LoggerFactory.getLogger(VpsHostInventoryService.class);
    private static final String LEAD_PORTAL_STACK = "lead-portal-stack";
    private static final String LEAD_PORTAL_PAYMENTS_PROXY = "lead-portal-payments-proxy";
    private static final List<String> ALLOWED_DOCKER_LOG_TARGETS =
            List.of(LEAD_PORTAL_STACK, LEAD_PORTAL_PAYMENTS_PROXY);
    private static final String BACKEND_MODULE = "backend";
    private static final String RESTART_CONFIRMATION = "RESTART_BACKEND";
    private static final int BACKEND_RESTART_STOP_TIMEOUT_SECONDS = 5;
    private static final Pattern SAFE_CONTAINER_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.-]{0,127}");

    private final McpProperties properties;
    private final HttpClient httpClient;
    private final AtomicReference<Instant> lastBackendRestart = new AtomicReference<>();

    /**
     * Inicializa o serviço com allowlist de hosts e parâmetros SSH do MCP.
     */
    public VpsHostInventoryService(McpProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.vpsHostInventory().timeoutSeconds()))
                .build();
    }

    /**
     * Recupera exclusivamente o backend canônico quando ele estiver indisponível e confirma o health posterior.
     */
    public synchronized Map<String, Object> recoverBackend(String module, String confirmation, String reason) {
        McpProperties.VpsHostInventory config = properties.vpsHostInventory();
        validateBackendRecovery(config, module, confirmation, reason);

        Map<String, Object> healthBefore = readBackendHealth(config);
        if (Boolean.TRUE.equals(healthBefore.get("healthy"))) {
            return recoveryResult("not_required", reason, healthBefore, healthBefore, List.of());
        }

        enforceRecoveryCooldown(config);
        List<String> restartOutput = executeSshBackendRestart(config);
        lastBackendRestart.set(Instant.now());
        Map<String, Object> healthAfter = awaitBackendHealth(config);
        String status = Boolean.TRUE.equals(healthAfter.get("healthy")) ? "recovered" : "restart_unhealthy";
        logger.info("MCP backend recovery completed module={} host={} container={} status={} reason={}",
                BACKEND_MODULE, config.backendHost(), config.backendContainer(), status, reason);
        return recoveryResult(status, reason, healthBefore, healthAfter, restartOutput);
    }

    /**
     * Valida habilitação, confirmação, justificativa e alvos fixos da recuperação.
     */
    private void validateBackendRecovery(McpProperties.VpsHostInventory config, String module,
                                         String confirmation, String reason) {
        if (!config.enabled() || !config.backendRecoveryEnabled()) {
            throw new IllegalArgumentException("backend recovery is disabled");
        }
        if (!BACKEND_MODULE.equals(module)) {
            throw new IllegalArgumentException("module must be backend");
        }
        if (!RESTART_CONFIRMATION.equals(confirmation)) {
            throw new IllegalArgumentException("confirmation must be " + RESTART_CONFIRMATION);
        }
        if (!StringUtils.hasText(reason) || reason.trim().length() < 10 || reason.trim().length() > 300
                || reason.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("reason must contain between 10 and 300 characters");
        }
        normalizeHost(config.backendHost());
        if (!SAFE_CONTAINER_NAME.matcher(config.backendContainer()).matches()) {
            throw new IllegalArgumentException("configured backend container is invalid");
        }
    }

    /**
     * Impede reinícios repetidos durante a janela de estabilização do backend.
     */
    private void enforceRecoveryCooldown(McpProperties.VpsHostInventory config) {
        Instant previous = lastBackendRestart.get();
        if (previous == null) {
            return;
        }
        long remaining = Duration.between(Instant.now(),
                previous.plusSeconds(config.backendRecoveryCooldownSeconds())).toSeconds();
        if (remaining > 0) {
            throw new IllegalArgumentException("backend recovery cooldown active for " + remaining + " seconds");
        }
    }

    /**
     * Executa via SSH somente o restart do backend, com parada curta para concluir antes do timeout do MCP.
     */
    private List<String> executeSshBackendRestart(McpProperties.VpsHostInventory config) {
        String destination = config.user() + "@" + config.backendHost();
        List<String> command = List.of(
                config.sshCommand(), "-i", config.identityFile(), "-o", "BatchMode=yes",
                "-o", "IdentitiesOnly=yes", "-o", "StrictHostKeyChecking=accept-new",
                "-o", "UserKnownHostsFile=" + config.knownHostsFile(),
                "-o", "ConnectTimeout=" + config.timeoutSeconds(), destination,
                "docker restart --time " + BACKEND_RESTART_STOP_TIMEOUT_SECONDS + " "
                        + config.backendContainer());
        return executeCommand(command, "ssh backend restart", config.backendHost());
    }

    /**
     * Aguarda o backend responder com sucesso após o restart, respeitando tentativas configuradas.
     */
    private Map<String, Object> awaitBackendHealth(McpProperties.VpsHostInventory config) {
        Map<String, Object> result = Map.of("healthy", false, "detail", "health check not attempted");
        for (int attempt = 1; attempt <= config.backendHealthAttempts(); attempt++) {
            result = readBackendHealth(config);
            if (Boolean.TRUE.equals(result.get("healthy"))) {
                return result;
            }
            if (attempt < config.backendHealthAttempts()) {
                try {
                    Thread.sleep(config.backendHealthDelayMillis());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalArgumentException("backend health verification interrupted");
                }
            }
        }
        return result;
    }

    /**
     * Consulta o endpoint de saúde canônico sem expor conteúdo sensível da resposta.
     */
    private Map<String, Object> readBackendHealth(McpProperties.VpsHostInventory config) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.backendHealthUrl()))
                    .timeout(Duration.ofSeconds(config.timeoutSeconds())).GET().build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return Map.of("healthy", response.statusCode() >= 200 && response.statusCode() < 300,
                    "httpStatus", response.statusCode());
        } catch (IOException ex) {
            logger.warn("MCP backend health check failed url={} reason={}",
                    config.backendHealthUrl(), ex.getMessage());
            return Map.of("healthy", false, "detail", "connection_failed");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Map.of("healthy", false, "detail", "interrupted");
        } catch (IllegalArgumentException ex) {
            logger.error("MCP backend health URL is invalid url={}", config.backendHealthUrl(), ex);
            return Map.of("healthy", false, "detail", "invalid_health_url");
        }
    }

    /**
     * Monta a evidência estruturada e auditável da tentativa de recuperação.
     */
    private Map<String, Object> recoveryResult(String status, String reason, Map<String, Object> healthBefore,
                                               Map<String, Object> healthAfter, List<String> restartOutput) {
        McpProperties.VpsHostInventory config = properties.vpsHostInventory();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("module", BACKEND_MODULE);
        result.put("host", config.backendHost());
        result.put("container", config.backendContainer());
        result.put("status", status);
        result.put("reason", reason.trim());
        result.put("healthBefore", healthBefore);
        result.put("healthAfter", healthAfter);
        result.put("restartOutput", restartOutput);
        result.put("checkedAt", Instant.now().toString());
        return result;
    }

    /**
     * Retorna os hosts que podem ser consultados pela tool de inventário.
     */
    public List<String> allowedHosts() {
        return properties.vpsHostInventory().allowedHosts();
    }

    /**
     * Retorna os alvos remotos de logs Docker liberados para diagnóstico.
     */
    public List<String> allowedDockerLogTargets() {
        return ALLOWED_DOCKER_LOG_TARGETS;
    }

    /**
     * Consulta um VPS permitido e retorna linhas de inventário separadas por seções.
     */
    public Map<String, Object> inspect(String host) {
        if (!properties.vpsHostInventory().enabled()) {
            throw new IllegalArgumentException(
                    "vps host inventory is disabled (set mcp.vps-host-inventory.enabled=true)");
        }
        String normalizedHost = normalizeHost(host);
        List<String> outputLines = executeSshInventory(normalizedHost);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("host", normalizedHost);
        response.put("user", properties.vpsHostInventory().user());
        response.put("sections", parseSections(outputLines));
        response.put("lines", outputLines);
        return response;
    }

    /**
     * Lê os logs Docker de um alvo remoto conhecido sem aceitar comandos ou nomes arbitrários.
     */
    public Map<String, Object> readDockerLogs(String host, String target, int lines, String contains) {
        if (!properties.vpsHostInventory().enabled()) {
            throw new IllegalArgumentException(
                    "vps host inventory is disabled (set mcp.vps-host-inventory.enabled=true)");
        }
        String normalizedHost = normalizeHost(host);
        String normalizedTarget = normalizeDockerLogTarget(target);
        int maxLines = properties.dockerOps().maxLines();
        if (lines < 1 || lines > maxLines) {
            throw new IllegalArgumentException("lines must be between 1 and " + maxLines);
        }

        List<String> outputLines = executeSshDockerLogs(normalizedHost, normalizedTarget, lines);
        if (StringUtils.hasText(contains)) {
            outputLines = outputLines.stream().filter(line -> line.contains(contains)).toList();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("host", normalizedHost);
        response.put("target", normalizedTarget);
        response.put("requestedLines", lines);
        response.put("returnedLines", outputLines.size());
        response.put("lines", outputLines);
        return response;
    }

    /**
     * Normaliza e valida se o host solicitado está liberado na allowlist.
     */
    private String normalizeHost(String host) {
        if (!StringUtils.hasText(host)) {
            throw new IllegalArgumentException("host is required");
        }
        String normalized = host.trim();
        if (!properties.vpsHostInventory().allowedHosts().contains(normalized)) {
            throw new IllegalArgumentException("host must be one of: "
                    + String.join(", ", properties.vpsHostInventory().allowedHosts()));
        }
        return normalized;
    }

    /**
     * Valida o alvo lógico para impedir consulta de containers arbitrários no VPS.
     */
    private String normalizeDockerLogTarget(String target) {
        if (!StringUtils.hasText(target)) {
            throw new IllegalArgumentException("target is required");
        }
        String normalized = target.trim();
        if (!ALLOWED_DOCKER_LOG_TARGETS.contains(normalized)) {
            throw new IllegalArgumentException("target must be one of: "
                    + String.join(", ", ALLOWED_DOCKER_LOG_TARGETS));
        }
        return normalized;
    }

    /**
     * Executa somente o script fixo de inventário no host remoto.
     */
    private List<String> executeSshInventory(String host) {
        String destination = properties.vpsHostInventory().user() + "@" + host;
        List<String> command = List.of(
                properties.vpsHostInventory().sshCommand(),
                "-i", properties.vpsHostInventory().identityFile(),
                "-o", "BatchMode=yes",
                "-o", "IdentitiesOnly=yes",
                "-o", "StrictHostKeyChecking=accept-new",
                "-o", "UserKnownHostsFile=" + properties.vpsHostInventory().knownHostsFile(),
                "-o", "ConnectTimeout=" + properties.vpsHostInventory().timeoutSeconds(),
                destination,
                inventoryScript()
        );
        return executeCommand(command, "ssh host inventory", host);
    }

    /**
     * Executa uma leitura remota fixa para o alvo operacional permitido.
     */
    private List<String> executeSshDockerLogs(String host, String target, int lines) {
        String destination = properties.vpsHostInventory().user() + "@" + host;
        String script = switch (target) {
            case LEAD_PORTAL_STACK -> leadPortalStackLogsScript(lines);
            case LEAD_PORTAL_PAYMENTS_PROXY -> leadPortalPaymentsProxyLogsScript(lines);
            default -> throw new IllegalArgumentException("unsupported docker log target: " + target);
        };
        List<String> command = List.of(
                properties.vpsHostInventory().sshCommand(),
                "-i", properties.vpsHostInventory().identityFile(),
                "-o", "BatchMode=yes",
                "-o", "IdentitiesOnly=yes",
                "-o", "StrictHostKeyChecking=accept-new",
                "-o", "UserKnownHostsFile=" + properties.vpsHostInventory().knownHostsFile(),
                "-o", "ConnectTimeout=" + properties.vpsHostInventory().timeoutSeconds(),
                destination,
                script
        );
        return executeCommand(command, "ssh docker logs", host);
    }

    /**
     * Retorna estado e logs dos três containers canônicos do Lead Portal sem aceitar nomes livres.
     */
    private String leadPortalStackLogsScript(int lines) {
        return """
                for container in lead-portal-backend lead-portal-frontend lead-portal-proxy; do
                  printf '__MCP_CONTAINER__\\n%s\\n' "$container"
                  if ! docker inspect "$container" >/dev/null 2>&1; then
                    printf '__MCP_STATUS__\\nnot_found|false|0||||\\n'
                    printf '__MCP_LOGS__\\ncontainer not found\\n'
                    continue
                  fi
                  printf '__MCP_STATUS__\\n'
                  docker inspect --format '{{.State.Status}}|{{.State.Restarting}}|{{.RestartCount}}|{{.State.ExitCode}}|{{if .State.Health}}{{.State.Health.Status}}{{end}}|{{.Config.Image}}' "$container"
                  printf '__MCP_LOGS__\\n'
                  docker logs --timestamps --tail __MCP_LINES__ "$container" 2>&1
                done
                """.replace("__MCP_LINES__", Integer.toString(lines));
    }

    /**
     * Resolve nomes conhecidos do container do proxy e retorna sua situação seguida dos logs recentes.
     */
    private String leadPortalPaymentsProxyLogsScript(int lines) {
        return """
                container=''
                for candidate in lead-portal-payments-service-proxy-1 lead-portal-payments-service_proxy_1 lead-portal-payments-proxy-1 lead-portal-payments_proxy_1; do
                  if docker inspect "$candidate" >/dev/null 2>&1; then container="$candidate"; break; fi
                done
                if [ -z "$container" ]; then
                  echo 'proxy container not found; known candidates:' >&2
                  docker ps -a --format '{{.Names}}|{{.Status}}|{{.Image}}' | sed -n '/lead-portal-payments.*proxy/p' >&2
                  exit 4
                fi
                printf '__MCP_CONTAINER__\\n%s\\n' "$container"
                printf '__MCP_STATUS__\\n'
                docker inspect --format '{{.State.Status}}|{{.State.Restarting}}|{{.RestartCount}}|{{.State.ExitCode}}|{{.State.Error}}' "$container"
                printf '__MCP_LOGS__\\n'
                docker logs --timestamps --tail __MCP_LINES__ "$container" 2>&1
                """.replace("__MCP_LINES__", Integer.toString(lines));
    }

    /**
     * Define os comandos remotos permitidos para inventário de host.
     */
    private String inventoryScript() {
        return """
                printf '__MCP_HOSTNAME__\\n'; hostname 2>/dev/null || true
                printf '__MCP_UPTIME__\\n'; uptime -p 2>/dev/null || uptime 2>/dev/null || true
                printf '__MCP_CPU__\\n'; nproc 2>/dev/null || true; lscpu 2>/dev/null | sed -n '1,12p' || true
                printf '__MCP_MEMORY__\\n'; free -m 2>/dev/null || true
                printf '__MCP_DISK__\\n'; df -h / 2>/dev/null || true
                printf '__MCP_OS__\\n'; cat /etc/os-release 2>/dev/null | sed -n '1,8p' || true
                printf '__MCP_PORTS__\\n'; (ss -lntp 2>/dev/null || netstat -lntp 2>/dev/null || true) | sed -n '1,80p'
                printf '__MCP_DOCKER__\\n'; docker ps --format '{{.Names}}|{{.Status}}|{{.Image}}' 2>/dev/null || true
                """;
    }

    /**
     * Executa o comando local configurado com timeout e converte a saída para linhas.
     */
    private List<String> executeCommand(List<String> command, String operation, String host) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(properties.vpsHostInventory().timeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalArgumentException(operation + " timed out after "
                        + Duration.ofSeconds(properties.vpsHostInventory().timeoutSeconds()).toSeconds() + " seconds");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            List<String> outputLines = output.isBlank() ? List.of() : output.lines().toList();
            if (process.exitValue() != 0) {
                throw new IllegalArgumentException(operation + " failed: " + String.join("\n", outputLines));
            }
            return outputLines;
        } catch (IOException ex) {
            logger.error("mcp-server vps host inventory failed to start operation={} host={}", operation, host, ex);
            throw new IllegalArgumentException("failed to execute " + operation + ": " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.error("mcp-server vps host inventory interrupted operation={} host={}", operation, host, ex);
            throw new IllegalArgumentException(operation + " interrupted");
        }
    }

    /**
     * Organiza a saída textual do inventário por seções conhecidas.
     */
    private Map<String, List<String>> parseSections(List<String> lines) {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        String currentSection = "raw";
        sections.put(currentSection, new java.util.ArrayList<>());
        for (String line : lines) {
            if (line.startsWith("__MCP_") && line.endsWith("__")) {
                currentSection = line.substring("__MCP_".length(), line.length() - "__".length()).toLowerCase();
                sections.putIfAbsent(currentSection, new java.util.ArrayList<>());
                continue;
            }
            sections.get(currentSection).add(line);
        }
        return sections;
    }
}
