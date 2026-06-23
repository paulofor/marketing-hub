package com.marketinghub.hypothesis.pain.service.pending;

/** Resumo de uma hipótese já gerada para o mesmo nicho usado para evitar repetição no prompt. */
public record HypothesisPainPendingExistingHypothesis(
        String id,
        String title,
        String problem,
        String promise,
        String persona,
        String mechanism,
        String uniqueMechanism,
        String entrega,
        String status
) {
}
