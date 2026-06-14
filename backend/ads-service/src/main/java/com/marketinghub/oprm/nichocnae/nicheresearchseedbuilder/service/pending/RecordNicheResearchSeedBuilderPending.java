package com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.pending;

import java.math.BigDecimal;
import java.time.Instant;

/** Representa um ciclo CNAE aguardando geração de seed e frases de pesquisa da etapa dois. */
public record RecordNicheResearchSeedBuilderPending(
    Long researchCycleId,
    Long sourceNicheId,
    String cnaeCode,
    String cnaeDescription,
    String nicheName,
    BigDecimal sourceScore,
    Long meiVolume,
    String openAiModelCode,
    String openAiModelName,
    String triggerSource,
    String previousQualityStatus,
    String previousNextMoveCode,
    String previousNextMove,
    String previousLearningNotes,
    String status,
    Instant startedAt,
    Instant createdAt) {}
