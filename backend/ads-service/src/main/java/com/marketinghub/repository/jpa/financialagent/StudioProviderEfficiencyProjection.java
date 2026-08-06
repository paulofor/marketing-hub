package com.marketinghub.repository.jpa.financialagent;

import java.math.BigDecimal;

/** Responsabilidade: projetar custos e aprovacoes comerciais do Estudio por provedor. */
public interface StudioProviderEfficiencyProjection {
  /** Retorna o provedor consolidado no ledger. */
  String getProvider();

  /** Retorna a quantidade total de tentativas cobraveis. */
  Long getTotalAttempts();

  /** Retorna quantas tentativas possuem custo conhecido. */
  Long getKnownCostAttempts();

  /** Retorna a soma dos custos conhecidos sem converter moeda. */
  BigDecimal getKnownCostUsd();

  /** Retorna a quantidade de assets submetidos a revisao comercial. */
  Long getReviewedAssets();

  /** Retorna a quantidade de assets aprovados comercialmente. */
  Long getApprovedAssets();

  /** Retorna a quantidade de assets ainda pendentes de revisao. */
  Long getPendingReviewAssets();
}
