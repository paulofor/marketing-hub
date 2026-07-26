package com.marketinghub.facebookadsworker.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Valida a serialização segura de objetos usados em logs estruturados.
 */
class JsonLogFormatterTest {

    /**
     * Garante que tokens e segredos de configuração Meta não aparecem em logs.
     */
    @Test
    void masksSensitiveFieldsBeforeWritingJsonLogValue() {
        Object wrapped = JsonLogFormatter.wrap(Map.of(
            "accessToken", "user-token-value",
            "systemUserAccessToken", "system-token-value",
            "appSecret", "secret-value",
            "nested", Map.of("access_token", "nested-token-value"),
            "name", "Produtividade 360"
        ));

        String rendered = wrapped.toString();

        assertTrue(rendered.contains("\"accessToken\":\"***\""));
        assertTrue(rendered.contains("\"systemUserAccessToken\":\"***\""));
        assertTrue(rendered.contains("\"appSecret\":\"***\""));
        assertTrue(rendered.contains("\"access_token\":\"***\""));
        assertTrue(rendered.contains("Produtividade 360"));
        assertFalse(rendered.contains("user-token-value"));
        assertFalse(rendered.contains("system-token-value"));
        assertFalse(rendered.contains("secret-value"));
        assertFalse(rendered.contains("nested-token-value"));
    }
}
