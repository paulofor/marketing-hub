package com.marketinghub.pde.harness.v1;

import java.nio.file.Path;
import java.util.Objects;

/** Expõe a prova local de prontidão sem executar modelo ou revelar credenciais. */
public record PdeHarnessHealth(
    boolean ready,
    String codexVersion,
    String sdkVersion,
    Path codexHome,
    String platformFamily,
    String platformOs,
    String userAgent) {

  /** Valida os campos retornados pelo handshake oficial do App Server. */
  public PdeHarnessHealth {
    codexVersion = Objects.requireNonNull(codexVersion, "codexVersion");
    sdkVersion = Objects.requireNonNull(sdkVersion, "sdkVersion");
    codexHome = Objects.requireNonNull(codexHome, "codexHome");
    platformFamily = Objects.requireNonNull(platformFamily, "platformFamily");
    platformOs = Objects.requireNonNull(platformOs, "platformOs");
    userAgent = Objects.requireNonNull(userAgent, "userAgent");
  }
}
