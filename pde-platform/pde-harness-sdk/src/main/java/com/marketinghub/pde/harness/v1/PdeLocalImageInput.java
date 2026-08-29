package com.marketinghub.pde.harness.v1;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Representa uma imagem privada e auditável que será copiada para a interação segregada. */
public record PdeLocalImageInput(
    String reference, Path sourcePath, String mediaType, String sha256) {
  private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
  private static final Set<String> ALLOWED_MEDIA_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

  /** Valida referência, caminho, tipo e hash antes de qualquer leitura do arquivo. */
  public PdeLocalImageInput {
    reference = requireText(reference, "reference");
    if (reference.length() > 128) {
      throw new IllegalArgumentException("reference excede 128 caracteres");
    }
    sourcePath = Objects.requireNonNull(sourcePath, "sourcePath").toAbsolutePath().normalize();
    mediaType = requireText(mediaType, "mediaType").toLowerCase(Locale.ROOT);
    if (!ALLOWED_MEDIA_TYPES.contains(mediaType)) {
      throw new IllegalArgumentException("mediaType de imagem não permitido: " + mediaType);
    }
    sha256 = requireText(sha256, "sha256").toLowerCase(Locale.ROOT);
    if (!SHA_256.matcher(sha256).matches()) {
      throw new IllegalArgumentException("sha256 deve conter 64 caracteres hexadecimais");
    }
  }

  /** Devolve a extensão segura correspondente ao tipo validado. */
  public String safeExtension() {
    return switch (mediaType) {
      case "image/jpeg" -> ".jpg";
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      default -> throw new IllegalStateException("mediaType validado sem extensão segura");
    };
  }

  /** Confirma que os bytes possuem a assinatura do tipo declarado antes de copiá-los ao turno. */
  public boolean matchesMediaSignature(byte[] content) {
    Objects.requireNonNull(content, "content");
    return switch (mediaType) {
      case "image/jpeg" ->
          content.length >= 3
              && unsigned(content[0]) == 0xFF
              && unsigned(content[1]) == 0xD8
              && unsigned(content[2]) == 0xFF;
      case "image/png" ->
          content.length >= 8
              && unsigned(content[0]) == 0x89
              && unsigned(content[1]) == 0x50
              && unsigned(content[2]) == 0x4E
              && unsigned(content[3]) == 0x47
              && unsigned(content[4]) == 0x0D
              && unsigned(content[5]) == 0x0A
              && unsigned(content[6]) == 0x1A
              && unsigned(content[7]) == 0x0A;
      case "image/webp" ->
          content.length >= 12
              && unsigned(content[0]) == 0x52
              && unsigned(content[1]) == 0x49
              && unsigned(content[2]) == 0x46
              && unsigned(content[3]) == 0x46
              && unsigned(content[8]) == 0x57
              && unsigned(content[9]) == 0x45
              && unsigned(content[10]) == 0x42
              && unsigned(content[11]) == 0x50;
      default -> false;
    };
  }

  /** Converte byte assinado da JVM no valor binário usado pelas assinaturas de arquivos. */
  private int unsigned(byte value) {
    return Byte.toUnsignedInt(value);
  }

  /** Valida texto obrigatório e remove espaços externos. */
  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " é obrigatório");
    }
    return value.trim();
  }
}
