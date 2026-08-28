package com.marketinghub.pde.harness.v1;

import java.util.Objects;

/** Resume a memória entregue ao modelo sem devolver seu conteúdo em telemetria técnica. */
public record PdeMemoryAudit(
    String customerScopeFingerprint,
    long memoryRevision,
    int deliveredEntryCount,
    String snapshotSha256,
    String contextTemplateVersion,
    String contextTemplateSha256) {

  /** Valida a evidência mínima necessária para reproduzir qual memória foi utilizada. */
  public PdeMemoryAudit {
    customerScopeFingerprint =
        Objects.requireNonNull(customerScopeFingerprint, "customerScopeFingerprint");
    if (memoryRevision < 0 || deliveredEntryCount < 0) {
      throw new IllegalArgumentException("revisão e quantidade de memória não podem ser negativas");
    }
    snapshotSha256 = Objects.requireNonNull(snapshotSha256, "snapshotSha256");
    contextTemplateVersion =
        Objects.requireNonNull(contextTemplateVersion, "contextTemplateVersion");
    contextTemplateSha256 = Objects.requireNonNull(contextTemplateSha256, "contextTemplateSha256");
  }
}
