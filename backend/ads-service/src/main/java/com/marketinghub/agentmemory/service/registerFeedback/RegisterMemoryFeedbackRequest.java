package com.marketinghub.agentmemory.service.registerFeedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Contrato de feedback oficial que confirma, contradiz ou retira uma memória. */
public record RegisterMemoryFeedbackRequest(
    @NotBlank @Pattern(regexp = "CONFIRMED|CONTRADICTED|INCONCLUSIVE|RETIRED") String outcome,
    @NotBlank @Size(max = 4000) String evidence,
    @Size(max = 700) String sourceReference) {}
