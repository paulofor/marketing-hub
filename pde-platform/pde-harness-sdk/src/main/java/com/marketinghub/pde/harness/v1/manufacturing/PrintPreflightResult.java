package com.marketinghub.pde.harness.v1.manufacturing;

import java.util.List;

/**
 * Transporta a verificação técnica auditável sem aprovar fabricação, qualidade estética ou gasto.
 */
public record PrintPreflightResult(
    String scopeFingerprint,
    String artifactSha256,
    String specificationReference,
    String sku,
    int pixelWidth,
    int pixelHeight,
    double effectiveDpi,
    List<String> issues) {

  /** Congela os motivos para preservar o resultado encaminhado à persistência do backend. */
  public PrintPreflightResult {
    issues = List.copyOf(issues);
  }

  /** Informa somente se os controles locais do arquivo foram aprovados. */
  public boolean technicalCheckPassed() {
    return issues.isEmpty();
  }

  /** Identifica a versão do algoritmo que gerou a evidência. */
  public String contractVersion() {
    return "manufacturing-print-preflight-v1";
  }

  /** Impede interpretar preflight como autorização de compra ou prova física. */
  public boolean productionAuthorized() {
    return false;
  }
}
