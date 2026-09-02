package com.marketinghub.repository.jpa.productdiscovery;

/** Responsabilidade: projetar o estado funcional mínimo de um ciclo na listagem independente. */
public interface ProductDiscoveryIndependentStatusProjection {
  /** Retorna o ciclo correlacionado à execução independente. */
  Long getCycleId();

  /** Retorna o estado persistido do ciclo de descoberta. */
  String getCycleStatus();

  /** Retorna quantas oportunidades do ciclo estão prontas para dossiê. */
  Long getReadyOpportunityCount();

  /** Retorna quantos produtos já foram materializados a partir do ciclo. */
  Long getProductCount();
}
