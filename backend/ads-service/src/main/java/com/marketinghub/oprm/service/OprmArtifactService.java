package com.marketinghub.oprm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.oprm.OprmArtifact;
import com.marketinghub.oprm.OprmArtifactStatus;
import com.marketinghub.oprm.OprmJob;
import com.marketinghub.oprm.dto.OprmArtifactEnvelopeDto;
import com.marketinghub.oprm.dto.OprmArtifactPublishRequestDto;
import com.marketinghub.oprm.dto.OprmArtifactPublishResponseDto;
import com.marketinghub.oprm.dto.OprmArtifactSummaryDto;
import com.marketinghub.oprm.repository.OprmArtifactRepository;
import com.marketinghub.oprm.repository.OprmJobRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OprmArtifactService {
    private final OprmArtifactRepository artifactRepository;
    private final OprmJobRepository jobRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OprmArtifactPublishResponseDto publishArtifact(OprmArtifactPublishRequestDto request) {
        OprmArtifact duplicatedArtifact = artifactRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (duplicatedArtifact != null) {
            return new OprmArtifactPublishResponseDto(
                    duplicatedArtifact.getArtifactId(),
                    duplicatedArtifact.getArtifactType(),
                    duplicatedArtifact.getArtifactVersion(),
                    duplicatedArtifact.getCreatedAt().toString(),
                    duplicatedArtifact.getArtifactStatus().name(),
                    true
            );
        }

        UUID jobId;
        try {
            jobId = UUID.fromString(request.jobId());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "jobId must be a valid UUID");
        }

        OprmJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OPRM job not found"));

        OprmArtifactEnvelopeDto artifactEnvelope = request.artifact();
        if (!request.correlationId().equals(artifactEnvelope.correlationId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "correlationId from request must match artifact envelope");
        }

        if (artifactEnvelope.sourceRefs().isEmpty() && artifactEnvelope.inputRefs().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "artifact lineage requires sourceRefs or inputRefs");
        }

        OprmArtifact saved = artifactRepository.save(OprmArtifact.builder()
                .job(job)
                .artifactId(artifactEnvelope.artifactId())
                .artifactType(artifactEnvelope.artifactType())
                .artifactVersion(artifactEnvelope.artifactVersion())
                .moduleName(artifactEnvelope.moduleName())
                .producer(artifactEnvelope.producer())
                .artifactCreatedAt(parseCreatedAt(artifactEnvelope.createdAt()))
                .correlationId(artifactEnvelope.correlationId())
                .occupationSeedRef(job.getOccupationSeedRef())
                .traceId(artifactEnvelope.traceId())
                .sourceRefsJson(toJson(artifactEnvelope.sourceRefs()))
                .inputRefsJson(toJson(artifactEnvelope.inputRefs()))
                .payloadJson(toJson(artifactEnvelope.payload()))
                .lineageJson(toJson(request.lineage()))
                .metadataJson(toJson(artifactEnvelope.metadata()))
                .artifactStatus(artifactEnvelope.status())
                .confidenceScore(toBigDecimal(artifactEnvelope.confidenceScore()))
                .idempotencyKey(request.idempotencyKey())
                .build());

        return new OprmArtifactPublishResponseDto(
                saved.getArtifactId(),
                saved.getArtifactType(),
                saved.getArtifactVersion(),
                saved.getCreatedAt().toString(),
                saved.getArtifactStatus().name(),
                false
        );
    }

    @Transactional(readOnly = true)
    public List<OprmArtifactSummaryDto> listArtifacts(String correlationId,
                                                      String occupationSeedRef,
                                                      OprmArtifactStatus status) {
        if (correlationId != null && !correlationId.isBlank()) {
            return artifactRepository.findByCorrelationIdOrderByCreatedAtDesc(correlationId)
                    .stream()
                    .map(this::toSummary)
                    .toList();
        }

        if (occupationSeedRef != null && !occupationSeedRef.isBlank()) {
            List<OprmArtifact> artifacts = status == null
                    ? artifactRepository.findByOccupationSeedRefOrderByCreatedAtDesc(occupationSeedRef)
                    : artifactRepository.findByOccupationSeedRefAndArtifactStatusOrderByCreatedAtDesc(occupationSeedRef, status);
            return artifacts.stream().map(this::toSummary).toList();
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "use correlationId or occupationSeedRef to filter OPRM artifacts");
    }

    private OprmArtifactSummaryDto toSummary(OprmArtifact artifact) {
        return new OprmArtifactSummaryDto(
                artifact.getArtifactId(),
                artifact.getArtifactType(),
                artifact.getArtifactVersion(),
                artifact.getArtifactStatus(),
                artifact.getOccupationSeedRef(),
                artifact.getCorrelationId(),
                artifact.getCreatedAt().toString()
        );
    }

    private Instant parseCreatedAt(String createdAt) {
        try {
            return Instant.parse(createdAt);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "artifact.createdAt must use ISO-8601");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "unable to serialize artifact payload for persistence");
        }
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
