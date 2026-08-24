package com.marketinghub.pde.service;

/** Representa uma transação Pepper consultada no provedor antes de aplicar mudança financeira. */
public record PepperTransactionSnapshot(
        String transactionId,
        String buyerEmail,
        String paymentStatus,
        String offerHash,
        String offerTitle,
        Integer amount,
        String currency,
        String experienceVersion) {}
