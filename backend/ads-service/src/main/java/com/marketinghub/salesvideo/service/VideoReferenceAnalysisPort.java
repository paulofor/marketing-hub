package com.marketinghub.salesvideo.service;

import com.marketinghub.salesvideo.VideoReference;
import com.marketinghub.salesvideo.VideoReferenceAnalysisExecution;

/**
 * Define a porta estável entre o cadastro editorial de referências e a etapa versionada de análise.
 */
public interface VideoReferenceAnalysisPort {

  /** Enfileira uma referência recém-persistida usando um snapshot imutável de entrada. */
  VideoReferenceAnalysisExecution enqueue(VideoReference reference);

  /** Valida se a contingência manual está liberada para a referência. */
  void assertManualContingencyAllowed(Long referenceId);
}
