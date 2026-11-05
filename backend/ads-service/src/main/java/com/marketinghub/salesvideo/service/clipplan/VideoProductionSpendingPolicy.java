package com.marketinghub.salesvideo.service.clipplan;

import com.marketinghub.salesvideo.VideoProject;

/** Responsabilidade: consultar impedimentos financeiros antes de permitir consumo de vídeo. */
public interface VideoProductionSpendingPolicy {
  /** Retorna a causa persistida do bloqueio ou ausência de impedimento deste contrato. */
  String paidExecutionBlocker(VideoProject project);
}
