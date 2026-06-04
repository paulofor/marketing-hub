package com.marketinghub.openai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Responsabilidade: resolver o token OpenAI a partir de variável de ambiente/propriedade ou arquivo seguro montado no host. */
@Component
public class OpenAiApiKeyResolver {
    private static final Logger log = LoggerFactory.getLogger(OpenAiApiKeyResolver.class);

    /** Resolve o token priorizando a propriedade explícita e usando o arquivo configurado como fallback operacional. */
    public String resolve(OpenAiProperties properties) {
        if (StringUtils.hasText(properties.getApiKey())) {
            return properties.getApiKey().trim();
        }
        if (!StringUtils.hasText(properties.getApiKeyFile())) {
            return null;
        }
        Path tokenPath = Path.of(properties.getApiKeyFile());
        if (!Files.isRegularFile(tokenPath)) {
            log.debug("Token OpenAI não encontrado no arquivo configurado; operation=openai-api-key-resolve path={}", tokenPath);
            return null;
        }
        try {
            return Files.readString(tokenPath, StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            log.error(
                    "Falha ao ler token OpenAI do arquivo configurado; operation=openai-api-key-resolve path={}",
                    tokenPath,
                    ex);
            return null;
        }
    }
}
