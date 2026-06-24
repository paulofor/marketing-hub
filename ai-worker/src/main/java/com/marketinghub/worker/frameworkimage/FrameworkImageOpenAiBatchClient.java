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

/**
 * Responsabilidade: gerar imagens do framework via OpenAI com chamadas diretas em modo Flex.
 */
@Component
public class FrameworkImageOpenAiBatchClient {
    private static final Logger log = LoggerFactory.getLogger(FrameworkImageOpenAiBatchClient.class);
    private static final String IMAGE_GENERATION_ENDPOINT = "/images/generations";
    private static final String FLEX_EXECUTION_ID = "flex";
    private static final int DEFAULT_MAX_IN_MEMORY_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB
    private static final Duration DEFAULT_BATCH_POLL_INTERVAL = Duration.ofMillis(500);
    private static final Duration DEFAULT_BATCH_TIMEOUT = Duration.ofMinutes(5);

    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final String defaultModel;
    private final boolean enabled;
    private final Duration batchPollInterval;
    private final Duration batchTimeout;
    private final int maxInMemorySizeBytes;

    public FrameworkImageOpenAiBatchClient(WebClient.Builder builder,
                                           @Value("${openai.api-key:}") String apiKey,
                                           @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                           @Value("${openai.image-model:gpt-image-2}") String defaultModel,
                                           @Value("${openai.max-in-memory-size-bytes:52428800}") int maxInMemorySizeBytes,
                                           @Value("${openai.batch-poll-interval:PT0.5S}") Duration batchPollInterval,
                                           @Value("${openai.batch-timeout:PT5M}") Duration batchTimeout) {
        this.defaultModel = defaultModel;
        this.enabled = StringUtils.hasText(apiKey);
        this.maxInMemorySizeBytes = normalizeMaxInMemorySize(maxInMemorySizeBytes);
        WebClient.Builder clientBuilder = builder.clone()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(this.maxInMemorySizeBytes));
        if (enabled) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        this.webClient = clientBuilder.build();
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.batchPollInterval = normalizeDuration(batchPollInterval, DEFAULT_BATCH_POLL_INTERVAL);
        this.batchTimeout = normalizeDuration(batchTimeout, DEFAULT_BATCH_TIMEOUT);
    }

    /** Gera imagens para os jobs informados sem usar a Batch API da OpenAI. */
    public Map<UUID, FrameworkImageBatchResult> generateBatch(List<FrameworkImageJobDto> jobs) {
        if (!enabled) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }
        if (jobs == null || jobs.isEmpty()) {
            return Map.of();
        }

        log.info("Framework image Flex start: preparing {} OpenAI request(s)", jobs.size());
        Map<UUID, FrameworkImageBatchResult> results = new LinkedHashMap<>();
        for (FrameworkImageJobDto job : jobs) {
            if (job == null || job.id() == null) {
                continue;
            }
            try {
                ImageGenerationResponse response = webClient.post()
                        .uri(IMAGE_GENERATION_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(buildGenerationPayload(job))
                        .retrieve()
                        .bodyToMono(ImageGenerationResponse.class)
                        .block();
                if (response == null) {
                    results.put(job.id(), FrameworkImageBatchResult.failure(job.id(), FLEX_EXECUTION_ID,
                            "OpenAI Flex response unavailable"));
                    continue;
                }
                String model = resolveModel(response.model());
                String prompt = response.prompt();
                ImageData imageData = response.firstImageData();
                if (imageData == null) {
                    results.put(job.id(), FrameworkImageBatchResult.failure(job.id(), FLEX_EXECUTION_ID,
                            "OpenAI Flex response did not include image data"));
                    continue;
                }
                results.put(job.id(), FrameworkImageBatchResult.success(
                        job.id(),
                        FLEX_EXECUTION_ID,
                        model,
                        prompt,
                        decodeBase64(imageData.base64()),
                        imageData.url()));
            } catch (RuntimeException ex) {
                log.error("Framework image Flex request failed for job {}", job.id(), ex);
                results.put(job.id(), FrameworkImageBatchResult.failure(job.id(), FLEX_EXECUTION_ID, ex.getMessage()));
            }
        }
        log.info("Framework image OpenAI Flex returned {} result(s)", results.size());
        return results;
    }

    /** Monta o payload da chamada direta de geração de imagem para a OpenAI. */
    private Map<String, Object> buildGenerationPayload(FrameworkImageJobDto job) {
        Map<String, Object> payload = new LinkedHashMap<>();
        String selectedModel = resolveModel(job.model());
        payload.put("model", selectedModel);
        payload.put("prompt", job.prompt());
        if (supportsResponseFormat(selectedModel)) {
            payload.put("response_format", "b64_json");
        }
        return payload;
    }

    private int normalizeMaxInMemorySize(int configuredValue) {
        if (configuredValue > 0) {
            return configuredValue;
        }
        log.warn("Invalid openai.max-in-memory-size-bytes value ({}). Falling back to {} bytes.",
                configuredValue, DEFAULT_MAX_IN_MEMORY_SIZE_BYTES);
        return DEFAULT_MAX_IN_MEMORY_SIZE_BYTES;
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

    private String resolveModel(String requestedModel) {
        String normalizedDefaultModel = StringUtils.hasText(defaultModel) ? defaultModel.trim() : "gpt-image-2";
        if (!StringUtils.hasText(requestedModel)) {
            return normalizedDefaultModel;
        }

        String trimmed = requestedModel.trim();
        if ("gpt-image-1".equalsIgnoreCase(trimmed) || "gpt-image-1.0".equalsIgnoreCase(trimmed)) {
            return normalizedDefaultModel;
        }
        return trimmed;
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
