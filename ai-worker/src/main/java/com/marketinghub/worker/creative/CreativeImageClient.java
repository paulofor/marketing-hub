package com.marketinghub.worker.creative;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Simple wrapper around the OpenAI images API for creative image generation.
 */
@Component
public class CreativeImageClient {
    private final WebClient webClient;
    private final BackendAssetClient assetClient;
    private final String model;
    private static final Logger log = LoggerFactory.getLogger(CreativeImageClient.class);

    public CreativeImageClient(WebClient.Builder builder,
                               BackendAssetClient assetClient,
                               @Value("${openai.api-key:}") String apiKey,
                               @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                               @Value("${openai.image-model:dall-e-3}") String model) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.assetClient = assetClient;
        this.model = model;
    }

    public String generateImage(String prompt) {
        Map<String, Object> payload = Map.of(
                "model", model,
                "prompt", prompt,
                "response_format", "b64_json"
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
        ImageData data = response.data().get(0);
        if (data.base64() != null && !data.base64().isBlank()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(data.base64());
                String filename = "creative-" + UUID.randomUUID() + ".png";
                return assetClient.uploadImage(imageBytes, filename);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Failed to decode image payload", e);
            }
        }
        if (data.url() != null && !data.url().isBlank()) {
            log.warn("OpenAI image response missing base64 payload, using remote URL");
            return data.url();
        }
        throw new RuntimeException("No image returned from OpenAI");
    }

    private record ImageResponse(List<ImageData> data) {}
    private record ImageData(String url, @JsonProperty("b64_json") String base64) {}
}
