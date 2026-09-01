package com.marketinghub.repository.jpa.product;

/** Responsabilidade: projetar somente a identidade necessária ao resumo da cadeia do produto. */
public record ProductValueChainSummaryProduct(
    Long id, String name, String internalName, String commercialStatus) {}
