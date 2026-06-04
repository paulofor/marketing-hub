package com.marketinghub.nichocnae.sourcesearcher;

/** Representa uma fonte candidata no payload de conclusão da etapa três enviado ao backend. */
public record SourceCandidateRequest(
        String sourceUrl,
        String sourceTitle,
        String sourceSnippet,
        String sourceDomain,
        String sourceGroup,
        Integer searchPosition,
        String status) {}
