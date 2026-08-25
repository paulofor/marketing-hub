package com.marketinghub.salesvideo;

/** Define o estado persistido de uma execução da análise automática de vídeo de referência. */
public enum VideoReferenceAnalysisStatus {
  QUEUED,
  RUNNING,
  COMPLETED,
  FAILED
}
