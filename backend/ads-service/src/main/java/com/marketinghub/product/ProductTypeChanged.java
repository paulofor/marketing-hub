package com.marketinghub.product;

/**
 * Responsabilidade: comunicar a mudança confirmada do vínculo de tipo dentro da mesma transação.
 */
public record ProductTypeChanged(Long productId, Long productTypeId) {}
