package com.marketinghub.pde.dto;

/** Contrato para acionar manualmente a reconciliacao de compras Pepper no PDE. */
public record PepperSyncRequest(String productSlug, String search, String transactionHash) {}
