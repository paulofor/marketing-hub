package com.marketinghub.microservice.exception.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.microservice.Microservice;
import com.marketinghub.microservice.exception.MicroserviceExceptionLog;
import com.marketinghub.microservice.exception.dto.MicroserviceExceptionRequest;
import com.marketinghub.microservice.exception.dto.MicroserviceExceptionSummary;
import com.marketinghub.microservice.exception.repository.MicroserviceExceptionRepository;
import com.marketinghub.microservice.repository.MicroserviceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MicroserviceExceptionService {
    private final MicroserviceExceptionRepository repository;
    private final MicroserviceRepository microserviceRepository;
    private final ObjectMapper objectMapper;

    public MicroserviceExceptionService(MicroserviceExceptionRepository repository,
                                        MicroserviceRepository microserviceRepository,
                                        ObjectMapper objectMapper) {
        this.repository = repository;
        this.microserviceRepository = microserviceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MicroserviceExceptionLog logException(Long microserviceId, MicroserviceExceptionRequest request) {
        Microservice microservice = microserviceRepository.findById(microserviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Microserviço não encontrado"));

        Instant occurredAt = Optional.ofNullable(request.occurredAt()).orElse(Instant.now());
        String severity = normalizeSeverity(request.severity());
        String exceptionType = StringUtils.hasText(request.exceptionType()) ? request.exceptionType() : "Exception";
        String context = serializeContext(request.context());

        MicroserviceExceptionLog log = MicroserviceExceptionLog.builder()
                .microservice(microservice)
                .exceptionType(exceptionType)
                .message(request.message())
                .stackTrace(request.stackTrace())
                .severity(severity)
                .serviceVersion(request.serviceVersion())
                .hostname(request.hostname())
                .context(context)
                .occurredAt(occurredAt)
                .build();

        return repository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<MicroserviceExceptionLog> list(Long microserviceId, String severity, Pageable pageable) {
        if (microserviceId != null && !microserviceRepository.existsById(microserviceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Microserviço não encontrado");
        }
        boolean hasSeverity = StringUtils.hasText(severity);
        if (microserviceId != null && hasSeverity) {
            return repository.findAllByMicroserviceIdAndSeverityIgnoreCase(microserviceId, severity, pageable);
        }
        if (microserviceId != null) {
            return repository.findAllByMicroserviceId(microserviceId, pageable);
        }
        if (hasSeverity) {
            return repository.findAllBySeverityIgnoreCase(severity, pageable);
        }
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Map<Long, MicroserviceExceptionSummary> summarizeByMicroservices(List<Microservice> microservices) {
        if (microservices == null || microservices.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = microservices.stream()
                .map(Microservice::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, MicroserviceExceptionLog> latestByMicroservice = repository.findLatestByMicroserviceIds(ids).stream()
                .collect(Collectors.toMap(log -> log.getMicroservice().getId(), log -> log, (a, b) -> a));

        Map<Long, Long> countsByMicroservice = repository.countByMicroserviceIds(ids).stream()
                .collect(Collectors.toMap(MicroserviceExceptionRepository.MicroserviceExceptionCountView::getMicroserviceId,
                        MicroserviceExceptionRepository.MicroserviceExceptionCountView::getTotal));

        Map<Long, MicroserviceExceptionSummary> summaries = new HashMap<>();
        for (Long id : ids) {
            MicroserviceExceptionLog latest = latestByMicroservice.get(id);
            long total = countsByMicroservice.getOrDefault(id, 0L);
            summaries.put(id, MicroserviceExceptionSummary.builder()
                    .lastOccurredAt(latest != null ? latest.getOccurredAt() : null)
                    .lastMessage(latest != null ? latest.getMessage() : null)
                    .lastSeverity(latest != null ? latest.getSeverity() : null)
                    .totalCount(total)
                    .build());
        }
        return summaries;
    }

    private String normalizeSeverity(String severity) {
        if (!StringUtils.hasText(severity)) {
            return "ERROR";
        }
        return severity.trim().toUpperCase(Locale.US);
    }

    private String serializeContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contexto inválido", e);
        }
    }
}
