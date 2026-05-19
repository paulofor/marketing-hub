package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class OpenAiSalesPageAnalyzer {

    private static final Set<String> TERMINAL_BATCH_STATUSES = Set.of("completed", "failed", "expired", "cancelled");

    private final RestClient restClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiSalesPageAnalyzer(RestClient.Builder builder, OpenAiProperties properties) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.normalizedBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .defaultHeader("OpenAI-Beta", "reasoning=1")
                .build();
    }

    public SalesPageAnalysisResult analyze(long pageId, String canonicalUrl, String htmlBodyText) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new IllegalStateException("OpenAI api key não configurada para análise de sales page");
        }
        String payloadLine = buildBatchLine(pageId, canonicalUrl, htmlBodyText);
        String inputFileId = uploadInputFile(payloadLine);
        BatchInfo batch = createBatch(inputFileId);
        BatchInfo completed = awaitBatch(batch);
        if (!"completed".equalsIgnoreCase(completed.status()) || !StringUtils.hasText(completed.outputFileId())) {
            throw new IllegalStateException("Batch da OpenAI não completou com output_file_id. status=" + completed.status());
        }
        String output = downloadOutput(completed.outputFileId());
        return parseBatchOutput(output);
    }

    private String buildBatchLine(long pageId, String canonicalUrl, String htmlBodyText) {
        String prompt = "Analise a página de vendas e devolva JSON válido com os campos: score_total (0-100), sections_json (objeto), copy_json (objeto), visual_json (objeto), image_json (objeto), analysis_notes (texto curto). URL: "
                + canonicalUrl + "\nConteúdo: " + htmlBodyText;
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "custom_id", "page-" + pageId,
                    "method", "POST",
                    "url", "/v1/responses",
                    "body", Map.of(
                            "model", properties.normalizedModel(),
                            "input", List.of(
                                    Map.of("role", "system", "content", "Você analisa páginas de venda e responde exclusivamente em JSON válido sem markdown."),
                                    Map.of("role", "user", "content", prompt)
                            ),
                            "response_format", Map.of("type", "json_object")
                    )
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar linha batch", e);
        }
    }

    private String uploadInputFile(String line) {
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("purpose", "batch");
        multipart.add("file", new org.springframework.core.io.ByteArrayResource((line + "\n").getBytes(StandardCharsets.UTF_8)) {
            @Override public String getFilename() { return "sales-page-analysis.jsonl"; }
        });
        FileUploadResponse response = restClient.post().uri("/files").contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipart).retrieve().body(FileUploadResponse.class);
        if (response == null || !StringUtils.hasText(response.id())) throw new IllegalStateException("Upload do arquivo batch falhou");
        return response.id();
    }

    private BatchInfo createBatch(String inputFileId) {
        BatchInfo response = restClient.post().uri("/batches")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("input_file_id", inputFileId, "endpoint", "/v1/responses", "completion_window", "24h"))
                .retrieve().body(BatchInfo.class);
        if (response == null || !StringUtils.hasText(response.id())) throw new IllegalStateException("Criação do batch falhou");
        return response;
    }

    private BatchInfo awaitBatch(BatchInfo batch) {
        Instant start = Instant.now();
        BatchInfo current = batch;
        while (!TERMINAL_BATCH_STATUSES.contains(current.status())) {
            if (Instant.now().toEpochMilli() - start.toEpochMilli() > properties.normalizedBatchTimeoutMs()) {
                throw new IllegalStateException("Timeout aguardando batch " + current.id());
            }
            try { Thread.sleep(properties.normalizedBatchPollIntervalMs()); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
            current = restClient.get().uri("/batches/{id}", current.id()).retrieve().body(BatchInfo.class);
            if (current == null || !StringUtils.hasText(current.status())) throw new IllegalStateException("Batch status inválido");
        }
        return current;
    }

    private String downloadOutput(String outputFileId) {
        return restClient.get().uri("/files/{id}/content", outputFileId).retrieve().body(String.class);
    }

    private SalesPageAnalysisResult parseBatchOutput(String outputJsonl) {
        try {
            String line = outputJsonl.lines().filter(StringUtils::hasText).findFirst().orElseThrow();
            JsonNode root = objectMapper.readTree(line);
            JsonNode outputText = root.path("response").path("body").path("output").get(0).path("content").get(0).path("text");
            if (outputText.isMissingNode() || outputText.isNull()) {
                outputText = root.path("response").path("body").path("output_text");
            }
            JsonNode parsed = objectMapper.readTree(outputText.asText());
            return new SalesPageAnalysisResult(
                    parsed.path("score_total").decimalValue(),
                    objectMapper.writeValueAsString(parsed.path("sections_json")),
                    objectMapper.writeValueAsString(parsed.path("copy_json")),
                    objectMapper.writeValueAsString(parsed.path("visual_json")),
                    objectMapper.writeValueAsString(parsed.path("image_json")),
                    parsed.path("analysis_notes").asText("Análise gerada via OpenAI batch"),
                    "html-v1",
                    "openai-batch-v1",
                    properties.normalizedModel()
            );
        } catch (Exception e) {
            log.error("Falha parse output batch OpenAI. output={}", outputJsonl, e);
            throw new IllegalStateException("Falha ao interpretar output do batch OpenAI", e);
        }
    }

    private record FileUploadResponse(String id) {}
    private record BatchInfo(String id, String status, @JsonProperty("output_file_id") String outputFileId) {}
}
