package com.marketinghub.repository.jpa.experiment.video;

/** Projeta somente os dados de reputação de provider necessários ao SalesVideo. */
public interface ExperimentVideoAssetProviderReviewProjection {
  /** Retorna o provider usado para gerar o ativo de vídeo. */
  String getProvider();

  /** Retorna o status funcional persistido do ativo de vídeo. */
  String getStatus();

  /** Retorna o status de revisão humana/comercial persistido do ativo de vídeo. */
  String getReviewStatus();
}
