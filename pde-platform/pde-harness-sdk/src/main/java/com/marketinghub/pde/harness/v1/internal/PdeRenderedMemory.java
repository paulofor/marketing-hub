package com.marketinghub.pde.harness.v1.internal;

import com.marketinghub.pde.harness.v1.PdeMemoryAudit;
import java.util.Objects;

/** Mantém juntos o contexto textual seguro e a auditoria sem conteúdo sensível. */
public record PdeRenderedMemory(String contextText, PdeMemoryAudit audit) {

  /** Exige que texto e auditoria sejam produzidos pela mesma renderização. */
  public PdeRenderedMemory {
    contextText = Objects.requireNonNull(contextText, "contextText");
    audit = Objects.requireNonNull(audit, "audit");
  }
}
