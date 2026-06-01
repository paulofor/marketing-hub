package com.marketinghub.facebookads.playbook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.facebookads.playbook.*;
import com.marketinghub.facebookads.playbook.dto.ExperimentFacebookApiLogDto;
import com.marketinghub.facebookads.playbook.dto.ExperimentFacebookApiLogIngestionRequest;
import com.marketinghub.repository.jpa.facebookads.playbook.ExperimentAdSetJobApiLogRepository;
import com.marketinghub.repository.jpa.facebookads.playbook.ExperimentFacebookApiLogEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class ExperimentFacebookApiLogService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;
    private static final Pattern JSON_TOKEN_PATTERN =
            Pattern.compile("(?i)(\"(?:access_token|token|authorization)\"\s*:\s*\")([^\"]*)");
    private static final Pattern QUERY_TOKEN_PATTERN =
            Pattern.compile("(?i)(access_token=)([^&\\s]+)");

    private final ExperimentRepository experimentRepository;
    private final ExperimentAdSetJobApiLogRepository jobApiLogRepository;
    private final ExperimentFacebookApiLogEntryRepository apiLogEntryRepository;
    private final ObjectMapper objectMapper;

    public ExperimentFacebookApiLogService(ExperimentRepository experimentRepository,
                                           ExperimentAdSetJobApiLogRepository jobApiLogRepository,
                                           ExperimentFacebookApiLogEntryRepository apiLogEntryRepository,
                                           ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.jobApiLogRepository = jobApiLogRepository;
        this.apiLogEntryRepository = apiLogEntryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ExperimentFacebookApiLogDto> findLogs(Long experimentId, int limit) {
        if (experimentId == null) {
            throw new EntityNotFoundException("Experimento não informado");
        }
        if (!experimentRepository.existsById(experimentId)) {
            throw new EntityNotFoundException("Experimento %d não encontrado".formatted(experimentId));
        }
        int resolvedLimit = resolveLimit(limit);
        Pageable pageable = PageRequest.of(
                0,
                resolvedLimit,
                Sort.by(Sort.Order.desc("requestedAt").nullsLast(), Sort.Order.desc("createdAt"))
        );
        List<ExperimentAdSetJobApiLog> jobLogs = jobApiLogRepository.findByJobWorkflowExperimentId(experimentId, pageable);
        List<ExperimentFacebookApiLogEntry> customLogs = apiLogEntryRepository.findByExperimentId(experimentId, pageable);
        return Stream.concat(
                        jobLogs.stream().map(this::toDto),
                        customLogs.stream().map(this::toDto)
                )
                .sorted(Comparator.comparing(this::sortKey, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(resolvedLimit)
                .toList();
    }

    @Transactional
    public void registerLogs(Long experimentId, ExperimentFacebookApiLogIngestionRequest request) {
        if (experimentId == null) {
            throw new EntityNotFoundException("Experimento não informado");
        }
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experimento %d não encontrado".formatted(experimentId)));
        if (request == null || CollectionUtils.isEmpty(request.logs())) {
            return;
        }
        ExperimentFacebookApiLogContext context = request.context() != null
                ? request.context()
                : ExperimentFacebookApiLogContext.CAMPAIGN_AD_SET;
        List<ExperimentFacebookApiLogEntry> entries = new ArrayList<>();
        for (ExperimentFacebookApiLogIngestionRequest.ApiCallPayload payload : request.logs()) {
            if (payload == null) {
                continue;
            }
            ExperimentFacebookApiLogEntry entry = new ExperimentFacebookApiLogEntry();
            entry.setExperiment(experiment);
            entry.setContext(context);
            entry.setProvider(resolveProvider(payload.provider()));
            entry.setEndpoint(payload.endpoint());
            entry.setHttpMethod(payload.httpMethod());
            entry.setStatusCode(payload.statusCode());
            entry.setRequestedAt(payload.requestedAt());
            entry.setRespondedAt(payload.respondedAt());
            entry.setRequestPayload(asJsonString(payload.requestPayload()));
            entry.setResponsePayload(asJsonString(payload.responsePayload()));
            entry.setErrorMessage(payload.errorMessage());
            entries.add(entry);
        }
        if (!entries.isEmpty()) {
            apiLogEntryRepository.saveAll(entries);
        }
    }

    private Instant sortKey(ExperimentFacebookApiLogDto dto) {
        if (dto == null) {
            return null;
        }
        if (dto.requestedAt() != null) {
            return dto.requestedAt();
        }
        if (dto.respondedAt() != null) {
            return dto.respondedAt();
        }
        return dto.createdAt();
    }

    private ExperimentFacebookApiLogDto toDto(ExperimentAdSetJobApiLog log) {
        ExperimentAdSetJob job = log.getJob();
        ExperimentAdSetWorkflow workflow = job != null ? job.getWorkflow() : null;
        Instant requestedAt = log.getRequestedAt();
        Instant respondedAt = log.getRespondedAt();
        Long durationMs = (requestedAt != null && respondedAt != null)
                ? Duration.between(requestedAt, respondedAt).toMillis()
                : null;
        return new ExperimentFacebookApiLogDto(
                log.getId(),
                job != null ? job.getId() : null,
                job != null ? job.getType() : null,
                job != null ? job.getWorker() : null,
                job != null ? job.getStatus() : null,
                workflow != null ? workflow.getId() : null,
                job != null ? job.getResourceId() : null,
                job != null && job.getType() != null ? job.getType().name() : null,
                log.getProvider(),
                sanitizeEndpoint(log.getEndpoint()),
                log.getHttpMethod(),
                log.getStatusCode(),
                log.getErrorMessage(),
                requestedAt,
                respondedAt,
                durationMs,
                sanitizePayload(log.getRequestPayload()),
                sanitizePayload(log.getResponsePayload()),
                log.getCreatedAt()
        );
    }

    private ExperimentFacebookApiLogDto toDto(ExperimentFacebookApiLogEntry entry) {
        Instant requestedAt = entry.getRequestedAt();
        Instant respondedAt = entry.getRespondedAt();
        Long durationMs = (requestedAt != null && respondedAt != null)
                ? Duration.between(requestedAt, respondedAt).toMillis()
                : null;
        return new ExperimentFacebookApiLogDto(
                entry.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                entry.getContext() != null ? entry.getContext().name() : null,
                entry.getProvider(),
                sanitizeEndpoint(entry.getEndpoint()),
                entry.getHttpMethod(),
                entry.getStatusCode(),
                entry.getErrorMessage(),
                requestedAt,
                respondedAt,
                durationMs,
                sanitizePayload(entry.getRequestPayload()),
                sanitizePayload(entry.getResponsePayload()),
                entry.getCreatedAt()
        );
    }

    private int resolveLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String sanitizeEndpoint(String endpoint) {
        if (!StringUtils.hasText(endpoint)) {
            return endpoint;
        }
        Matcher matcher = QUERY_TOKEN_PATTERN.matcher(endpoint);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String masked = maskValue(matcher.group(2));
            matcher.appendReplacement(buffer, matcher.group(1) + masked);
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String sanitizePayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return payload;
        }
        try {
            JsonNode node = objectMapper.readTree(payload);
            maskJsonNode(node);
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return maskRawPayload(payload);
        }
    }

    private void maskJsonNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode child = entry.getValue();
                if (isSensitiveKey(entry.getKey()) && child != null && child.isTextual()) {
                    objectNode.put(entry.getKey(), maskValue(child.asText()));
                } else {
                    maskJsonNode(child);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                maskJsonNode(child);
            }
        }
    }

    private boolean isSensitiveKey(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains("token") || normalized.contains("authorization");
    }

    private String maskRawPayload(String payload) {
        Matcher jsonMatcher = JSON_TOKEN_PATTERN.matcher(payload);
        StringBuffer buffer = new StringBuffer();
        while (jsonMatcher.find()) {
            String replacement = jsonMatcher.group(1) + maskValue(jsonMatcher.group(2)) + '"';
            jsonMatcher.appendReplacement(buffer, replacement);
        }
        jsonMatcher.appendTail(buffer);
        Matcher queryMatcher = QUERY_TOKEN_PATTERN.matcher(buffer.toString());
        StringBuffer finalBuffer = new StringBuffer();
        while (queryMatcher.find()) {
            queryMatcher.appendReplacement(finalBuffer, queryMatcher.group(1) + maskValue(queryMatcher.group(2)));
        }
        queryMatcher.appendTail(finalBuffer);
        return finalBuffer.toString();
    }

    private String maskValue(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "***";
        }
        String trimmed = raw.trim();
        if (trimmed.length() <= 6) {
            return "***";
        }
        return trimmed.substring(0, 3) + "…" + trimmed.substring(trimmed.length() - 2);
    }

    private String resolveProvider(String provider) {
        if (StringUtils.hasText(provider)) {
            return provider.trim();
        }
        return "FACEBOOK";
    }

    private String asJsonString(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.toString();
    }
}
