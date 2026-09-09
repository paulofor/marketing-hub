package com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.retry;

import jakarta.validation.constraints.*;

/**
 * Responsabilidade: solicitar nova tentativa para a revisão e proposta explicitamente indicadas.
 */
public record RetryDecisionProposal(
    @Min(0) long expectedRevision, @NotNull Long previousProposalId) {}
