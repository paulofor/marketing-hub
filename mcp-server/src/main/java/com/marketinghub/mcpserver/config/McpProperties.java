package com.marketinghub.mcpserver.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

/**
 * Centraliza as propriedades de configuração usadas pelo servidor MCP.
 */
@Validated
@ConfigurationProperties(prefix = "mcp")
public record McpProperties(
        @NotBlank String serverName,
        @NotBlank String serverVersion,
        @NotNull @Valid Logs logs,
        @NotNull @Valid ChatLogs chatLogs,
        @NotNull @Valid DockerOps dockerOps,
        @NotNull @Valid BuildInfo buildInfo,
        @NotNull @Valid VpsHostInventory vpsHostInventory,
        @NotNull @Valid ProductDiscoveryWorker productDiscoveryWorker,
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
            @NotBlank String opsMonitorWorkerPath,
            @NotBlank String pdePlatformBackendPath,
            @NotBlank String videoManagementServicePath,
            @NotBlank String customerAgentWorkerPath,
            @NotBlank String financialAgentWorkerPath,
            @NotBlank String experimentStrategistWorkerPath,
            @NotBlank String metaAdApproverWorkerPath,
            @NotBlank String themisImageStudioPath,
            @Positive int fetchTimeoutSeconds,
            @Positive int fetchAttempts,
            @Positive int fetchRetryDelayMillis,
            @Positive int maxLines,
            @Positive int httpTailRangeBytes
    ) {
    }

    /**
     * Define os limites da leitura de logs de containers de chat via Docker.
     */
    public record ChatLogs(
            boolean enabled,
            @NotEmpty List<@NotBlank String> allowedContainers,
            @NotBlank String dockerCommand,
            @Positive int maxLines,
            @Positive int timeoutSeconds
    ) {
    }

    /**
     * Define as operações Docker permitidas para diagnóstico operacional no host do MCP.
     */
    public record DockerOps(
            boolean enabled,
            @NotEmpty List<@NotBlank String> allowedContainers,
            @NotBlank String dockerCommand,
            @Positive int maxLines,
            @Positive int timeoutSeconds,
            boolean restartEnabled
    ) {
    }

    /**
     * Define os módulos e URLs permitidos para consulta de identidade de build em runtime.
     */
    public record BuildInfo(
            boolean enabled,
            @NotEmpty List<@NotBlank String> allowedModules,
            @NotEmpty Map<@NotBlank String, @NotBlank String> moduleInfoUrls,
            @Positive int timeoutSeconds
    ) {
    }

    /**
     * Define o acesso SSH restrito usado para inventário físico dos VPS permitidos.
     */
    public record VpsHostInventory(
            boolean enabled,
            @NotEmpty List<@NotBlank String> allowedHosts,
            @NotBlank String sshCommand,
            @NotBlank String user,
            @NotBlank String identityFile,
            @NotBlank String knownHostsFile,
            @Positive int timeoutSeconds,
            boolean backendRecoveryEnabled,
            @NotBlank String backendHost,
            @NotBlank String backendContainer,
            @NotBlank String backendHealthUrl,
            @Positive int backendRecoveryCooldownSeconds,
            @Positive int backendHealthAttempts,
            @Positive int backendHealthDelayMillis
    ) {
    }

    /**
     * Define como o MCP consulta o health operacional do Product Discovery Worker.
     */
    public record ProductDiscoveryWorker(
            boolean enabled,
            @NotBlank String container,
            @NotBlank String dockerCommand,
            @NotBlank String healthUrl,
            @Positive int timeoutSeconds
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
