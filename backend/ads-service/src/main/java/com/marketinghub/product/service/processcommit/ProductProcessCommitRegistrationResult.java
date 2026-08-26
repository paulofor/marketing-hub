package com.marketinghub.product.service.processcommit;

/** Resultado interno que distingue criação de repetição idempotente do mesmo vínculo. */
public record ProductProcessCommitRegistrationResult(
    ProductProcessCommitResponse commit, boolean created) {}
