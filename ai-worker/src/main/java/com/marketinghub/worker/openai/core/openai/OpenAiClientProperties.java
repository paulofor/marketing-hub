package com.marketinghub.worker.openai.core.openai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Responsabilidade: centralizar credenciais, modelo, catálogo de preços e URL efetiva dos clientes OpenAI. */
@Validated
@ConfigurationProperties(prefix = "openai")
public record OpenAiClientProperties(
        @NotBlank
        String apiKey,

        String apiKeyFile,

        @NotBlank
        String baseUrl,

        @NotBlank
        String model,

        @NotNull
        Duration timeout,

        @NotBlank
        String pricingCatalogUrl,

        boolean allowLocalBaseUrl
) {
    private static final Logger log = LoggerFactory.getLogger(OpenAiClientProperties.class);
    private static final Set<String> INVALID_PLACEHOLDER_KEYS = Set.of("test-key", "changeme", "change-me", "dummy", "placeholder");

    /** Mantém compatibilidade com testes e configurações que ainda não informam arquivo de token. */
    public OpenAiClientProperties(
            String apiKey,
            String baseUrl,
            String model,
            Duration timeout,
            String pricingCatalogUrl,
            boolean allowLocalBaseUrl) {
        this(apiKey, null, baseUrl, model, timeout, pricingCatalogUrl, allowLocalBaseUrl);
    }

    /** Normaliza valores opcionais, resolve token por arquivo seguro e bloqueia placeholders em chamadas reais. */
    public OpenAiClientProperties {
        baseUrl = OpenAiBaseUrlGuard.resolve(baseUrl, allowLocalBaseUrl);
        if (timeout == null) {
            timeout = Duration.ofMinutes(30);
        }
        if (pricingCatalogUrl == null || pricingCatalogUrl.isBlank()) {
            pricingCatalogUrl = "http://191.252.181.168/api/modelos/openai/catalogo/v1/modelos";
        }
        apiKeyFile = normalize(apiKeyFile);
        apiKey = resolveApiKey(apiKey, apiKeyFile, allowLocalBaseUrl);
    }

    /** Resolve o token priorizando valor real explícito e usando arquivo seguro quando o valor estiver ausente ou for placeholder. */
    private static String resolveApiKey(String configuredApiKey, String configuredApiKeyFile, boolean allowLocalBaseUrl) {
        String normalizedApiKey = normalize(configuredApiKey);
        if (hasUsableApiKey(normalizedApiKey)) {
            return normalizedApiKey;
        }
        String fileApiKey = readApiKeyFile(configuredApiKeyFile);
        if (hasUsableApiKey(fileApiKey)) {
            return fileApiKey;
        }
        if (allowLocalBaseUrl && normalizedApiKey != null) {
            return normalizedApiKey;
        }
        throw new IllegalStateException(
                "OPENAI_API_KEY inválida ou ausente no Worker AI; configure OPENAI_API_KEY com token real ou OPENAI_API_KEY_FILE apontando para o arquivo seguro.");
    }

    /** Lê o token OpenAI montado como arquivo seguro quando disponível. */
    private static String readApiKeyFile(String configuredApiKeyFile) {
        if (configuredApiKeyFile == null) {
            return null;
        }
        Path tokenPath = Path.of(configuredApiKeyFile);
        if (!Files.isRegularFile(tokenPath)) {
            log.debug("Arquivo de token OpenAI não encontrado no Worker AI; operation=openai-api-key-resolve path={}", tokenPath);
            return null;
        }
        try {
            return normalize(Files.readString(tokenPath, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            log.error(
                    "Falha ao ler token OpenAI do arquivo configurado no Worker AI; operation=openai-api-key-resolve path={}",
                    tokenPath,
                    ex);
            return null;
        }
    }

    /** Indica se a chave informada parece ser uma credencial real, e não um valor de teste. */
    private static boolean hasUsableApiKey(String value) {
        return value != null && !INVALID_PLACEHOLDER_KEYS.contains(value.toLowerCase());
    }

    /** Normaliza textos de configuração preservando nulo quando não há conteúdo. */
    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
