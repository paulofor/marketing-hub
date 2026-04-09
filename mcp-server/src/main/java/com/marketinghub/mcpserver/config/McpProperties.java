package com.marketinghub.mcpserver.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mcp")
public record McpProperties(@NotBlank String serverName, @NotBlank String serverVersion) {
}
