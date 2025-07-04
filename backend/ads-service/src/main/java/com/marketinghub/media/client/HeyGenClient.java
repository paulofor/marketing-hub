package com.marketinghub.media.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Client for HeyGen API.
 */
@Component
public class HeyGenClient {
    private final WebClient webClient;

    public HeyGenClient(WebClient webClient, @Value("${heygen.base-url:https://api.heygen.com}") String baseUrl) {
        this.webClient = webClient.mutate().baseUrl(baseUrl).build();
    }

    public Mono<Map<String, Object>> createVideo(Map<String, Object> request) {
        // TODO: auth header
        return webClient.post().uri("/v2/video/generate").bodyValue(request).retrieve().bodyToMono(Map.class);
    }

    public Mono<Map<String, Object>> getVideo(String id) {
        return webClient.get().uri("/v2/video/{id}", id).retrieve().bodyToMono(Map.class);
    }
}
