package com.marketinghub.oprm.nichocnae.routinesynthesizer.service.pending;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Contrato interno que entrega sinais suficientes para a etapa seis sintetizar o cartão de rotina. */
public record RecordRoutineSynthesizerPending(
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
