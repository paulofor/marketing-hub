package com.marketinghub.nichocnae.routinesynthesizer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Unidade de trabalho da etapa seis contendo o ciclo e sinais extraídos para síntese. */
public record RoutineSynthesizerPending(
        Long researchCycleId,
        Long sourceNicheId,
        String cnaeCode,
        String cnaeDescription,
        String nicheName,
        BigDecimal sourceScore,
        String cycleStatus,
        Integer totalExtractedSignals,
        Instant startedAt,
        List<SignalForRoutineSynthesis> signals) {}
