package com.marketinghub.pde.transitionpause.v1;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Registra resultado humano ou sinal de segurança de uma sessão experimental. */
public record TransitionPauseEventRequest(
        @NotBlank @Size(max = 64) String participantId,
        @NotBlank @Size(max = 64) String sessionId,
        @NotBlank String eventType,
        @Min(0) @Max(10) Integer effortBefore,
        @Min(0) @Max(10) Integer effortAfter,
        @Min(0) @Max(600) Integer secondsUntilTaskStarted,
        Boolean firstStepCompleted,
        @Size(max = 500) String discomfortNote) {}
