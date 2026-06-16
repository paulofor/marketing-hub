package com.marketinghub.hypothesis.service.finalizeHypothesis;

import jakarta.validation.constraints.NotBlank;

/** Contrato de entrada da etapa de fechamento que materializa o framework validado como hipótese. */
public record FinalizeHypothesisRequest(
        @NotBlank(message = "O nome da hipótese é obrigatório")
        String name) {
}
