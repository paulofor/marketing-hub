package com.marketinghub.imagegeneration;

import org.springframework.util.StringUtils;

/** Responsabilidade: centralizar o modelo e a qualidade OpenAI homologados para novas imagens. */
public final class OpenAiImageGenerationPolicy {
  public static final String CANONICAL_MODEL = "gpt-image-2.5-sunburst";
  public static final String CANONICAL_DISPLAY_NAME = "GPT Image 2.5 Sunburst";
  public static final String CANONICAL_QUALITY = "high";

  /** Impede instanciação porque esta classe representa uma política estática. */
  private OpenAiImageGenerationPolicy() {}

  /** Confirma se o identificador recebido corresponde ao modelo visual homologado. */
  public static boolean isCanonicalModel(String value) {
    return StringUtils.hasText(value) && CANONICAL_MODEL.equalsIgnoreCase(value.trim());
  }

  /** Converte configuração vazia, antiga ou não homologada para o modelo visual canônico. */
  public static String normalizeToCanonicalModel(String value) {
    return CANONICAL_MODEL;
  }
}
