package com.marketinghub.worker.creative;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * Simple wrapper around the OpenAI images API for creative image generation.
 */
@Component
public class CreativeImageClient {
    private final WebClient webClient;
    private final BackendAssetClient assetClient;
    private final String model;
    private final CreativeImageOptimizer imageOptimizer;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private static final Logger log = LoggerFactory.getLogger(CreativeImageClient.class);
    private static final int DEFAULT_MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);

    public CreativeImageClient(WebClient.Builder builder,
                               BackendAssetClient assetClient,
                               CreativeImageOptimizer imageOptimizer,
                               @Value("${openai.api-key:}") String apiKey,
                               @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                               @Value("${openai.image-model:dall-e-3}") String model) {
        this.enabled = apiKey != null && !apiKey.isBlank();
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) CONNECT_TIMEOUT.toMillis())
                .responseTimeout(REQUEST_TIMEOUT)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler((int) REQUEST_TIMEOUT.getSeconds()))
                        .addHandlerLast(new WriteTimeoutHandler((int) REQUEST_TIMEOUT.getSeconds())));
        WebClient.Builder clientBuilder = builder.clone()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(DEFAULT_MAX_IN_MEMORY_SIZE));
        if (enabled) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        this.webClient = clientBuilder.build();
        this.assetClient = assetClient;
        this.imageOptimizer = imageOptimizer;
        this.model = model;
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        if (!enabled) {
            log.warn("OpenAI API key not configured; image generation will be skipped");
        }
    }

    public String generateImage(String prompt) {
        if (!enabled) {
            log.warn("Skipping image generation because OpenAI API key is missing");
            return null;
        }
        Map<String, Object> payload = Map.of(
                "model", model,
                "prompt", prompt,
                "response_format", "b64_json"
        );

        log.info("Sending image generation prompt to OpenAI: {}", prompt);
        ImageResponse response;
        try {
            response = webClient.post()
                    .uri("/images/generations")
                    .bodyValue(payload)
                    .exchangeToMono(this::readImageResponse)
                    .block(REQUEST_TIMEOUT);
        } catch (Exception ex) {
            log.error("OpenAI image generation failed after {} seconds", REQUEST_TIMEOUT.getSeconds(), ex);
            throw new RuntimeException("Failed to call OpenAI image API", ex);
        }

        log.info("OpenAI image response: {}", response);

        if (response == null || response.data().isEmpty()) {
            throw new RuntimeException("No image returned from OpenAI");
        }
        ImageData data = response.data().get(0);
        if (data.base64() != null && !data.base64().isBlank()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(data.base64());
                CreativeImageOptimizer.OptimizedImage optimized = imageOptimizer.optimize(imageBytes);
                if (log.isInfoEnabled()) {
                    log.info("Optimized OpenAI image from {} bytes to {} bytes", imageBytes.length,
                            optimized.content().length);
                }
                String filename = "creative-" + UUID.randomUUID() + "." + optimized.extension();
                return assetClient.uploadImage(optimized.content(), filename, model, prompt);
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

    private Mono<ImageResponse> readImageResponse(ClientResponse response) {
        return response.body(BodyExtractors.toDataBuffers())
                .map(this::toByteArray)
                .reduceWith(ByteArrayOutputStream::new, this::appendChunk)
                .map(ByteArrayOutputStream::toByteArray)
                .map(this::parseResponse);
    }

    private byte[] toByteArray(DataBuffer buffer) {
        try {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            return bytes;
        } finally {
            DataBufferUtils.release(buffer);
        }
    }

    private ByteArrayOutputStream appendChunk(ByteArrayOutputStream output, byte[] chunk) {
        if (output.size() + chunk.length > DEFAULT_MAX_IN_MEMORY_SIZE) {
            throw new IllegalStateException("OpenAI image payload exceeds allowed size");
        }
        output.write(chunk, 0, chunk.length);
        return output;
    }

    private ImageResponse parseResponse(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, ImageResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to decode image payload", e);
        }
    }

    private record ImageResponse(List<ImageData> data) {}
    private record ImageData(String url, @JsonProperty("b64_json") String base64) {}
}
