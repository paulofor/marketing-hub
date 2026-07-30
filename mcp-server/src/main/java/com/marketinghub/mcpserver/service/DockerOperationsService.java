package com.marketinghub.mcpserver.service;

import com.marketinghub.mcpserver.config.McpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Executa operações Docker restritas para diagnóstico e recuperação operacional no host MCP.
 */
@Service
public class DockerOperationsService {
    private static final Logger logger = LoggerFactory.getLogger(DockerOperationsService.class);
    private static final int DEFAULT_LINES = 200;
    private static final List<String> ACTIONS = List.of("ps", "logs", "restart");

    private final McpProperties properties;

    /**
     * Inicializa o serviço com allowlist de containers e limites de execução Docker.
     */
    public DockerOperationsService(McpProperties properties) {
        this.properties = properties;
    }

    /**
     * Retorna as ações Docker aceitas pela tool operacional.
     */
    public List<String> allowedActions() {
        return ACTIONS;
    }

    /**
     * Retorna os containers que podem ser consultados ou reiniciados pelo MCP.
     */
    public List<String> allowedContainers() {
        return properties.dockerOps().allowedContainers();
    }

    /**
     * Retorna o limite máximo de linhas de logs por chamada.
     */
    public int maxLines() {
        return properties.dockerOps().maxLines();
    }

    /**
     * Executa a ação Docker solicitada quando ela estiver habilitada e dentro da allowlist.
     */
    public Map<String, Object> execute(String action, String container, Integer requestedLines, String contains) {
        if (!properties.dockerOps().enabled()) {
            throw new IllegalArgumentException("docker operations are disabled");
        }

        String normalizedAction = normalizeAction(action);
        return switch (normalizedAction) {
            case "ps" -> executePs();
            case "logs" -> executeLogs(container, requestedLines, contains);
            case "restart" -> executeRestart(container);
            default -> throw new IllegalArgumentException("action must be one of: " + String.join(", ", ACTIONS));
        };
    }

    /**
     * Normaliza e valida a ação solicitada.
     */
    private String normalizeAction(String action) {
        if (!StringUtils.hasText(action)) {
            throw new IllegalArgumentException("action is required");
        }
        String normalized = action.trim().toLowerCase();
        if (!ACTIONS.contains(normalized)) {
            throw new IllegalArgumentException("action must be one of: " + String.join(", ", ACTIONS));
        }
        return normalized;
    }

    /**
     * Valida se o container solicitado está liberado para operações Docker.
     */
    private String normalizeContainer(String container) {
        if (!StringUtils.hasText(container)) {
            throw new IllegalArgumentException("container is required for this action");
        }
        String normalized = container.trim();
        if (!properties.dockerOps().allowedContainers().contains(normalized)) {
            throw new IllegalArgumentException("container must be one of: "
                    + String.join(", ", properties.dockerOps().allowedContainers()));
        }
        return normalized;
    }

    /**
     * Ajusta a quantidade de linhas ao intervalo permitido pela configuração.
     */
    private int sanitizeLines(Integer requestedLines) {
        if (requestedLines == null) {
            return Math.min(DEFAULT_LINES, properties.dockerOps().maxLines());
        }
        if (requestedLines < 1 || requestedLines > properties.dockerOps().maxLines()) {
            throw new IllegalArgumentException("lines must be between 1 and " + properties.dockerOps().maxLines());
        }
        return requestedLines;
    }

    /**
     * Lista containers Docker do host em formato previsível para diagnóstico.
     */
    private Map<String, Object> executePs() {
        List<String> lines = executeDockerCommand(List.of(
                properties.dockerOps().dockerCommand(),
                "ps",
                "--all",
                "--format",
                "{{.Names}}|{{.Status}}|{{.Image}}"
        ), "docker ps", "");

        List<Map<String, Object>> containers = lines.stream()
                .filter(StringUtils::hasText)
                .map(this::parsePsLine)
                .filter(container -> Boolean.TRUE.equals(container.get("allowed")))
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("action", "ps");
        response.put("returnedContainers", containers.size());
        response.put("allowedContainers", properties.dockerOps().allowedContainers());
        response.put("containers", containers);
        return response;
    }

    /**
     * Converte uma linha do docker ps em campos úteis para a resposta estruturada.
     */
    private Map<String, Object> parsePsLine(String line) {
        String[] parts = line.split("\\|", 3);
        Map<String, Object> container = new LinkedHashMap<>();
        container.put("name", parts.length > 0 ? parts[0] : "");
        container.put("status", parts.length > 1 ? parts[1] : "");
        container.put("image", parts.length > 2 ? parts[2] : "");
        container.put("allowed", parts.length > 0 && properties.dockerOps().allowedContainers().contains(parts[0]));
        return container;
    }

    /**
     * Retorna logs Docker do container permitido com filtro textual opcional.
     */
    private Map<String, Object> executeLogs(String container, Integer requestedLines, String contains) {
        String normalizedContainer = normalizeContainer(container);
        int lines = sanitizeLines(requestedLines);
        List<String> outputLines = executeDockerCommand(List.of(
                properties.dockerOps().dockerCommand(),
                "logs",
                "--tail",
                String.valueOf(lines),
                "--timestamps",
                normalizedContainer
        ), "docker logs", normalizedContainer);
        List<String> filteredLines = applyContainsFilter(outputLines, contains);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("action", "logs");
        response.put("container", normalizedContainer);
        response.put("requestedLines", lines);
        response.put("returnedLines", filteredLines.size());
        response.put("contains", contains == null ? "" : contains);
        response.put("lines", filteredLines);
        return response;
    }

    /**
     * Reinicia um container permitido quando a operação estiver explicitamente habilitada.
     */
    private Map<String, Object> executeRestart(String container) {
        if (!properties.dockerOps().restartEnabled()) {
            throw new IllegalArgumentException("docker restart is disabled (set mcp.docker-ops.restart-enabled=true)");
        }
        String normalizedContainer = normalizeContainer(container);
        List<String> outputLines = executeDockerCommand(List.of(
                properties.dockerOps().dockerCommand(),
                "restart",
                normalizedContainer
        ), "docker restart", normalizedContainer);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("action", "restart");
        response.put("container", normalizedContainer);
        response.put("status", "executed");
        response.put("lines", outputLines);
        return response;
    }

    /**
     * Chama o Docker CLI configurado com timeout e log operacional de falhas.
     */
    private List<String> executeDockerCommand(List<String> command, String operation, String container) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(properties.dockerOps().timeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalArgumentException(operation + " timed out after "
                        + Duration.ofSeconds(properties.dockerOps().timeoutSeconds()).toSeconds() + " seconds");
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            List<String> outputLines = output.isBlank() ? List.of() : output.lines().toList();
            if (process.exitValue() != 0) {
                throw new IllegalArgumentException(operation + " failed: " + String.join("\n", outputLines));
            }
            return outputLines;
        } catch (IOException ex) {
            logger.error("mcp-server executeDockerCommand failed to start command operation={} container={}",
                    operation, container, ex);
            throw new IllegalArgumentException("failed to execute " + operation + ": " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.error("mcp-server executeDockerCommand interrupted operation={} container={}",
                    operation, container, ex);
            throw new IllegalArgumentException(operation + " interrupted");
        }
    }

    /**
     * Filtra linhas por texto literal quando o argumento contains é informado.
     */
    private List<String> applyContainsFilter(List<String> lines, String contains) {
        if (!StringUtils.hasText(contains)) {
            return new ArrayList<>(lines);
        }
        return lines.stream()
                .filter(line -> line.contains(contains))
                .toList();
    }
}
