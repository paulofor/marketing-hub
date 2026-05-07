package com.marketinghub.worker.geralanding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.OpenAiCostEstimator;
import com.marketinghub.worker.openai.OpenAiResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class GeraLandingOpenAiBatchClient {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingOpenAiBatchClient.class);

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final boolean enabled;
    private final Duration batchTimeout;
    private final Duration pollInterval;

    public GeraLandingOpenAiBatchClient(WebClient.Builder builder,
                                        ObjectMapper objectMapper,
                                        @Value("${openai.api-key:}") String apiKey,
                                        @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                        @Value("${openai.batch-timeout:PT30M}") Duration batchTimeout,
                                        @Value("${openai.batch.poll-interval-ms:3000}") long pollIntervalMs) {
        this.objectMapper = objectMapper;
        this.enabled = StringUtils.hasText(apiKey);
        this.batchTimeout = batchTimeout != null && !batchTimeout.isNegative() && !batchTimeout.isZero()
                ? batchTimeout
                : Duration.ofMinutes(30);
        this.pollInterval = Duration.ofMillis(Math.max(500, pollIntervalMs));
        WebClient.Builder clientBuilder = builder.clone().baseUrl(baseUrl);
        if (enabled) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
        } else {
            log.warn("OPENAI_API_KEY não configurada; jobs de gera-landing ficarão pendentes");
        }
        this.webClient = clientBuilder.build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public GeraLandingJobCompletionPayload generate(GeraLandingJobDto job) {
        if (!enabled) {
            throw new IllegalStateException("OpenAI API key não configurada");
        }
        try {
            String jsonlLine = objectMapper.writeValueAsString(Map.of(
                    "custom_id", job.id().toString(),
                    "method", "POST",
                    "url", "/v1/responses",
                    "body", objectMapper.readValue(job.prompt(), Map.class)));

            String inputFileId = uploadBatchInput(jsonlLine + "\n");
            BatchResponse batch = createBatch(inputFileId);
            BatchResponse completed = pollUntilCompleted(batch.id());
            if (!StringUtils.hasText(completed.outputFileId())) {
                throw new IllegalStateException("Batch finalizado sem output_file_id");
            }
            String rawOutput = downloadFileContent(completed.outputFileId());
            OpenAiResponse response = parseFirstResponse(rawOutput);
            String modelResponse = response.firstText();
            if (!StringUtils.hasText(modelResponse)) {
                throw new IllegalStateException("Modelo não retornou conteúdo para gera-landing");
            }
            Integer inputTokens = response.usage() != null ? response.usage().effectiveInputTokens() : null;
            Integer outputTokens = response.usage() != null ? response.usage().effectiveOutputTokens() : null;
            return new GeraLandingJobCompletionPayload(
                    modelResponse,
                    rawOutput,
                    job.requestBodyJson(),
                    response.id(),
                    inputTokens,
                    outputTokens,
                    OpenAiCostEstimator.estimateUsd(job.model(), response.usage()));
        } catch (WebClientResponseException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();
            log.error("Falha HTTP OpenAI batch no gera-landing [jobId={}, stage={}, status={}, responseBody={}]",
                    job.id(), job.section(), statusCode.value(), ex.getResponseBodyAsString());
            throw new IllegalStateException("Falha HTTP ao gerar conteúdo de gera-landing em modo batch", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao gerar conteúdo de gera-landing em modo batch", ex);
        }
    }

    private String uploadBatchInput(String jsonlContent) {
        MultipartBodyBuilder form = new MultipartBodyBuilder();
        form.part("purpose", "batch");
        form.part("file", jsonlContent.getBytes(StandardCharsets.UTF_8))
                .filename("geralanding-batch.jsonl")
                .contentType(MediaType.TEXT_PLAIN);
        FileResponse file = webClient.post().uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(form.build())
                .retrieve()
                .bodyToMono(FileResponse.class)
                .block();
        if (file == null || !StringUtils.hasText(file.id())) {
            throw new IllegalStateException("Falha ao obter file_id para batch");
        }
        return file.id();
    }

    private BatchResponse createBatch(String inputFileId) {
        BatchResponse batch = webClient.post().uri("/batches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("input_file_id", inputFileId, "endpoint", "/v1/responses", "completion_window", "24h"))
                .retrieve().bodyToMono(BatchResponse.class).block();
        if (batch == null || !StringUtils.hasText(batch.id())) {
            throw new IllegalStateException("Falha ao criar batch");
        }
        return batch;
    }

    private BatchResponse pollUntilCompleted(String batchId) throws InterruptedException {
        long timeoutMillis = batchTimeout.toMillis();
        long intervalMillis = pollInterval.toMillis();
        long maxAttempts = Math.max(1, (timeoutMillis + intervalMillis - 1) / intervalMillis);
        for (long attempt = 1; attempt <= maxAttempts; attempt++) {
            BatchResponse batch = webClient.get().uri("/batches/{id}", batchId)
                    .retrieve().bodyToMono(BatchResponse.class).block();
            if (batch == null) {
                throw new IllegalStateException("Resposta vazia ao consultar batch " + batchId);
            }
            String status = batch.status();
            if ("completed".equalsIgnoreCase(status)) {
                return batch;
            }
            if (List.of("failed", "expired", "cancelled").contains(status)) {
                throw new IllegalStateException("Batch finalizado com status inválido: " + status);
            }
            Thread.sleep(pollInterval.toMillis());
        }
        throw new IllegalStateException("Timeout aguardando conclusão do batch " + batchId
                + " após " + batchTimeout);
    }

    private String downloadFileContent(String fileId) {
        byte[] bytes = webClient.get().uri("/files/{id}/content", fileId)
                .retrieve().bodyToMono(byte[].class)
                .block();
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("Arquivo de saída do batch vazio");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private OpenAiResponse parseFirstResponse(String jsonlOutput) throws Exception {
        String firstLine = jsonlOutput.lines().filter(StringUtils::hasText).findFirst()
                .orElseThrow(() -> new IllegalStateException("Saída do batch sem linhas"));
        BatchOutputLine line = objectMapper.readValue(firstLine, BatchOutputLine.class);
        if (line.status_code() != null && line.status_code() >= 400) {
            String rawErrorBody = line.response() != null ? line.response().rawBody() : null;
            throw new IllegalStateException("OpenAI retornou erro no batch. status=" + line.status_code()
                    + ", custom_id=" + line.custom_id()
                    + ", body=" + rawErrorBody);
        }
        if (line.response() == null || line.response().body() == null || line.response().body().isNull()) {
            throw new IllegalStateException("Linha de saída do batch sem response.body");
        }
        return objectMapper.treeToValue(line.response().body(), OpenAiResponse.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FileResponse(String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BatchResponse(String id, String status, @com.fasterxml.jackson.annotation.JsonProperty("output_file_id") String outputFileId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BatchOutputLine(String custom_id, Integer status_code, BatchHttpResponse response) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BatchHttpResponse(JsonNode body) {
        String rawBody() {
            return body != null ? body.toString() : null;
        }
    }
}
