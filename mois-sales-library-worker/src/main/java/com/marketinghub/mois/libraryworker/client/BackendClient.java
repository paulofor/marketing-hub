package com.marketinghub.mois.libraryworker.client;

import com.marketinghub.mois.libraryworker.model.WorkerDtos.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BackendClient {
    private final RestClient restClient;
    public BackendClient(RestClient restClient) { this.restClient = restClient; }

    public ClaimResponse claim(ClaimRequest request) {
        return restClient.post().uri("/api/mois/sales-library/jobs:claim").contentType(MediaType.APPLICATION_JSON).body(request).retrieve().body(ClaimResponse.class);
    }
    public void complete(long jobId, CompleteRequest request) {
        restClient.post().uri("/api/mois/sales-library/jobs/{jobId}:complete", jobId).contentType(MediaType.APPLICATION_JSON).body(request).retrieve().toBodilessEntity();
    }
    public void fail(long jobId, FailRequest request) {
        restClient.post().uri("/api/mois/sales-library/jobs/{jobId}:fail", jobId).contentType(MediaType.APPLICATION_JSON).body(request).retrieve().toBodilessEntity();
    }
}
