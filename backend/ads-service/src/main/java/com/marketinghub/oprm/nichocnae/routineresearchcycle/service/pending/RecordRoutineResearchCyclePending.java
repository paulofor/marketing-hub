package com.marketinghub.oprm.nichocnae.routineresearchcycle.service.pending;

import java.math.BigDecimal;
import java.time.Instant;

/** Representa uma unidade de trabalho fechada para iniciar/controlar a pesquisa de rotina do nicho CNAE. */
public record RecordRoutineResearchCyclePending(
    Long researchCycleId,
    Long sourceNicheId,
    String cnaeCode,
    String cnaeDescription,
    String nicheName,
    String originalNicheName,
    String neutralNicheName,
    String researchMode,
    BigDecimal solutionLanguageRiskScore,
    BigDecimal sourceScore,
    String triggerSource,
    String status,
    Instant startedAt,
    Instant createdAt) {}
