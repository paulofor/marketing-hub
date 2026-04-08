package com.marketinghub.worker.frameworkimage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class FrameworkImageOpenAiBatchClient {
    private static final Logger log = LoggerFactory.getLogger(FrameworkImageOpenAiBatchClient.class);
    private static final String IMAGE_GENERATION_ENDPOINT = "/v1/images/generations";
    private static final String BATCH_COMPLETION_WINDOW = "24h";
    private static final String BATCH_FILE_NAME = "framework-image-batch.jsonl";
    private static final int DEFAULT_MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Duration DEFAULT_BATCH_POLL_INTERVAL = Duration.ofMillis(500);
    private static final Duration DEFAULT_BATCH_TIMEOUT = Duration.ofMinutes(5);
    private static final Set<String> TERMINAL_BATCH_STATUSES =
            Set.of("completed", "failed", "expired", "cancelled");

    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final String defaultModel;
    private final boolean enabled;
    private final Duration batchPollInterval;
    private final Duration batchTimeout;

    public FrameworkImageOpenAiBatchClient(WebClient.Builder builder,
                                           @Value("${openai.api-key:}") String apiKey,
                                           @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                           @Value("${openai.image-model:gpt-image-1}") String defaultModel,
                                           @Value("${openai.batch-poll-interval:PT0.5S}") Duration batchPollInterval,
                                           @Value("${openai.batch-timeout:PT5M}") Duration batchTimeout) {
        this.defaultModel = defaultModel;
        this.enabled = StringUtils.hasText(apiKey);
        WebClient.Builder clientBuilder = builder.clone()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(DEFAULT_MAX_IN_MEMORY_SIZE));
        if (enabled) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        this.webClient = clientBuilder.build();
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.batchPollInterval = normalizeDuration(batchPollInterval, DEFAULT_BATCH_POLL_INTERVAL);
        this.batchTimeout = normalizeDuration(batchTimeout, DEFAULT_BATCH_TIMEOUT);
    }

    public Map<UUID, FrameworkImageBatchResult> generateBatch(List<FrameworkImageJobDto> jobs) {
        if (!enabled) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }
        if (jobs == null || jobs.isEmpty()) {
            return Map.of();
        }

        String batchContent = buildBatchFileContent(jobs);
        String inputFileId = uploadBatchFile(batchContent.getBytes(StandardCharsets.UTF_8));
        OpenAiBatch createdBatch = createBatch(inputFileId);
        OpenAiBatch completedBatch = awaitCompletion(createdBatch);
        if (!StringUtils.hasText(completedBatch.outputFileId())) {
            throw new IllegalStateException("OpenAI image batch completed without output_file_id");
        }

        String outputContent = downloadBatchOutput(completedBatch.outputFileId());
        Map<UUID, FrameworkImageBatchResult> results = parseBatchOutput(outputContent, completedBatch.id());
        log.info("Framework image OpenAI batch {} returned {} result(s)", completedBatch.id(), results.size());
        return results;
    }

    private String buildBatchFileContent(List<FrameworkImageJobDto> jobs) {
        StringBuilder builder = new StringBuilder();
        for (FrameworkImageJobDto job : jobs) {
            if (job == null || job.id() == null) {
                continue;
            }
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("custom_id", job.id().toString());
            line.put("method", "POST");
            line.put("url", IMAGE_GENERATION_ENDPOINT);
            line.put("body", buildGenerationPayload(job));
            try {
                builder.append(mapper.writeValueAsString(line)).append('\n');
            } catch (IOException e) {
                throw new RuntimeException("Failed to serialize OpenAI image batch line for job " + job.id(), e);
            }
        }
        return builder.toString();
    }

    private Map<String, Object> buildGenerationPayload(FrameworkImageJobDto job) {
        Map<String, Object> payload = new LinkedHashMap<>();
        String selectedModel = StringUtils.hasText(job.model()) ? job.model().trim() : defaultModel;
        payload.put("model", selectedModel);
        payload.put("prompt", job.prompt());
        if (supportsResponseFormat(selectedModel)) {
            payload.put("response_format", "b64_json");
        }
        return payload;
    }

    private String uploadBatchFile(byte[] content) {
        ByteArrayResource resource = new ByteArrayResource(content) {
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
                .block();

        if (file == null || !StringUtils.hasText(file.id())) {
            throw new IllegalStateException("OpenAI file upload failed for framework image batch");
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
                .block();

        if (batch == null || !StringUtils.hasText(batch.id())) {
            throw new IllegalStateException("OpenAI batch creation failed for framework image jobs");
        }
        return batch;
    }

    private OpenAiBatch awaitCompletion(OpenAiBatch initial) {
        OpenAiBatch current = initial;
        Instant deadline = Instant.now().plus(batchTimeout);
        while (current != null && !isTerminal(current.status())) {
            if (Instant.now().isAfter(deadline)) {
                throw new IllegalStateException("Timed out waiting for OpenAI image batch " + current.id()
                        + " in status " + current.status());
            }
            try {
                Thread.sleep(Math.max(50L, batchPollInterval.toMillis()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for OpenAI image batch", e);
            }
            current = webClient.get()
                    .uri("/batches/{id}", current.id())
                    .retrieve()
                    .bodyToMono(OpenAiBatch.class)
                    .block();
        }

        if (current == null) {
            throw new IllegalStateException("OpenAI returned null batch while polling framework image jobs");
        }
        if (!"completed".equalsIgnoreCase(current.status())) {
            throw new IllegalStateException("OpenAI image batch " + current.id() + " finished with status "
                    + current.status());
        }
        return current;
    }

    private String downloadBatchOutput(String fileId) {
        return webClient.get()
                .uri("/files/{id}/content", fileId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private Map<UUID, FrameworkImageBatchResult> parseBatchOutput(String outputContent, String batchId) {
        Map<UUID, FrameworkImageBatchResult> results = new LinkedHashMap<>();
        if (!StringUtils.hasText(outputContent)) {
            return results;
        }

        for (String line : outputContent.split("\\n")) {
            if (!StringUtils.hasText(line)) {
                continue;
            }
            try {
                BatchOutput output = mapper.readValue(line, BatchOutput.class);
                UUID jobId = parseJobId(output.customId());
                if (jobId == null) {
                    continue;
                }
                if (output.response() == null || !output.response().isSuccessful() || output.response().body() == null) {
                    String errorMessage = output.error() != null
                            ? output.error().message()
                            : "OpenAI batch response unavailable";
                    results.put(jobId, FrameworkImageBatchResult.failure(jobId, batchId, errorMessage));
                    continue;
                }

                ImageGenerationResponse response = mapper.convertValue(output.response().body(), ImageGenerationResponse.class);
                String model = StringUtils.hasText(response.model()) ? response.model() : defaultModel;
                String prompt = response.prompt();
                ImageData imageData = response.firstImageData();
                if (imageData == null) {
                    results.put(jobId, FrameworkImageBatchResult.failure(jobId, batchId,
                            "OpenAI batch response did not include image data"));
                    continue;
                }
                results.put(jobId, FrameworkImageBatchResult.success(
                        jobId,
                        batchId,
                        model,
                        prompt,
                        decodeBase64(imageData.base64()),
                        imageData.url()));
            } catch (Exception ex) {
                throw new RuntimeException("Failed to parse OpenAI image batch output line", ex);
            }
        }
        return results;
    }

    private UUID parseJobId(String customId) {
        if (!StringUtils.hasText(customId)) {
            return null;
        }
        try {
            return UUID.fromString(customId.trim());
        } catch (IllegalArgumentException ex) {
            log.warn("Skipping OpenAI batch item with invalid custom_id: {}", customId);
            return null;
        }
    }

    private boolean isTerminal(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        return TERMINAL_BATCH_STATUSES.contains(status.toLowerCase(Locale.ROOT));
    }

    private byte[] decodeBase64(String base64) {
        if (!StringUtils.hasText(base64)) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Failed to decode OpenAI image payload", ex);
        }
    }

    private Duration normalizeDuration(Duration candidate, Duration fallback) {
        if (candidate == null || candidate.isNegative() || candidate.isZero()) {
            return fallback;
        }
        return candidate;
    }

    private boolean supportsResponseFormat(String selectedModel) {
        if (!StringUtils.hasText(selectedModel)) {
            return true;
        }
        return !selectedModel.toLowerCase(Locale.ROOT).startsWith("gpt-image-");
    }

    public record FrameworkImageBatchResult(UUID jobId,
                                            String batchId,
                                            String model,
                                            String prompt,
                                            byte[] imageContent,
                                            String imageUrl,
                                            boolean success,
                                            String errorMessage) {
        public static FrameworkImageBatchResult success(UUID jobId,
                                                        String batchId,
                                                        String model,
                                                        String prompt,
                                                        byte[] imageContent,
                                                        String imageUrl) {
            return new FrameworkImageBatchResult(jobId, batchId, model, prompt, imageContent, imageUrl, true, null);
        }

        public static FrameworkImageBatchResult failure(UUID jobId, String batchId, String errorMessage) {
            return new FrameworkImageBatchResult(jobId, batchId, null, null, null, null, false, errorMessage);
        }
    }

    private record OpenAiFile(String id) {
    }

    private record OpenAiBatch(String id,
                               String status,
                               @JsonProperty("output_file_id") String outputFileId) {
    }

    private record BatchOutput(@JsonProperty("custom_id") String customId,
                               BatchResponse response,
                               BatchError error) {
    }

    private record BatchResponse(@JsonProperty("status_code") Integer statusCode,
                                 Map<String, Object> body) {
        boolean isSuccessful() {
            return statusCode != null && statusCode >= 200 && statusCode < 300;
        }
    }

    private record BatchError(String message) {
    }

    private record ImageGenerationResponse(String model,
                                           String prompt,
                                           List<ImageData> data) {
        ImageData firstImageData() {
            if (data == null || data.isEmpty()) {
                return null;
            }
            ImageData first = data.get(0);
            if (first == null) {
                return null;
            }
            if (!StringUtils.hasText(first.url()) && !StringUtils.hasText(first.base64())) {
                return null;
            }
            return first;
        }
    }

    private record ImageData(String url,
                             @JsonProperty("b64_json") String base64) {
    }
}
