package com.marketinghub.product.bigidea.v1.service.save;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/** Solicita uma nova revisão com controle de concorrência e motivo explícito. */
public record SaveBigIdeaRequest(
    @NotNull @Min(0) Integer expectedRevision,
    @NotNull @Valid BigIdeaContent content,
    @NotBlank @Size(max = 2000) String changeReason) {}
