package com.marketinghub.videomanagement.service.provider;

import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;

/**
 * Callback usado pelos providers para reportar progresso intermediário.
 */
@FunctionalInterface
public interface ProgressCallback {
    void onProgress(Integer percent, SalesVideoStatus status, String message);
}
