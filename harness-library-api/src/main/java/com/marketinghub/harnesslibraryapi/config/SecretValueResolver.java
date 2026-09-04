package com.marketinghub.harnesslibraryapi.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/** Lê secrets diretos ou montados em arquivo sem registrar seu conteúdo. */
public final class SecretValueResolver {
  private static final Logger log = LoggerFactory.getLogger(SecretValueResolver.class);

  /** Impede instanciação de um utilitário sem estado. */
  private SecretValueResolver() {}

  /** Resolve um secret obrigatório e exige pelo menos 32 caracteres de entropia operacional. */
  public static byte[] resolve(String label, String directValue, String filePath) {
    String value = directValue;
    if (StringUtils.hasText(filePath)) {
      try {
        value = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
      } catch (IOException ex) {
        log.error("Falha ao ler secret obrigatório label={} file={}", label, filePath, ex);
        throw new IllegalStateException("Secret obrigatório não pode ser lido: " + label, ex);
      }
    }
    if (!StringUtils.hasText(value) || value.trim().length() < 32) {
      throw new IllegalStateException("Secret obrigatório ausente ou curto: " + label);
    }
    return value.trim().getBytes(StandardCharsets.UTF_8);
  }
}
