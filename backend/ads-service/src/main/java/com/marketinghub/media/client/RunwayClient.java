package com.marketinghub.media.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Client for Runway API.
 */
@Component
public class RunwayClient {
    private final WebClient webClient;

    public RunwayClient(WebClient webClient, @Value("${runway.base-url:https://api.runwayml.com}") String baseUrl) {
        this.webClient = webClient.mutate().baseUrl(baseUrl).build();
    }

    public Mono<Map<String, Object>> createClip(Map<String, Object> request) {
        // TODO: auth header
        return webClient.post().uri("/v1/generate").bodyValue(request).retrieve().bodyToMono(Map.class);
    }

    public Mono<Map<String, Object>> getJob(String id) {
        return webClient.get().uri("/v1/jobs/{id}", id).retrieve().bodyToMono(Map.class);
    }
}
