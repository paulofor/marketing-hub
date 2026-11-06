package com.marketinghub.salesvideo.service;

import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Define elegibilidade e limites de reprocessamento sem repetir contrato financeiro anterior. */
@Component
public class SalesVideoReprocessPolicy {
  private final Map<SalesVideoJobType, Integer> maxAttempts;

  /** Configura limites locais por tipo, sem autorizar uma nova geração externa. */
  public SalesVideoReprocessPolicy(
      @Value("${sales-video.reprocess.script-max-attempts:2}") int scriptMax,
      @Value("${sales-video.reprocess.storyboard-max-attempts:2}") int storyboardMax,
      @Value("${sales-video.reprocess.render-max-attempts:3}") int renderMax,
      @Value("${sales-video.reprocess.publish-max-attempts:1}") int publishMax) {
    maxAttempts = new EnumMap<>(SalesVideoJobType.class);
    maxAttempts.put(SalesVideoJobType.SCRIPT, scriptMax);
    maxAttempts.put(SalesVideoJobType.STORYBOARD, storyboardMax);
    maxAttempts.put(SalesVideoJobType.RENDER, renderMax);
    maxAttempts.put(SalesVideoJobType.RETRY, renderMax);
    maxAttempts.put(SalesVideoJobType.PUBLISH, publishMax);
  }

  /** Exige preflight novo para render Runway e aplica o limite dos demais reprocessamentos. */
  public void ensureRetryAllowed(SalesVideoJob job, Enum<?> reason) {
    if (requiresGovernedRender(job)) {
      throw VideoModuleException.conflict(
          VideoModuleErrorCode.JOB_RETRY_POLICY_BLOCKED,
          "Render Runway não pode repetir o contrato financeiro anterior. Conciliar a tentativa e reaproveitar resultado/callback; nova geração exige preflight e reserva vigentes pelo ciclo governado.");
    }
    int limit = resolveLimit(job.getJobType());
    if (job.getRetryAttempt() >= limit) {
      throw VideoModuleException.conflict(
          VideoModuleErrorCode.JOB_RETRY_POLICY_BLOCKED,
          String.format("Limite de %d tentativas atingido para o job %d", limit, job.getId()));
    }
  }

  /** Informa à UI a mesma elegibilidade aplicada pelo comando de reprocessamento. */
  public boolean hasAttemptsRemaining(SalesVideoJob job) {
    return !requiresGovernedRender(job) && job.getRetryAttempt() < resolveLimit(job.getJobType());
  }

  /** Distingue geração Runway paga de roteiro, publicação e acabamento de arquivo existente. */
  private boolean requiresGovernedRender(SalesVideoJob job) {
    return (job.getJobType() == SalesVideoJobType.RENDER
            || job.getJobType() == SalesVideoJobType.RETRY)
        && job.getProviderName() != null
        && job.getProviderName().toUpperCase(java.util.Locale.ROOT).startsWith("RUNWAY");
  }

  /** Obtém o limite configurado sem alterar o estado ou apagar histórico do job. */
  private int resolveLimit(SalesVideoJobType jobType) {
    return maxAttempts.getOrDefault(jobType, 1);
  }
}
