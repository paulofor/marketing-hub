package com.marketinghub.videomanagement.service;

import com.marketinghub.videomanagement.client.BackendVideoClient;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.exception.BackendIntegrationException;
import com.marketinghub.videomanagement.client.payload.JobHeartbeatPayload;
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
        onProgress(percent, status, message, null);
    }

    /** Envia progresso e evidência estruturada ao ledger canônico do backend. */
    @Override
    public void onProgress(Integer percent, SalesVideoStatus status, String message, String detailsJson) {
        SalesVideoJob updated = backendClient.reportProgress(
                jobId, new JobProgressPayload(percent, status, message, detailsJson));
        if (updated != null && updated.status() == SalesVideoStatus.VIDEO_FAILED) {
            throw new BackendIntegrationException(
                    "Backend interrompeu o job " + jobId + " após violação do gate financeiro");
        }
        backendClient.reportHeartbeat(jobId, new JobHeartbeatPayload(message, null));
    }
}
