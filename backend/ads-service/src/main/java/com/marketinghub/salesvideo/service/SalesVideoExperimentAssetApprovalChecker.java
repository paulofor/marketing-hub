package com.marketinghub.salesvideo.service;

/**
 * Contrato de compliance para verificar se um asset originado de experimento pode ser publicado.
 */
public interface SalesVideoExperimentAssetApprovalChecker {
  /** Informa se o asset está liberado para publicação comercial no SalesVideo. */
  boolean isApprovedForPublication(Long assetId);

  /** Informa se todos os ativos do acabamento foram reprovados e permitem substituição. */
  boolean isRejectedForReplacement(Long salesVideoJobId);
}
