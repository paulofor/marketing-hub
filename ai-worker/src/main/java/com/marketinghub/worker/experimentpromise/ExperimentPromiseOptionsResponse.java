package com.marketinghub.worker.experimentpromise;

import java.math.BigDecimal;
import java.util.List;

/** Representa o contrato de solicitação e resposta de opções de promessa no backend. */
public record ExperimentPromiseOptionsResponse(
        Long requestId,
        String status,
        String prompt,
        List<ExperimentPromiseOptionDto> options,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd) {
}
