package com.marketinghub.niche.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: expor os dados agregados de um nicho na listagem administrativa paginada. */
public interface MarketNicheListItemProjection {
    /** Retorna o identificador do nicho. */
    Long getId();

    /** Retorna o nome comercial do nicho. */
    String getName();

    /** Retorna a data de criação usada na ordenação da listagem. */
    Instant getCreatedAt();

    /** Retorna o custo operacional total já acumulado em reais. */
    BigDecimal getTotalCost();

    /** Retorna a quantidade de hipóteses geradas pelo pipeline de hipótese. */
    Long getPipelineHypothesesCount();

    /** Retorna a quantidade de experimentos vinculados ao nicho. */
    Long getExperimentsCount();
}
