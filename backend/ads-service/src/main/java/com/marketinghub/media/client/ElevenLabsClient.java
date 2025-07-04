package com.marketinghub.media.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Client for ElevenLabs API.
 */
@Component
public class ElevenLabsClient {
    private final WebClient webClient;

    public ElevenLabsClient(WebClient webClient, @Value("${elevenlabs.base-url:https://api.elevenlabs.io}") String baseUrl) {
        this.webClient = webClient.mutate().baseUrl(baseUrl).build();
    }

    public Mono<Map<String, Object>> createSpeech(String voiceId, Map<String, Object> request) {
        // TODO: auth header
        return webClient.post().uri("/v1/text-to-speech/{voiceId}", voiceId).bodyValue(request).retrieve().bodyToMono(Map.class);
    }
}
