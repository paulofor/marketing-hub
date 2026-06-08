package com.marketinghub.mcpserver.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Centraliza as propriedades de configuração usadas pelo servidor MCP.
 */
@Validated
@ConfigurationProperties(prefix = "mcp")
public record McpProperties(
        @NotBlank String serverName,
        @NotBlank String serverVersion,
        @NotNull @Valid Logs logs,
        @NotNull @Valid Meta meta,
        @NotNull @Valid Github github
) {
    /**
     * Define os caminhos e limites usados para leitura de logs dos módulos Java.
     */
    public record Logs(
            @NotBlank String backendPath,
            @NotBlank String aiWorkerPath,
            @NotBlank String leadPortalPath,
            @NotBlank String facebookAdsPath,
            @NotBlank String emailServicePath,
            @NotBlank String leadPortalPaymentPath,
            @NotBlank String mdsPath,
            @NotBlank String moisPath,
            @NotBlank String moisSalesLibraryWorkerPath,
            @NotBlank String moisHotmartPath,
            @NotBlank String clickbankColetorMoisPath,
            @NotBlank String oprmColetorReceitaPath,
            @Positive int fetchTimeoutSeconds,
            @Positive int fetchAttempts,
            @Positive int fetchRetryDelayMillis,
            @Positive int maxLines,
            @Positive int httpTailRangeBytes
    ) {
    }

    /**
     * Define as credenciais e destinos permitidos para ferramentas Meta.
     */
    public record Meta(
            boolean enabled,
            @NotBlank String graphBaseUrl,
            @NotBlank String graphVersion,
            String accessToken,
            String debugAccessToken,
            @NotEmpty List<@NotBlank String> docsAllowedHosts
    ) {
    }

    /**
     * Define as credenciais e repositório usados pelas ferramentas GitHub Actions.
     */
    public record Github(
            boolean enabled,
            @NotBlank String apiBaseUrl,
            String owner,
            String repo,
            String token
    ) {
        /**
         * Valida que o owner do GitHub foi configurado quando as ferramentas GitHub estão ativas.
         */
        @jakarta.validation.constraints.AssertTrue(message = "mcp.github.owner must not be blank when mcp.github.enabled=true")
        private boolean isOwnerValid() {
            return !enabled || (owner != null && !owner.isBlank());
        }

        /**
         * Valida que o repositório do GitHub foi configurado quando as ferramentas GitHub estão ativas.
         */
        @jakarta.validation.constraints.AssertTrue(message = "mcp.github.repo must not be blank when mcp.github.enabled=true")
        private boolean isRepoValid() {
            return !enabled || (repo != null && !repo.isBlank());
        }
    }
}
