package com.marketinghub.mds.client;

import com.marketinghub.mds.dto.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class BackendMdsClient {
    private final RestClient restClient;

    public BackendMdsClient(RestClient backendRestClient) {
        this.restClient = backendRestClient;
    }

    public List<BackendMdsRequestDto> getPendingRequests() {
        return restClient.get()
                .uri("/api/internal/mds/requests/pending")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public BackendMdsRequestDto claim(Long requestId, BackendClaimRequestDto claimRequestDto) {
        return restClient.post()
                .uri("/api/internal/mds/requests/{id}/claim", requestId)
                .body(claimRequestDto)
                .retrieve()
                .body(BackendMdsRequestDto.class);
    }

    public void heartbeat(Long requestId, BackendHeartbeatRequestDto heartbeatRequestDto) {
        restClient.post()
                .uri("/api/internal/mds/requests/{id}/heartbeat", requestId)
                .body(heartbeatRequestDto)
                .retrieve()
                .toBodilessEntity();
    }

    public void complete(Long requestId, BackendCompleteRequestDto completeRequestDto) {
        restClient.post()
                .uri("/api/internal/mds/requests/{id}/complete", requestId)
                .body(completeRequestDto)
                .retrieve()
                .toBodilessEntity();
    }

    public void fail(Long requestId, BackendFailRequestDto failRequestDto) {
        restClient.post()
                .uri("/api/internal/mds/requests/{id}/fail", requestId)
                .body(failRequestDto)
                .retrieve()
                .toBodilessEntity();
    }

    public BackendArtifactPublishBatchResponseDto publishBatch(BackendArtifactPublishBatchRequestDto requestDto) {
        return restClient.post()
                .uri("/api/internal/mds/artifacts/publish-batch")
                .body(requestDto)
                .retrieve()
                .body(BackendArtifactPublishBatchResponseDto.class);
    }
}
