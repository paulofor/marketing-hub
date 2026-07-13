package com.marketinghub.salesvideo.service;

import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.dto.JobCompletionRequest;

/**
 * Sincroniza dominios externos interessados quando um render de SalesVideo e concluido.
 */
public interface SalesVideoCompletedRenderAssetSync {
    /** Propaga os dados finais do render para ativos comerciais associados ao job. */
    void syncCompletedRender(SalesVideoJob job,
                             JobCompletionRequest request,
                             Integer durationSeconds,
                             String resolution);
}
