package com.marketinghub.experiment.directrecruitment.v1.service.pause;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Responsabilidade: registrar o operador e a causa da pausa do convite. */
public record PauseDirectRecruitmentRequest(
    @NotBlank @Size(max = 100) String pausedBy, @NotBlank @Size(max = 500) String reason) {}
