package com.marketinghub.pde.transitionpause.v1;

import java.util.List;

/** Entrega a variante atribuída e suas instruções sem linguagem clínica ou coercitiva. */
public record TransitionPauseSessionResponse(
        String sessionId,
        String variant,
        int durationSeconds,
        List<String> steps,
        String exitInstruction) {}
