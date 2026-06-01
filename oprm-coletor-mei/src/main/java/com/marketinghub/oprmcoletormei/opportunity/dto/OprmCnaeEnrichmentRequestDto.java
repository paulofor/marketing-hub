package com.marketinghub.oprmcoletormei.opportunity.dto;

import java.util.List;

/** DTO usado pelo OPRM para publicar enriquecimento e candidatos de nicho no backend. */
public record OprmCnaeEnrichmentRequestDto(
        String cnaeCode,
        String enrichmentCycleId,
        String routineSignals,
        String painSignals,
        String mechanismSignals,
        String proofSignals,
        String offerSignals,
        String sourceSummary,
        List<OprmNicheCandidateRequestDto> candidates) {}
