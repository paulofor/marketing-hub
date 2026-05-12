package com.marketinghub.mcpserver.service;

import com.marketinghub.mcpserver.config.McpProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ShellCommandService {

    private final McpProperties properties;

    public ShellCommandService(McpProperties properties) {
        this.properties = properties;
    }

    public Map<String, Object> execute(String command) {
        if (!properties.shell().enabled()) {
            throw new IllegalArgumentException("shell tools are disabled (set mcp.shell.enabled=true)");
        }
        String trimmed = command == null ? "" : command.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("command is required");
        }

        String firstToken = trimmed.split("\\s+")[0].toLowerCase(Locale.ROOT);
        if (properties.shell().allowedCommands().stream().noneMatch(cmd -> cmd.equalsIgnoreCase(firstToken))) {
            throw new IllegalArgumentException("command not allowed: " + firstToken);
        }

        ProcessBuilder pb = new ProcessBuilder("bash", "-lc", trimmed);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            boolean finished = process.waitFor(properties.shell().timeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalArgumentException("command timed out after " + properties.shell().timeoutSeconds() + "s");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (output.length() > properties.shell().maxOutputChars()) {
                output = output.substring(0, properties.shell().maxOutputChars()) + "\n... [truncated]";
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("command", trimmed);
            result.put("exitCode", process.exitValue());
            result.put("output", output);
            result.put("timeoutSeconds", properties.shell().timeoutSeconds());
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to execute command: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("command interrupted", e);
        }
    }
}
