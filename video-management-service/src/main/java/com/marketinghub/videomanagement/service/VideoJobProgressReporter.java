package com.marketinghub.videomanagement.service;

import com.marketinghub.videomanagement.client.BackendVideoClient;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.client.payload.JobProgressPayload;
import com.marketinghub.videomanagement.service.provider.ProgressCallback;

/**
 * Implementação de {@link ProgressCallback} que envia atualizações ao backend.
 */
public class VideoJobProgressReporter implements ProgressCallback {
    private final BackendVideoClient backendClient;
    private final long jobId;

    public VideoJobProgressReporter(BackendVideoClient backendClient,
                                    long jobId) {
        this.backendClient = backendClient;
        this.jobId = jobId;
    }

    @Override
    public void onProgress(Integer percent, SalesVideoStatus status, String message) {
        backendClient.reportProgress(jobId, new JobProgressPayload(percent, status, message, null));
    }
}
