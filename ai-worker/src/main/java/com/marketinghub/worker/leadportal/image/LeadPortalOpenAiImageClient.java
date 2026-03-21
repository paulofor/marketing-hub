package com.marketinghub.worker.leadportal.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.creative.CreativeImageOptimizer;
import com.marketinghub.worker.imagegeneration.ImageGenerationPlan;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import org.springframework.http.HttpStatusCode;
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
    private static final Duration DEFAULT_BATCH_POLL_INTERVAL = Duration.ofMillis(500);
    private static final Duration DEFAULT_BATCH_TIMEOUT = Duration.ofMinutes(5);
    private static final String IMAGE_GENERATION_ENDPOINT = "/v1/images/generations";
    private static final String BATCH_COMPLETION_WINDOW = "24h";
    private static final String BATCH_FILE_NAME = "lead-portal-images.jsonl";
    private static final Set<String> TERMINAL_BATCH_STATUSES =
            Set.of("completed", "failed", "expired", "cancelled");

    private final WebClient webClient;
    private final CreativeImageOptimizer imageOptimizer;
    private final ObjectMapper mapper;
    private final String defaultModel;
    private final boolean enabled;
    private final Duration batchPollInterval;
    private final Duration batchTimeout;

    public LeadPortalOpenAiImageClient(
            WebClient.Builder builder,
            CreativeImageOptimizer imageOptimizer,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${openai.image-model:gpt-image-1}") String model,
            @Value("${openai.batch-poll-interval:PT0.5S}") Duration batchPollInterval,
            @Value("${openai.batch-timeout:PT5M}") Duration batchTimeout) {
        this.imageOptimizer = imageOptimizer;
        this.defaultModel = model;
        this.enabled = apiKey != null && !apiKey.isBlank();
        WebClient.Builder clientBuilder = builder.clone().baseUrl(baseUrl);
        if (enabled) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        this.webClient = clientBuilder.build();
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.batchPollInterval = normalizeDuration(batchPollInterval, DEFAULT_BATCH_POLL_INTERVAL);
        this.batchTimeout = normalizeDuration(batchTimeout, DEFAULT_BATCH_TIMEOUT);
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

    public Map<String, BatchGenerationResult> generatePromptBatch(List<BatchPromptRequest> requests) {
        if (!enabled) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }
        if (requests == null || requests.isEmpty()) {
            return Map.of();
        }

        StringBuilder builder = new StringBuilder();
        int totalRequests = 0;
        for (BatchPromptRequest request : requests) {
            if (request == null || request.customId() == null || request.customId().isBlank()) {
                continue;
            }
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("custom_id", request.customId());
            line.put("method", "POST");
            line.put("url", IMAGE_GENERATION_ENDPOINT);
            line.put("body", buildGenerationPayload(request.prompt(), request.plan()));
            try {
                builder.append(mapper.writeValueAsString(line)).append('\n');
                totalRequests++;
            } catch (IOException e) {
                throw new RuntimeException("Failed to serialize OpenAI image batch line for " + request.customId(), e);
            }
        }

        if (totalRequests == 0) {
            return Map.of();
        }

        log.info("Submitting {} prompt-only image requests via OpenAI batch", totalRequests);
        String inputFileId = uploadBatchFile(builder.toString().getBytes(StandardCharsets.UTF_8));
        OpenAiBatch batch = createBatch(inputFileId);
        OpenAiBatch completed = awaitCompletion(batch);
        if (completed.outputFileId() == null || completed.outputFileId().isBlank()) {
            throw new IllegalStateException("OpenAI image batch completed without output_file_id");
        }
        String outputContent = downloadBatchFile(completed.outputFileId());
        return parseBatchOutput(outputContent);
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

        return toOptimizedImage(response);
    }

    public CreativeImageOptimizer.OptimizedImage generateFromPrompt(String prompt, ImageGenerationPlan plan) {
        if (!enabled) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }

        log.info("Requesting lead-portal image generation from prompt only");

        ImageResponse response = webClient.post()
                .uri("/images/generations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildGenerationPayload(prompt, plan))
                .exchangeToMono(this::readResponse)
                .block(REQUEST_TIMEOUT);

        return toOptimizedImage(response);
    }

    private Map<String, Object> buildGenerationPayload(String prompt, ImageGenerationPlan plan) {
        Map<String, Object> payload = new LinkedHashMap<>();
        String selectedModel = plan != null && plan.apiModel() != null ? plan.apiModel() : defaultModel;
        payload.put("model", selectedModel);
        if (supportsResponseFormat(selectedModel)) {
            payload.put("response_format", "b64_json");
        }
        if (prompt != null && !prompt.isBlank()) {
            payload.put("prompt", prompt);
        }
        if (plan != null && plan.sizeLabel() != null) {
            payload.put("size", plan.sizeLabel());
        }
        return payload;
    }

    private byte[] normalizeBaseImage(byte[] baseImage) {
        BufferedImage image = decodeImage(baseImage);
        BufferedImage imageWithAlpha = ensureAlphaChannel(image);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(imageWithAlpha, "png", output)) {
                throw new IllegalStateException("Failed to encode image as PNG");
            }
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to encode image as PNG", ex);
        }
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

    private BufferedImage ensureAlphaChannel(BufferedImage image) {
        if (image.getColorModel().hasAlpha()) {
            return image;
        }
        BufferedImage withAlpha =
                new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = withAlpha.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return withAlpha;
    }

    private MultiValueMap<String, Object> buildMultipartBody(byte[] baseImage, String prompt, ImageGenerationPlan plan) {
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        String selectedModel = plan != null && plan.apiModel() != null ? plan.apiModel() : defaultModel;
        body.add("model", selectedModel);
        if (supportsResponseFormat(selectedModel)) {
            // We need the binary payload because the downstream optimizer expects it.
            // By default OpenAI returns URLs, so we explicitly request the base64 variant.
            body.add("response_format", "b64_json");
        }
        if (prompt != null && !prompt.isBlank()) {
            body.add("prompt", prompt);
        }
        if (plan != null && plan.sizeLabel() != null) {
            body.add("size", plan.sizeLabel());
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


    private CreativeImageOptimizer.OptimizedImage toOptimizedImage(ImageResponse response) {
        byte[] imageBytes = extractImagePayload(response);
        return imageOptimizer.optimize(imageBytes);
    }

    private byte[] extractImagePayload(ImageResponse response) {
        if (response == null || response.data == null || response.data.isEmpty()) {
            String errorMessage = response != null && response.error != null ? response.error.message : null;
            if (errorMessage != null && !errorMessage.isBlank()) {
                throw new IllegalStateException("OpenAI image API returned error: " + errorMessage);
            }
            throw new IllegalStateException("OpenAI image API did not return a payload");
        }
        ImageData data = response.data.get(0);
        if (data.base64 != null && !data.base64.isBlank()) {
            try {
                return Base64.getDecoder().decode(data.base64);
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("OpenAI image API returned invalid base64 payload", ex);
            }
        }
        if (data.url != null && !data.url.isBlank()) {
            return downloadFromUrl(data.url);
        }
        throw new IllegalStateException("OpenAI image API did not return a payload");
    }

    private byte[] downloadFromUrl(String url) {
        byte[] content = webClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchangeToMono(response -> {
                    if (response.statusCode().isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new ImageGenerationException(
                                        response.statusCode(),
                                        "Failed to download generated image from URL: " + url
                                                + (body.isBlank() ? "" : " (" + body + ")"))));
                    }
                    return response.bodyToMono(byte[].class);
                })
                .block(REQUEST_TIMEOUT);
        if (content == null || content.length == 0) {
            throw new IllegalStateException("OpenAI image API returned an empty URL payload");
        }
        return content;
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
        HttpStatusCode status = response.statusCode();
        if (status.isError()) {
            String errorMessage = parsed != null && parsed.error != null && parsed.error.message != null
                    ? parsed.error.message
                    : "OpenAI image API returned status " + status;
            throw new ImageGenerationException(status, errorMessage);
        }
        return parsed;
    }


    private String uploadBatchFile(byte[] payload) {
        ByteArrayResource resource = new ByteArrayResource(payload) {
            @Override
            public String getFilename() {
                return BATCH_FILE_NAME;
            }
        };
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("purpose", "batch");
        multipart.add("file", resource);
        OpenAiFile file = webClient.post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipart))
                .retrieve()
                .bodyToMono(OpenAiFile.class)
                .block(REQUEST_TIMEOUT);
        if (file == null || file.id() == null || file.id().isBlank()) {
            throw new IllegalStateException("OpenAI file upload failed for lead-portal image batch");
        }
        return file.id();
    }

    private OpenAiBatch createBatch(String inputFileId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("input_file_id", inputFileId);
        payload.put("endpoint", IMAGE_GENERATION_ENDPOINT);
        payload.put("completion_window", BATCH_COMPLETION_WINDOW);
        OpenAiBatch batch = webClient.post()
                .uri("/batches")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OpenAiBatch.class)
                .block(REQUEST_TIMEOUT);
        if (batch == null || batch.id() == null || batch.id().isBlank()) {
            throw new IllegalStateException("OpenAI image batch creation failed");
        }
        return batch;
    }

    private OpenAiBatch awaitCompletion(OpenAiBatch initial) {
        OpenAiBatch current = initial;
        Instant start = Instant.now();
        while (!isTerminal(current)) {
            if (Duration.between(start, Instant.now()).compareTo(batchTimeout) > 0) {
                throw new IllegalStateException("Timed out waiting for OpenAI image batch " + current.id());
            }
            try {
                Thread.sleep(batchPollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for OpenAI image batch", e);
            }
            current = webClient.get()
                    .uri("/batches/{id}", current.id())
                    .retrieve()
                    .bodyToMono(OpenAiBatch.class)
                    .block(REQUEST_TIMEOUT);
            if (current == null) {
                throw new IllegalStateException("OpenAI returned null while polling image batch");
            }
        }
        if (!"completed".equals(current.status())) {
            throw new RuntimeException("OpenAI image batch finished with status " + current.status());
        }
        return current;
    }

    private boolean isTerminal(OpenAiBatch batch) {
        if (batch == null || batch.status() == null) {
            return true;
        }
        return TERMINAL_BATCH_STATUSES.contains(batch.status());
    }

    private String downloadBatchFile(String fileId) {
        return webClient.get()
                .uri("/files/{id}/content", fileId)
                .retrieve()
                .bodyToMono(String.class)
                .block(REQUEST_TIMEOUT);
    }

    private Map<String, BatchGenerationResult> parseBatchOutput(String content) {
        if (content == null || content.isBlank()) {
            return Map.of();
        }
        Map<String, BatchGenerationResult> results = new LinkedHashMap<>();
        String[] lines = content.split(\"\n\");
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            try {
                BatchOutput output = mapper.readValue(line, BatchOutput.class);
                if (output.response() != null && output.response().isSuccessful()) {
                    Map<String, Object> body = output.response().body();
                    if (body == null) {
                        results.put(output.customId(), new BatchGenerationResult(
                                output.customId(), null, output.response().statusCode(), "Resposta vazia da OpenAI"));
                        continue;
                    }
                    ImageResponse imageResponse = mapper.convertValue(body, ImageResponse.class);
                    CreativeImageOptimizer.OptimizedImage optimized = toOptimizedImage(imageResponse);
                    results.put(output.customId(), new BatchGenerationResult(
                            output.customId(), optimized, output.response().statusCode(), null));
                } else if (output.response() != null) {
                    results.put(output.customId(), new BatchGenerationResult(
                            output.customId(),
                            null,
                            output.response().statusCode(),
                            extractErrorMessage(output.response().body())));
                } else if (output.error() != null) {
                    results.put(output.customId(), new BatchGenerationResult(
                            output.customId(), null, null, output.error().message()));
                }
            } catch (Exception ex) {
                log.error("Failed to parse OpenAI image batch output line: {}", line, ex);
            }
        }
        return Collections.unmodifiableMap(results);
    }

    private String extractErrorMessage(Map<String, Object> body) {
        if (body == null) {
            return "OpenAI retornou erro sem detalhes";
        }
        Object error = body.get("error");
        if (error instanceof Map<?, ?> errorMap) {
            Object message = ((Map<?, ?>) error).get("message");
            if (message != null) {
                return message.toString();
            }
        }
        Object message = body.get("message");
        return message != null ? message.toString() : "Falha desconhecida ao gerar imagem";
    }

    private Duration normalizeDuration(Duration candidate, Duration fallback) {
        if (candidate == null || candidate.isZero() || candidate.isNegative()) {
            return fallback;
        }
        return candidate;
    }

    public record BatchPromptRequest(String customId, String prompt, ImageGenerationPlan plan) {}

    public record BatchGenerationResult(
            String customId, CreativeImageOptimizer.OptimizedImage image, Integer statusCode, String errorMessage) {
        public boolean isSuccessful() {
            return image != null && (errorMessage == null || errorMessage.isBlank());
        }
    }

    private record BatchOutput(@JsonProperty("custom_id") String customId,
                               BatchResponse response,
                               BatchError error) {}

    private record BatchResponse(@JsonProperty("status_code") Integer statusCode,
                                 @JsonProperty("request_id") String requestId,
                                 Map<String, Object> body) {
        boolean isSuccessful() {
            return statusCode != null && statusCode >= 200 && statusCode < 300;
        }
    }

    private record BatchError(String code, String message, String param, String line) {}

    private record OpenAiBatch(String id, String status,
                               @JsonProperty("output_file_id") String outputFileId) {}

    private record OpenAiFile(String id) {}

    private boolean supportsResponseFormat(String selectedModel) {
        if (selectedModel == null || selectedModel.isBlank()) {
            return true;
        }
        return !selectedModel.toLowerCase(Locale.ROOT).startsWith("gpt-image-");
    }

    public static class ImageGenerationException extends RuntimeException {
        private final HttpStatusCode status;

        public ImageGenerationException(HttpStatusCode status, String message) {
            super(message);
            this.status = status;
        }

        public ImageGenerationException(HttpStatusCode status, String message, Throwable cause) {
            super(message, cause);
            this.status = status;
        }

        public HttpStatusCode getStatus() {
            return status;
        }
    }

    private record ImageResponse(List<ImageData> data, ApiError error) {}

    private record ImageData(@JsonProperty("b64_json") String base64, String url) {}

    private record ApiError(String message, String type, String param, String code) {}
}
