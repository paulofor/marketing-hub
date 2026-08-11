package com.marketinghub.landinggeneratoragent;

/** Responsabilidade: distinguir expiração recuperável por inatividade de uma falha funcional. */
public class CodexActivityTimeoutException extends IllegalStateException {
  /** Cria a falha técnica que deve preservar a lease para retomada controlada. */
  public CodexActivityTimeoutException(String message) {
    super(message);
  }
}
