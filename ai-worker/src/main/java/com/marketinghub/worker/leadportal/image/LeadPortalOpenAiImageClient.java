package com.marketinghub.worker.leadportal.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.creative.CreativeImageOptimizer;
import ImageGenerationPlan;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class LeadPortalOpenAiImageClient {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalOpenAiImageClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);
    private static final byte[] PNG_SIGNATURE =
            new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private final WebClient webClient;
    private final CreativeImageOptimizer imageOptimizer;
    private final ObjectMapper mapper;
    private final String defaultModel;
    private final boolean enabled;

    public LeadPortalOpenAiImageClient(
            WebClient.Builder builder,
            CreativeImageOptimizer imageOptimizer,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${openai.image-model:gpt-image-1}") String model) {
        this.imageOptimizer = imageOptimizer;
        this.defaultModel = model;
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
        return defaultModel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public CreativeImageOptimizer.OptimizedImage generateFromBase(byte[] baseImage, String prompt, ImageGenerationPlan plan) {
        if (!enabled) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }
        if (baseImage == null || baseImage.length == 0) {
            throw new IllegalArgumentException("Base image must not be empty");
        }

        log.info("Requesting lead-portal image variation with prompt: {}", prompt);

        byte[] normalized = normalizeBaseImage(baseImage);
        MultiValueMap<String, Object> multipartBody = buildMultipartBody(normalized, prompt, plan);

        ImageResponse response = webClient.post()
                .uri("/images/edits")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartBody))
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

    private byte[] normalizeBaseImage(byte[] baseImage) {
        if (looksLikePng(baseImage)) {
            return baseImage;
        }
        BufferedImage image = decodeImage(baseImage);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", output)) {
                throw new IllegalStateException("Failed to encode image as PNG");
            }
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to encode image as PNG", ex);
        }
    }

    private boolean looksLikePng(byte[] bytes) {
        if (bytes == null || bytes.length < PNG_SIGNATURE.length) {
            return false;
        }
        for (int index = 0; index < PNG_SIGNATURE.length; index++) {
            if (bytes[index] != PNG_SIGNATURE[index]) {
                return false;
            }
        }
        return true;
    }

    private BufferedImage decodeImage(byte[] bytes) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new IllegalArgumentException("Unsupported image format");
            }
            return image;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to decode base image", ex);
        }
    }

    private MultiValueMap<String, Object> buildMultipartBody(byte[] baseImage, String prompt, ImageGenerationPlan plan) {
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        String selectedModel = plan != null && plan.apiModel() != null ? plan.apiModel() : defaultModel;
        body.add("model", selectedModel);
        if (prompt != null && !prompt.isBlank()) {
            body.add("prompt", prompt);
        }
        if (plan != null) {
            if (plan.sizeLabel() != null) {
                body.add("size", plan.sizeLabel());
            }
            if (plan.apiQuality() != null && !plan.apiQuality().isBlank()) {
                body.add("quality", plan.apiQuality());
            }
        }

        ByteArrayResource imageResource = new ByteArrayResource(baseImage) {
            @Override
            public String getFilename() {
                return "source.png";
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDisposition(ContentDisposition.builder("form-data")
                .name("image")
                .filename(imageResource.getFilename())
                .build());
        HttpEntity<ByteArrayResource> imagePart = new HttpEntity<>(imageResource, headers);
        body.add("image", imagePart);

        return body;
    }

    private Mono<ImageResponse> readResponse(ClientResponse response) {
        return response.body(BodyExtractors.toDataBuffers())
                .map(this::toByteArray)
                .reduceWith(ByteArrayOutputStream::new, (acc, bytes) -> {
                    acc.write(bytes, 0, bytes.length);
                    return acc;
                })
                .map(ByteArrayOutputStream::toByteArray)
                .map(this::parseResponse)
                .map(parsed -> ensureSuccess(parsed, response));
    }

    private byte[] toByteArray(DataBuffer dataBuffer) {
        try {
            byte[] chunk = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(chunk);
            return chunk;
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
