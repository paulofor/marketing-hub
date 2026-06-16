package com.marketinghub.hypothesis.pain.service;

import jakarta.validation.constraints.NotBlank;

/** Contrato de entrada para fechar o pipeline auditável como hipótese disponível para experimento. */
public record FinalizeHypothesisRequest(
        @NotBlank(message = "O nome da hipótese é obrigatório")
        String name) {
}
