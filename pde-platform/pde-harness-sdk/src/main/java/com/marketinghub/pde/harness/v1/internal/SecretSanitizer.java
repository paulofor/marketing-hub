package com.marketinghub.pde.harness.v1.internal;

import java.util.regex.Pattern;

/** Remove credenciais conhecidas antes que mensagens do processo sejam registradas. */
public final class SecretSanitizer {
  private static final Pattern NAMED_SECRET =
      Pattern.compile(
          "(?i)(access_token|refresh_token|id_token|authorization|cookie|openai_api_key|openai_api_key_file)([\\\"'\\s:=]+)([^\\\"'\\s,}]+)");
  private static final Pattern BEARER = Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9._\\-]+");

  /** Impede instanciação de uma classe composta apenas por funções puras. */
  private SecretSanitizer() {}

  /** Substitui tokens e cabeçalhos por marcadores seguros sem ocultar o restante da causa. */
  public static String sanitize(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    String bearerRedacted = BEARER.matcher(value).replaceAll("Bearer [redacted]");
    return NAMED_SECRET.matcher(bearerRedacted).replaceAll("$1$2[redacted]");
  }
}
