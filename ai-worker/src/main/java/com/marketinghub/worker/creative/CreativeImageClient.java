package com.marketinghub.worker.creative;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Simple wrapper around the OpenAI images API for creative image generation.
 */
@Component
public class CreativeImageClient {
    private final WebClient webClient;
    private final String model;
    private static final Logger log = LoggerFactory.getLogger(CreativeImageClient.class);

    public CreativeImageClient(WebClient.Builder builder,
                               @Value("${openai.api-key:}") String apiKey,
                               @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                               @Value("${openai.image-model:dall-e-3}") String model) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.model = model;
    }

    public String generateImage(String prompt) {
        Map<String, Object> payload = Map.of(
                "model", model,
                "prompt", prompt
        );

        log.info("Sending image generation prompt to OpenAI: {}", prompt);
        ImageResponse response = webClient.post()
                .uri("/images/generations")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(ImageResponse.class)
                .block();

        log.info("OpenAI image response: {}", response);

        if (response == null || response.data().isEmpty()) {
            throw new RuntimeException("No image returned from OpenAI");
        }
        return response.data().get(0).url();
    }

    private record ImageResponse(List<ImageData> data) {}
    private record ImageData(String url) {}
}
