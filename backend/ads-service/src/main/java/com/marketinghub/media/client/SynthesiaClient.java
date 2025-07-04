package com.marketinghub.media.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Client for Synthesia API.
 */
@Component
public class SynthesiaClient {
    private final WebClient webClient;

    public SynthesiaClient(WebClient webClient, @Value("${synthesia.base-url:https://api.synthesia.io}") String baseUrl) {
        this.webClient = webClient.mutate().baseUrl(baseUrl).build();
    }

    /**
     * Create video.
     */
    public Mono<Map<String, Object>> createVideo(Map<String, Object> request) {
        // TODO: add authentication header
        return webClient.post().uri("/v2/videos").bodyValue(request).retrieve().bodyToMono(Map.class);
    }

    public Mono<Map<String, Object>> getVideo(String id) {
        return webClient.get().uri("/v2/videos/{id}", id).retrieve().bodyToMono(Map.class);
    }
}
