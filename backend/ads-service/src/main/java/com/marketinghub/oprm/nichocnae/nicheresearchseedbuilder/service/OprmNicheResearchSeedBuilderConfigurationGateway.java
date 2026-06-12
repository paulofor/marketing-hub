package com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service;

import java.math.BigDecimal;
import java.util.Optional;

/** Define o contrato OPRM para consultar configuração de IA e custo da etapa dois sem depender de pacotes externos. */
public interface OprmNicheResearchSeedBuilderConfigurationGateway {

  /** Recupera o modelo de IA configurado para a etapa dois do pipeline OPRM nicho CNAE. */
  Optional<OprmNicheResearchSeedBuilderModel> findConfiguredModel();

  /** Estima o custo da chamada de IA usada pela etapa dois a partir do modelo e dos tokens informados. */
  BigDecimal estimateCostUsd(String model, Integer inputTokens, Integer outputTokens);
}
