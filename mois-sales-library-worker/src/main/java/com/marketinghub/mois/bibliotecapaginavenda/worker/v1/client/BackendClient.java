package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.client;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class BackendClient {
    private final RestClient restClient;
    public BackendClient(RestClient restClient) { this.restClient = restClient; }

    public ClaimResponse claim(ClaimRequest request) {
        log.info("MOIS sales-library worker calling backend claim endpoint. workspaceId={}, source={}", request.workspaceId(), request.source());
        ClaimResponse response = restClient.post()
                .uri("/api/mois/sales-library/jobs:claim")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ClaimResponse.class);
        log.info("MOIS sales-library worker claim response received. claimed={}, hasJob={}",
                response != null && response.claimed(),
                response != null && response.job() != null);
        return response;
    }
    public void complete(long jobId, CompleteRequest request) {
        log.info("MOIS sales-library worker calling backend complete endpoint. jobId={}, scoreTotal={}", jobId, request.scoreTotal());
        var entity = restClient.post()
                .uri("/api/mois/sales-library/jobs/{jobId}:complete", jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
        log.info("MOIS sales-library worker complete response received. jobId={}, status={}", jobId, entity.getStatusCode());
    }
    public void fail(long jobId, FailRequest request) {
        log.warn("MOIS sales-library worker calling backend fail endpoint. jobId={}, errorCategory={}, errorMessage={}",
                jobId, request.errorCategory(), request.errorMessage());
        var entity = restClient.post()
                .uri("/api/mois/sales-library/jobs/{jobId}:fail", jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
        log.info("MOIS sales-library worker fail response received. jobId={}, status={}", jobId, entity.getStatusCode());
    }
}
