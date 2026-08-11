package com.marketinghub.experimentstrategistworker;

/** Responsabilidade: sinalizar expiração técnica que admite uma retomada auditável da lease. */
public class CodexActivityTimeoutException extends IllegalStateException {
  /** Cria a expiração recuperável sem classificá-la como parecer funcional. */
  public CodexActivityTimeoutException(String message) {
    super(message);
  }
}
