package com.marketinghub.pde.service;

/** Compra paga da Pepper ja filtrada para ser reconciliada com o acesso PDE. */
public record PepperPaidTransaction(
        String transactionId,
        String buyerEmail,
        String paymentStatus,
        String offerHash,
        String offerTitle,
        Integer amount,
        String currency,
        String experienceVersion) {}
