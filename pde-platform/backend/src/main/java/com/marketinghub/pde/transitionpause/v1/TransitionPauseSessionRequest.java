package com.marketinghub.pde.transitionpause.v1;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Recebe o consentimento e o contexto mínimo para iniciar uma sessão experimental. */
public record TransitionPauseSessionRequest(
        @NotBlank @Size(max = 64) String participantId,
        @NotBlank @Size(max = 64) String sessionId,
        @NotBlank @Size(max = 300) String taskDescription,
        @NotNull @AssertTrue Boolean consentAccepted,
        @NotNull @AssertTrue Boolean safetyAcknowledged,
        @NotNull @AssertTrue Boolean voluntaryParticipation) {}
