package com.marketinghub.videomanagement.job;

import com.marketinghub.videomanagement.config.VideoManagementProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

/**
 * Cliente responsável por buscar jobs pendentes no backend.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoJobClient {
    private static final ParameterizedTypeReference<List<VideoJobSummary>> LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final VideoManagementProperties properties;

    public List<VideoJobSummary> fetchPendingJobs() {
        if (!properties.getJobs().isPollingEnabled()) {
            return Collections.emptyList();
        }
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/video/jobs")
                            .queryParam("status", "VIDEO_REQUESTED")
                            .queryParam("providerFamily", "EXTERNAL_VIDEO_MODULE")
                            .queryParam("limit", properties.getJobs().getBatchSize())
                            .build())
                    .retrieve()
                    .body(LIST_TYPE);
        } catch (Exception ex) {
            log.warn("Falha ao buscar jobs de vídeo: {}", ex.getMessage());
            log.debug("Erro completo ao buscar jobs", ex);
            return Collections.emptyList();
        }
    }
}
