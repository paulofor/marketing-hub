package com.marketinghub.worker.openai.core.openai;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.springframework.util.StringUtils;

/** Responsabilidade: impedir que clientes OpenAI usem URLs locais por engano em execução produtiva. */
public final class OpenAiBaseUrlGuard {

    public static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1";

    /** Impede instanciação porque a classe expõe apenas validações utilitárias de base URL. */
    private OpenAiBaseUrlGuard() {
    }

    /** Resolve a base URL efetiva da OpenAI, substituindo localhost por endpoint oficial quando não permitido. */
    public static String resolve(String configuredBaseUrl, boolean allowLocalBaseUrl) {
        String candidate = StringUtils.hasText(configuredBaseUrl) ? configuredBaseUrl.trim() : DEFAULT_OPENAI_BASE_URL;
        if (!allowLocalBaseUrl && isLocalBaseUrl(candidate)) {
            return DEFAULT_OPENAI_BASE_URL;
        }
        return candidate;
    }

    /** Indica se a base URL aponta para loopback/localhost e, portanto, depende de permissão explícita. */
    public static boolean isLocalBaseUrl(String configuredBaseUrl) {
        if (!StringUtils.hasText(configuredBaseUrl)) {
            return false;
        }
        try {
            URI uri = new URI(configuredBaseUrl.trim());
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                return false;
            }
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            return "localhost".equals(normalizedHost)
                    || "127.0.0.1".equals(normalizedHost)
                    || "0.0.0.0".equals(normalizedHost)
                    || "::1".equals(normalizedHost)
                    || "[::1]".equals(normalizedHost);
        } catch (URISyntaxException error) {
            return false;
        }
    }
}
