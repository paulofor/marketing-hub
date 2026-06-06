package com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.diagnoseContamination;

import java.time.Instant;
import java.util.List;

/** Resposta do diagnóstico que lista registros antigos potencialmente contaminados por solução. */
public record ContaminatedNicheDiagnosticResponse(
    Instant generatedAt,
    int totalCycles,
    int totalProfiles,
    List<ContaminatedNicheDiagnosticItem> items) {}
