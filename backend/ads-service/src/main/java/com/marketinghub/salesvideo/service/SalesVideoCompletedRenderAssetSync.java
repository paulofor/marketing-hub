package com.marketinghub.salesvideo.service;

import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.dto.JobCompletionRequest;
import com.marketinghub.salesvideo.dto.JobFailureRequest;

/** Sincroniza dominios externos interessados quando um render de SalesVideo e concluido. */
public interface SalesVideoCompletedRenderAssetSync {
  /** Propaga os dados finais do render para ativos comerciais associados ao job. */
  void syncCompletedRender(
      SalesVideoJob job, JobCompletionRequest request, Integer durationSeconds, String resolution);

  /** Propaga falhas finais de render para ativos comerciais associados ao job. */
  void syncFailedRender(SalesVideoJob job, JobFailureRequest request);
}
