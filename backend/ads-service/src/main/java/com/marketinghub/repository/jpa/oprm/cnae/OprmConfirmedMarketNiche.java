package com.marketinghub.repository.jpa.oprm.cnae;

import java.math.BigDecimal;

/** Representa o nicho de mercado confirmado pelo OPRM sem expor entidades de outros módulos ao service OPRM. */
public record OprmConfirmedMarketNiche(Long id, String name, BigDecimal cost) {}
