package com.marketinghub.nichocnae.enrichednichematerializer;

/** Campos complementares determinísticos enviados ao backend para o perfil de nicho enriquecido. */
public record EnrichedNicheProfileDraft(
        String personaSummary,
        String languagePatterns,
        String commercialTriggers,
        String objections) {}
