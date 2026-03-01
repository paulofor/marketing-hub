package com.marketinghub.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OpenAiBatchClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiBatchClient.class);
    private static final String RESPONSES_ENDPOINT = "/v1/responses";
    private static final Set<String> TERMINAL_STATUSES = Set.of("completed", "failed", "cancelled", "expired");

    private final WebClient webClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiBatchClient(@Qualifier("openAiWebClient") WebClient webClient,
                             OpenAiProperties properties,
                             ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public OpenAiResponse executeSingle(Map<String, Object> body, String customId) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("OpenAI API key not configured");
        }
        Map<String, RequestContext> contexts = Map.of(customId, new RequestContext(customId, body));
        Map<String, OpenAiResponse> responses = executeBatch(contexts);
        OpenAiResponse response = responses.get(customId);
        if (response == null) {
            throw new IllegalStateException("OpenAI batch did not return a response for " + customId);
        }
        if (response.hasError()) {
            throw new IllegalStateException("OpenAI returned an error: " + response.errorMessage());
        }
        return response;
    }

    private Map<String, OpenAiResponse> executeBatch(Map<String, RequestContext> contexts) {
        String fileId = uploadBatchFile(contexts);
        OpenAiBatch batch = createBatch(fileId);
        OpenAiBatch completed = awaitCompletion(batch);
        if (completed.outputFileId() == null || completed.outputFileId().isBlank()) {
            throw new IllegalStateException("OpenAI batch completed without output file");
        }
        String content = downloadFile(completed.outputFileId());
        return parseBatchOutput(content);
    }

    private String uploadBatchFile(Map<String, RequestContext> contexts) {
        StringBuilder builder = new StringBuilder();
        contexts.forEach((customId, ctx) -> {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("custom_id", customId);
            line.put("method", "POST");
            line.put("url", RESPONSES_ENDPOINT);
            line.put("body", ctx.payload());
            try {
                builder.append(objectMapper.writeValueAsString(line)).append("\n");
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize batch line for " + customId, e);
            }
        });

        byte[] payload = builder.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(payload) {
            @Override
            public String getFilename() {
                return "lead-portal-styles.jsonl";
            }
        };

        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("purpose", "batch");
        multipart.add("file", resource);

        return webClient.post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipart))
                .retrieve()
                .bodyToMono(OpenAiFile.class)
                .map(OpenAiFile::id)
                .block();
    }

    private OpenAiBatch createBatch(String inputFileId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("input_file_id", inputFileId);
        payload.put("endpoint", RESPONSES_ENDPOINT);
        payload.put("completion_window", properties.getBatchCompletionWindow());
        OpenAiBatch batch = webClient.post()
                .uri("/batches")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OpenAiBatch.class)
                .block();
        if (batch == null || batch.id() == null) {
            throw new IllegalStateException("Failed to create OpenAI batch");
        }
        return batch;
    }

    private OpenAiBatch awaitCompletion(OpenAiBatch initial) {
        OpenAiBatch current = initial;
        Instant start = Instant.now();
        while (!isTerminal(current)) {
            if (Duration.between(start, Instant.now()).compareTo(properties.getBatchTimeout()) > 0) {
                throw new IllegalStateException("Timed out waiting for OpenAI batch " + current.id());
            }
            try {
                Thread.sleep(properties.getBatchPollInterval().toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while polling OpenAI batch", e);
            }
            current = webClient.get()
                    .uri("/batches/{id}", current.id())
                    .retrieve()
                    .bodyToMono(OpenAiBatch.class)
                    .block();
            if (current == null) {
                throw new IllegalStateException("OpenAI returned null batch while polling");
            }
        }
        if (!"completed".equals(current.status())) {
            throw new IllegalStateException("OpenAI batch finished with status " + current.status());
        }
        return current;
    }

    private String downloadFile(String fileId) {
        return webClient.get()
                .uri("/files/{id}/content", fileId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private Map<String, OpenAiResponse> parseBatchOutput(String content) {
        Map<String, OpenAiResponse> responses = new LinkedHashMap<>();
        if (content == null || content.isBlank()) {
            return responses;
        }
        for (String line : content.split("\\n")) {
            if (line.isBlank()) {
                continue;
            }
            try {
                BatchOutput output = objectMapper.readValue(line, BatchOutput.class);
                if (output.response() != null && output.response().isSuccessful()) {
                    OpenAiResponse response = objectMapper.convertValue(output.response().body(), OpenAiResponse.class);
                    responses.put(output.customId(), response);
                } else if (output.response() != null) {
                    log.error("OpenAI batch request {} failed with status {}", output.customId(), output.response().statusCode());
                } else if (output.error() != null) {
                    log.error("OpenAI batch request {} failed: {} - {}", output.customId(), output.error().code(), output.error().message());
                }
            } catch (Exception e) {
                log.error("Failed to parse OpenAI batch output line: {}", line, e);
            }
        }
        return responses;
    }

    private boolean isTerminal(OpenAiBatch batch) {
        return batch == null || TERMINAL_STATUSES.contains(batch.status());
    }

    private record RequestContext(String customId, Map<String, Object> payload) {
    }

    private record OpenAiBatch(String id,
                               String status,
                               @JsonProperty("output_file_id") String outputFileId) {
    }

    private record OpenAiFile(String id) {
    }

    private record BatchOutput(@JsonProperty("custom_id") String customId,
                               BatchOutputResponse response,
                               BatchOutputError error) {
    }

    private record BatchOutputResponse(@JsonProperty("status_code") Integer statusCode,
                                       Map<String, Object> body) {
        boolean isSuccessful() {
            return statusCode != null && statusCode >= 200 && statusCode < 300;
        }
    }

    private record BatchOutputError(String message, String code) {
    }
}
