package com.marketinghub.creative.service;

import com.marketinghub.creative.Creative;
import java.util.ArrayList;
import java.util.List;

/** Valida os limites canônicos da copy publicável sem alterar o conteúdo armazenado. */
public final class CreativePublicationCopyPolicy {
  /** Impede instanciação de uma política determinística sem estado. */
  private CreativePublicationCopyPolicy() {}

  /** Retorna todos os excessos em caracteres Unicode para explicar o bloqueio ao operador. */
  public static List<String> violations(Creative creative) {
    if (creative == null) {
      return List.of("Criativo ausente.");
    }
    List<String> violations = new ArrayList<>();
    check(violations, "Texto principal", creative.getPrimaryText(), 125);
    check(violations, "Título", creative.getHeadline(), 40);
    check(violations, "Descrição", creative.getDescription(), 25);
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
