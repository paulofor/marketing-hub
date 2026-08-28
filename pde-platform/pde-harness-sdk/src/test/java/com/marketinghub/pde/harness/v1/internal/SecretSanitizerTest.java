package com.marketinghub.pde.harness.v1.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Impede que stderr do processo leve tokens conhecidos para os logs do worker. */
class SecretSanitizerTest {

  /** Oculta bearer token e refresh token preservando o texto diagnóstico. */
  @Test
  void redactsKnownSecrets() {
    String sanitized =
        SecretSanitizer.sanitize(
            "falha Authorization: Bearer abc.def refresh_token=segredo "
                + "OPENAI_API_KEY=sk-nao-registrar causa=timeout");

    assertFalse(sanitized.contains("abc.def"));
    assertFalse(sanitized.contains("segredo"));
    assertFalse(sanitized.contains("sk-nao-registrar"));
    assertTrue(sanitized.contains("timeout"));
  }
}
