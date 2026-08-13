package com.marketinghub.videomanagement.service.provider;

import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;

/**
 * Callback usado pelos providers para reportar progresso intermediário.
 */
@FunctionalInterface
public interface ProgressCallback {
    /** Reporta progresso operacional sem detalhes estruturados adicionais. */
    void onProgress(Integer percent, SalesVideoStatus status, String message);

    /** Reporta progresso com evidência estruturada para contratos financeiros e de auditoria. */
    default void onProgress(Integer percent, SalesVideoStatus status, String message, String detailsJson) {
        onProgress(percent, status, message);
    }
}
