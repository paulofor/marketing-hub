package com.marketinghub.pde.harness.v1;

import com.marketinghub.pde.harness.v1.internal.PdeHashing;

/** Identifica o relacionamento durável de um cliente dentro de um tenant e produto. */
public record PdeCustomerScope(
    String tenantReference, String productCode, String customerReference) {

  /** Valida e normaliza os três limites que impedem compartilhamento acidental de memória. */
  public PdeCustomerScope {
    tenantReference = requireText(tenantReference, "tenantReference");
    productCode = requireText(productCode, "productCode");
    customerReference = requireText(customerReference, "customerReference");
  }

  /** Calcula um identificador irreversível e estável para vínculo, workspace e auditoria. */
  public String fingerprint() {
    return PdeHashing.sha256(canonicalValue());
  }

  /** Produz a representação sem ambiguidade usada exclusivamente no cálculo do fingerprint. */
  String canonicalValue() {
    return part("tenant", tenantReference)
        + part("product", productCode)
        + part("customer", customerReference);
  }

  /** Codifica tamanho e valor para impedir colisões por concatenação de campos. */
  private static String part(String field, String value) {
    return field + ":" + value.length() + ":" + value + "\n";
  }

  /** Exige um identificador não vazio sem alterar maiúsculas, acentos ou conteúdo interno. */
  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " é obrigatório");
    }
    return value.trim();
  }
}
