package com.marketinghub.pde.harness.v1;

import java.util.Objects;

/** Representa uma falha tipada do PDE Harness SDK sem ocultar sua causa técnica. */
public final class PdeHarnessException extends RuntimeException {
  private final PdeHarnessFailureCategory category;

  /** Cria uma falha do harness com categoria e mensagem acionáveis. */
  public PdeHarnessException(PdeHarnessFailureCategory category, String message) {
    super(message);
    this.category = Objects.requireNonNull(category, "category");
  }

  /** Cria uma falha do harness preservando a exceção original. */
  public PdeHarnessException(PdeHarnessFailureCategory category, String message, Throwable cause) {
    super(message, cause);
    this.category = Objects.requireNonNull(category, "category");
  }

  /** Retorna a categoria estável usada pelo worker no callback ao backend. */
  public PdeHarnessFailureCategory category() {
    return category;
  }
}
