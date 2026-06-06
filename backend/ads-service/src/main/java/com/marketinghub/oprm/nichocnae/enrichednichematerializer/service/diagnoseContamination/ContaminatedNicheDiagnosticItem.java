package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.diagnoseContamination;

import java.time.Instant;

/** Representa um ciclo ou perfil histórico com possível contaminação por linguagem de solução. */
public record ContaminatedNicheDiagnosticItem(
    String recordType,
    Long recordId,
    Long researchCycleId,
    Long marketNicheId,
    String matchedTerm,
    String originalNicheName,
    String neutralNicheName,
    String displayedName,
    String status,
    String recommendation,
    Instant createdAt) {}
