package com.marketinghub.pde.harness.v1.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.harness.v1.PdeHarnessException;
import com.marketinghub.pde.harness.v1.PdeHarnessFailureCategory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Calcula hashes determinísticos dos prompts, schemas e contratos usados pelo harness. */
public final class PdeHashing {
  private static final System.Logger LOGGER = System.getLogger(PdeHashing.class.getName());

  /** Impede instanciação de uma classe composta apenas por funções de hash. */
  private PdeHashing() {}

  /** Calcula SHA-256 de um texto exatamente como ele será entregue ao App Server. */
  public static String sha256(String value) {
    return sha256(value.getBytes(StandardCharsets.UTF_8));
  }

  /** Serializa um JSON sem indentação e calcula seu SHA-256. */
  public static String sha256(ObjectMapper mapper, JsonNode value) {
    try {
      return sha256(mapper.writeValueAsBytes(value));
    } catch (JsonProcessingException ex) {
      LOGGER.log(System.Logger.Level.ERROR, "Falha ao serializar JSON para calcular SHA-256", ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.CONFIGURATION,
          "Não foi possível serializar o schema de saída para auditoria",
          ex);
    }
  }

  /** Calcula SHA-256 de bytes preservando zeros à esquerda na representação hexadecimal. */
  public static String sha256(byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (NoSuchAlgorithmException ex) {
      LOGGER.log(System.Logger.Level.ERROR, "SHA-256 não está disponível na JVM", ex);
      throw new IllegalStateException("SHA-256 indisponível na JVM", ex);
    }
  }
}
