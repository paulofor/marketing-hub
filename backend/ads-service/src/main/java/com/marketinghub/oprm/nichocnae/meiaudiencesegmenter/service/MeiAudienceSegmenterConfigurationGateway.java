package com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service;

import java.util.Optional;

/** Define o contrato OPRM para consultar a configuração de IA da etapa de segmentação MEI/autônomo. */
public interface MeiAudienceSegmenterConfigurationGateway {

  /** Recupera o modelo de IA configurado para a etapa de segmentação MEI/autônomo do pipeline OPRM nicho CNAE. */
  Optional<MeiAudienceSegmenterModel> findConfiguredModel();
}
