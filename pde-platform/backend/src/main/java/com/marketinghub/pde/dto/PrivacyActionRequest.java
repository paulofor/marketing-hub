package com.marketinghub.pde.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Recebe uma ação de direitos da titular autenticada pelo token do próprio acesso. */
public record PrivacyActionRequest(
        @NotBlank @Pattern(regexp = "ACCESS|CORRECTION|DELETION|OBJECTION") String action,
        @Size(max = 320) String correctedEmail
) {}
