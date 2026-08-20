package com.marketinghub.geralanding.publiclanding.service;

/** Responsabilidade: proteger a publicação pública com a prova visual canônica do produto. */
public interface ApprovedLandingProductEvidenceGate {

  /** Valida se o HTML reutiliza a quantidade mínima de entregáveis aprovados do experimento. */
  void validateApprovedAssetReferences(Long experimentId, String html);
}
