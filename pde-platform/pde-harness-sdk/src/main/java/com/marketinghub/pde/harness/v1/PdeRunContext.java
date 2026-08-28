package com.marketinghub.pde.harness.v1;

import java.nio.file.Path;
import java.util.Objects;

/** Identifica e segrega o produto, cliente, missão e workspace de uma execução PDE. */
public record PdeRunContext(
    String productCode,
    String productVersion,
    String customerReference,
    String missionReference,
    Path workspace) {

  /** Valida os correlatores mínimos e normaliza o caminho da execução. */
  public PdeRunContext {
    productCode = requireText(productCode, "productCode");
    productVersion = requireText(productVersion, "productVersion");
    customerReference = requireText(customerReference, "customerReference");
    missionReference = requireText(missionReference, "missionReference");
    workspace = Objects.requireNonNull(workspace, "workspace").toAbsolutePath().normalize();
  }

  /** Valida um correlator textual obrigatório sem aceitar espaços vazios. */
  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " é obrigatório");
    }
    return value.trim();
  }
}
