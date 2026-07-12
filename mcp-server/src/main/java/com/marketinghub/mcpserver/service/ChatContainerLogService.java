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
 * Lê logs de containers de chat por Docker com escopo restrito aos containers permitidos.
 */
@Service
public class ChatContainerLogService {
    private static final Logger logger = LoggerFactory.getLogger(ChatContainerLogService.class);
    private static final int DEFAULT_LINES = 200;

    private final McpProperties properties;

    /**
     * Inicializa o serviço com a lista de containers permitidos e limites operacionais.
     */
    public ChatContainerLogService(McpProperties properties) {
        this.properties = properties;
    }

    /**
     * Retorna os containers de chat que podem ter logs consultados pelo MCP.
     */
    public List<String> allowedContainers() {
        return properties.chatLogs().allowedContainers();
    }

    /**
     * Retorna o limite máximo de linhas por chamada.
     */
    public int maxLines() {
        return properties.chatLogs().maxLines();
    }

    /**
     * Executa docker logs para o container permitido e aplica filtro textual opcional.
     */
    public Map<String, Object> readLogs(String container, Integer requestedLines, String contains) {
        if (!properties.chatLogs().enabled()) {
            throw new IllegalArgumentException("chat container logs are disabled");
        }

        String normalizedContainer = normalizeContainer(container);
        int lines = sanitizeLines(requestedLines);
        List<String> outputLines = executeDockerLogs(normalizedContainer, lines);
        List<String> filteredLines = applyContainsFilter(outputLines, contains);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("container", normalizedContainer);
        response.put("requestedLines", lines);
        response.put("returnedLines", filteredLines.size());
        response.put("contains", contains == null ? "" : contains);
        response.put("lines", filteredLines);
        return response;
    }

    /**
     * Valida se o container solicitado faz parte da lista operacional permitida.
     */
    private String normalizeContainer(String container) {
        if (!StringUtils.hasText(container)) {
            throw new IllegalArgumentException("container is required");
        }
        String normalized = container.trim();
        if (!properties.chatLogs().allowedContainers().contains(normalized)) {
            throw new IllegalArgumentException("container must be one of: "
                    + String.join(", ", properties.chatLogs().allowedContainers()));
        }
        return normalized;
    }

    /**
     * Ajusta a quantidade de linhas ao intervalo permitido pela configuração.
     */
    private int sanitizeLines(Integer requestedLines) {
        if (requestedLines == null) {
            return Math.min(DEFAULT_LINES, properties.chatLogs().maxLines());
        }
        if (requestedLines < 1 || requestedLines > properties.chatLogs().maxLines()) {
            throw new IllegalArgumentException("lines must be between 1 and " + properties.chatLogs().maxLines());
        }
        return requestedLines;
    }

    /**
     * Chama o Docker CLI local com timeout para evitar bloqueio do endpoint MCP.
     */
    private List<String> executeDockerLogs(String container, int lines) {
        List<String> command = List.of(
                properties.chatLogs().dockerCommand(),
                "logs",
                "--tail",
                String.valueOf(lines),
                "--timestamps",
                container
        );
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(properties.chatLogs().timeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalArgumentException("docker logs timed out after "
                        + Duration.ofSeconds(properties.chatLogs().timeoutSeconds()).toSeconds() + " seconds");
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            List<String> outputLines = output.isBlank() ? List.of() : output.lines().toList();
            if (process.exitValue() != 0) {
                throw new IllegalArgumentException("docker logs failed: " + String.join("\n", outputLines));
            }
            return outputLines;
        } catch (IOException ex) {
            logger.error("mcp-server executeDockerLogs failed to start docker command for container={}", container, ex);
            throw new IllegalArgumentException("failed to execute docker logs: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.error("mcp-server executeDockerLogs interrupted for container={}", container, ex);
            throw new IllegalArgumentException("docker logs interrupted");
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
