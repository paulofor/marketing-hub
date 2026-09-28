package com.marketinghub.creative.service;

import com.marketinghub.creative.Creative;
import java.util.ArrayList;
import java.util.List;

/** Valida os limites canônicos da copy publicável sem alterar o conteúdo armazenado. */
public final class CreativePublicationCopyPolicy {
  public static final int PRIMARY_TEXT_MAX_LENGTH = 125;
  public static final int HEADLINE_MAX_LENGTH = 40;
  public static final int DESCRIPTION_MAX_LENGTH = 25;

  /** Impede instanciação de uma política determinística sem estado. */
  private CreativePublicationCopyPolicy() {}

  /** Retorna todos os excessos em caracteres Unicode para explicar o bloqueio ao operador. */
  public static List<String> violations(Creative creative) {
    if (creative == null) {
      return List.of("Criativo ausente.");
    }
    return violations(creative.getPrimaryText(), creative.getHeadline(), creative.getDescription());
  }

  /** Valida uma entrada ainda não persistida com a mesma contagem Unicode usada na aprovação. */
  public static List<String> violations(String primaryText, String headline, String description) {
    List<String> violations = new ArrayList<>();
    check(violations, "Texto principal", primaryText, PRIMARY_TEXT_MAX_LENGTH);
    check(violations, "Título", headline, HEADLINE_MAX_LENGTH);
    check(violations, "Descrição", description, DESCRIPTION_MAX_LENGTH);
    return List.copyOf(violations);
  }

  /** Conta caracteres completos, preservando espaços, quebras de linha e emojis. */
  private static void check(List<String> violations, String field, String value, int limit) {
    int length = value == null ? 0 : value.codePointCount(0, value.length());
    if (length > limit) {
      violations.add(field + ": " + length + "/" + limit + " caracteres.");
    }
  }
}
