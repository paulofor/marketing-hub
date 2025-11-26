package com.marketinghub.worker.leadportal.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.creative.CreativeImageOptimizer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class LeadPortalOpenAiImageClient {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalOpenAiImageClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

    private final WebClient webClient;
    private final CreativeImageOptimizer imageOptimizer;
    private final ObjectMapper mapper;
    private final String model;
    private final boolean enabled;

    public LeadPortalOpenAiImageClient(
            WebClient.Builder builder,
            CreativeImageOptimizer imageOptimizer,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${openai.image-model:gpt-image-1}") String model) {
        this.imageOptimizer = imageOptimizer;
        this.model = model;
        this.enabled = apiKey != null && !apiKey.isBlank();
        WebClient.Builder clientBuilder = builder.clone().baseUrl(baseUrl);
        if (enabled) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        this.webClient = clientBuilder.build();
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        if (!enabled) {
            log.warn("OpenAI API key not configured; lead-portal image generation will be skipped");
        }
    }

    public String getModel() {
        return model;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public CreativeImageOptimizer.OptimizedImage generateFromBase(byte[] baseImage, String prompt) {
        if (!enabled) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }
        if (baseImage == null || baseImage.length == 0) {
            throw new IllegalArgumentException("Base image must not be empty");
        }

        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("prompt", prompt);
        payload.put("image", Base64.getEncoder().encodeToString(baseImage));
        payload.put("response_format", "b64_json");

        log.info("Requesting lead-portal image variation with prompt: {}", prompt);
        ImageResponse response = webClient.post()
                .uri("/images/generations")
                .bodyValue(payload)
                .exchangeToMono(this::readResponse)
                .block(REQUEST_TIMEOUT);

        if (response == null || response.data == null || response.data.isEmpty()) {
            throw new IllegalStateException("OpenAI image API did not return a payload");
        }

        String base64 = response.data.get(0).base64;
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException("OpenAI image API returned an empty payload");
        }

        byte[] decoded = Base64.getDecoder().decode(base64);
        return imageOptimizer.optimize(decoded);
    }

    private Mono<ImageResponse> readResponse(ClientResponse response) {
        return response.body(BodyExtractors.toDataBuffers())
                .map(this::toByteArray)
                .reduceWith(ByteArrayOutputStream::new, (acc, bytes) -> {
                    try {
                        acc.write(bytes, 0, bytes.length);
                        return acc;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(ByteArrayOutputStream::toByteArray)
                .map(this::parseResponse)
                .map(parsed -> ensureSuccess(parsed, response));
    }

    private byte[] toByteArray(DataBuffer dataBuffer) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(chunk);
            output.write(chunk);
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read OpenAI image payload", e);
        } finally {
            DataBufferUtils.release(dataBuffer);
        }
    }

    private ImageResponse parseResponse(byte[] bytes) {
        try {
            return mapper.readValue(bytes, ImageResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to decode OpenAI image response", e);
        }
    }

    private ImageResponse ensureSuccess(ImageResponse parsed, ClientResponse response) {
        if (response.statusCode().isError()) {
            String errorMessage = parsed != null && parsed.error != null && parsed.error.message != null
                    ? parsed.error.message
                    : "OpenAI image API returned status " + response.statusCode();
            throw new RuntimeException(errorMessage);
        }
        return parsed;
    }

    private record ImageResponse(List<ImageData> data, ApiError error) {}

    private record ImageData(@JsonProperty("b64_json") String base64) {}

    private record ApiError(String message, String type, String param, String code) {}
}
