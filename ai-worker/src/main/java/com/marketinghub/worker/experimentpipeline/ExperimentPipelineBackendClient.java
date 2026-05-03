package com.marketinghub.worker.experimentpipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.util.UrlUtils;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ExperimentPipelineBackendClient {
    private static final Logger log = LoggerFactory.getLogger(ExperimentPipelineBackendClient.class);
    private static final Pattern FORM_CONTROL_TAG_PATTERN = Pattern.compile("(?is)<(input|textarea|select)\\b[^>]*>");
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("(?is)([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*([\"'])(.*?)\\2");

    private final WebClient webClient;
    private final String backendBaseUrl;
    private final String apiPrefix;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExperimentPipelineBackendClient(WebClient.Builder builder,
                                           @Value("${backend.base-url:http://191.252.181.168:8000}") String backendBaseUrl,
                                           @Value("${backend.api-prefix:/api}") String apiPrefix) {
        this.webClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    public List<ExperimentPipelineJobDto> listPending(int limit) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/experiment-pipeline/jobs/pending");
        String uri = url + "?limit=" + Math.max(1, limit);
        log.info("Fetching pending experiment pipeline jobs from {}", uri);
        List<ExperimentPipelineJobDto> payload = webClient.get()
                .uri(uri)
                .exchangeToFlux(response -> handleListResponse(uri, response.statusCode(), response))
                .collectList()
                .doOnError(err -> log.error("Failed to fetch pending experiment pipeline jobs from {}", uri, err))
                .block();
        List<ExperimentPipelineJobDto> result = payload != null ? payload : List.of();
        log.info("Backend returned {} pending experiment pipeline job(s)", result.size());
        return result;
    }

    public ExperimentPipelineJobDto claim(UUID jobId, String workerId) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/experiment-pipeline/jobs/", jobId.toString(), "/claim");
        return webClient.post()
                .uri(url)
                .bodyValue(Collections.singletonMap("workerId", workerId))
                .retrieve()
                .bodyToMono(ExperimentPipelineJobDto.class)
                .onErrorResume(err -> Mono.empty())
                .block();
    }

    public void complete(UUID jobId, ExperimentPipelineJobCompletionPayload payload) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/experiment-pipeline/jobs/", jobId.toString(), "/complete");
        log.info("POST /complete do pipeline (jobId={}, url={}, keys={}, landingHtmlDiag={}, requestBodyJsonRaw={}, rawResponseRaw={})",
                jobId,
                url,
                summarizeCompletionPayloadKeys(payload),
                summarizeLandingHtmlDiagnostic(payload),
                summarizeRawPayload(payload != null ? payload.requestBodyJson() : null),
                summarizeRawPayload(payload != null ? payload.rawResponse() : null));
        try {
            webClient.post()
                    .uri(url)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException ex) {
            String summarizedBody = summarizeErrorBody(ex.getResponseBodyAsString());
            if (ex.getStatusCode().value() == 422) {
                log.error("Erro 422 ao completar job de pipeline {} (url={}, keys={}, landingHtmlDiag={}): {}",
                        jobId,
                        url,
                        summarizeCompletionPayloadKeys(payload),
                        summarizeLandingHtmlDiagnostic(payload),
                        summarizedBody);
            } else {
                log.error("Erro HTTP {} ao completar job de pipeline {}: {}",
                        ex.getStatusCode().value(), jobId, summarizedBody);
            }
            throw ex;
        }
    }


    public void recordGenerationLog(UUID jobId,
                                    String requestBodyJson,
                                    String rawResponse,
                                    String model,
                                    Integer inputTokens,
                                    Integer outputTokens,
                                    BigDecimal costUsd) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/ai/generations/internal");
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("domain", "experiment-pipeline");
        body.put("referenceId", jobId != null ? jobId.toString() : null);
        body.put("prompt", requestBodyJson);
        body.put("rawResponse", rawResponse);
        body.put("model", model);
        body.put("inputTokens", inputTokens);
        body.put("outputTokens", outputTokens);
        body.put("costUsd", costUsd);
        webClient.post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(ignored -> log.info("Generation log persisted for experiment pipeline job {}", jobId))
                .doOnError(err -> log.warn("Failed to persist generation log for experiment pipeline job {}", jobId, err))
                .onErrorResume(err -> Mono.empty())
                .block();
    }

    public void fail(UUID jobId, String errorMessage) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/experiment-pipeline/jobs/", jobId.toString(), "/fail");
        webClient.post()
                .uri(url)
                .bodyValue(new ExperimentPipelineJobFailurePayload(errorMessage))
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(err -> Mono.empty())
                .block();
    }

    public void updateStage(UUID jobId, String stage) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/experiment-pipeline/jobs/", jobId.toString(), "/stage");
        webClient.post()
                .uri(url)
                .bodyValue(Collections.singletonMap("stage", stage))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public void registerGeraLandingPrompt(Long experimentId,
                                          String stageCode,
                                          String executionId,
                                          String promptContent) {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/internal/geralanding/stage-executions");
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("experimentId", experimentId);
        body.put("stageCode", stageCode);
        body.put("executionId", executionId);
        body.put("promptContent", promptContent);
        webClient.post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(ignored -> log.info("GeraLanding prompt execution persisted (experimentId={}, stageCode={}, executionId={})",
                        experimentId, stageCode, executionId))
                .doOnError(err -> log.warn("Failed to persist GeraLanding prompt execution (experimentId={}, stageCode={}, executionId={})",
                        experimentId, stageCode, executionId, err))
                .onErrorResume(err -> Mono.empty())
                .block();
    }

    private Flux<ExperimentPipelineJobDto> handleListResponse(String uri,
                                                              HttpStatusCode status,
                                                              org.springframework.web.reactive.function.client.ClientResponse response) {
        if (status.isError()) {
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMapMany(body -> Mono.error(new IllegalStateException(
                            "GET %s failed with status %s: %s".formatted(uri, status, body))));
        }
        return response.bodyToFlux(ExperimentPipelineJobDto.class);
    }


    private String summarizeRawPayload(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return "[empty]";
        }
        String compact = rawJson.replaceAll("\s+", " ").trim();
        return compact.length() > 2000 ? compact.substring(0, 2000) + "..." : compact;
    }

    private String summarizeCompletionPayloadKeys(ExperimentPipelineJobCompletionPayload payload) {
        if (payload == null || payload.responseContent() == null) {
            return "[]";
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(payload.responseContent(), Map.class);
            return parsed.keySet().toString();
        } catch (Exception ignored) {
            return "[unparseable-response-content]";
        }
    }

    private String summarizeErrorBody(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return "[sem corpo de erro]";
        }
        String compact = rawBody.replaceAll("\\s+", " ").trim();
        return compact.length() > 300 ? compact.substring(0, 300) + "..." : compact;
    }

    @SuppressWarnings("unchecked")
    private String summarizeLandingHtmlDiagnostic(ExperimentPipelineJobCompletionPayload payload) {
        if (payload == null || payload.responseContent() == null) {
            return "[sem-payload]";
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(payload.responseContent(), Map.class);
            Object node = parsed.containsKey("landingPageHtml") ? parsed.get("landingPageHtml") : parsed;
            if (!(node instanceof Map<?, ?> rawMap)) {
                return "[landingPageHtml-ausente]";
            }
            Map<String, Object> landingPayload = (Map<String, Object>) rawMap;
            Object htmlNode = landingPayload.get("htmlDocument");
            if (!(htmlNode instanceof String htmlDocument) || htmlDocument.isBlank()) {
                return "[htmlDocument-ausente]";
            }
            return "htmlLength=" + htmlDocument.length() + ", formFields=" + extractFieldSnapshot(htmlDocument);
        } catch (Exception ignored) {
            return "[landing-html-nao-processavel]";
        }
    }

    private String extractFieldSnapshot(String htmlDocument) {
        Matcher matcher = FORM_CONTROL_TAG_PATTERN.matcher(htmlDocument);
        List<String> fields = new java.util.ArrayList<>();
        while (matcher.find()) {
            String tag = matcher.group();
            Map<String, String> attrs = parseAttributes(tag);
            String name = attrs.get("name");
            if (name == null || name.isBlank()) {
                continue;
            }
            String type = attrs.get("type");
            if (type == null || type.isBlank()) {
                type = "text";
            }
            boolean required = attrs.containsKey("required") || tag.toLowerCase().contains(" required");
            fields.add(name + ":" + type + ":" + required);
        }
        return fields.isEmpty() ? "[]" : fields.toString();
    }

    private Map<String, String> parseAttributes(String tag) {
        Map<String, String> attrs = new java.util.LinkedHashMap<>();
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(tag);
        while (matcher.find()) {
            attrs.put(matcher.group(1).toLowerCase(), matcher.group(3));
        }
        return attrs;
    }
}
