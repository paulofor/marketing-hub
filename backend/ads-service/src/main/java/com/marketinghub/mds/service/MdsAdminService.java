package com.marketinghub.mds.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mds.*;
import com.marketinghub.mds.dto.*;
import com.marketinghub.repository.jpa.mds.MdsArtifactLineageEdgeRepository;
import com.marketinghub.repository.jpa.mds.MdsArtifactRecordRepository;
import com.marketinghub.repository.jpa.mds.MdsProcessingEventRepository;
import com.marketinghub.repository.jpa.mds.MdsRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class MdsAdminService {
    private final MdsRequestRepository requestRepository;
    private final MdsProcessingEventRepository processingEventRepository;
    private final MdsArtifactRecordRepository artifactRecordRepository;
    private final MdsArtifactLineageEdgeRepository lineageEdgeRepository;
    private final ObjectMapper objectMapper;

    public MdsAdminService(MdsRequestRepository requestRepository,
                           MdsProcessingEventRepository processingEventRepository,
                           MdsArtifactRecordRepository artifactRecordRepository,
                           MdsArtifactLineageEdgeRepository lineageEdgeRepository,
                           ObjectMapper objectMapper) {
        this.requestRepository = requestRepository;
        this.processingEventRepository = processingEventRepository;
        this.artifactRecordRepository = artifactRecordRepository;
        this.lineageEdgeRepository = lineageEdgeRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public MdsAdminRequestListResponse listRequests(String status,
                                                    Instant from,
                                                    Instant to,
                                                    String tenantOrProduct,
                                                    int page,
                                                    int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)));

        Specification<MdsRequest> spec = Specification.where(null);
        if (status != null && !status.isBlank()) {
            MdsRequestStatus parsedStatus = parseStatus(status);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), parsedStatus));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        if (tenantOrProduct != null && !tenantOrProduct.isBlank()) {
            String searchTerm = "%" + tenantOrProduct.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("correlationId")), searchTerm));
        }

        Page<MdsRequest> result = requestRepository.findAll(spec, pageable);
        List<MdsAdminRequestListItemResponse> items = result.getContent().stream()
                .map(this::toListItem)
                .toList();

        return new MdsAdminRequestListResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public MdsAdminRequestDetailResponse getRequestDetail(Long id) {
        MdsRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "mds request not found"));

        List<MdsAdminProcessingEventResponse> timeline = processingEventRepository
                .findByRequestIdOrderByCreatedAtAscIdAsc(id)
                .stream()
                .map(this::toTimelineItem)
                .toList();

        return new MdsAdminRequestDetailResponse(
                request.getId(),
                request.getStatus(),
                request.getMarket(),
                request.getProblem(),
                request.getDesiredOutcome(),
                request.getDeliveryConstraint(),
                request.getEvidencePreference(),
                request.getCorrelationId(),
                request.getFailureReason(),
                request.getCreatedAt(),
                request.getStartedAt(),
                request.getFinishedAt(),
                parseJson(request.getContextJson()),
                timeline,
                classifyFailure(request.getFailureReason()),
                "/api/mds/requests/" + request.getId() + "/artifacts",
                "/api/mds/reports/" + request.getId(),
                isRetryEligible(request.getStatus()),
                buildRetryReason(request.getStatus())
        );
    }

    @Transactional(readOnly = true)
    public MdsAdminArtifactsResponse listArtifactsWithLineage(Long requestId) {
        if (!requestRepository.existsById(requestId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "mds request not found");
        }

        List<MdsArtifactLineageEdge> edges = lineageEdgeRepository
                .findByParentArtifact_Request_IdOrChildArtifact_Request_IdOrderByIdAsc(requestId, requestId);

        Map<Long, List<Long>> parentByChild = edges.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        edge -> edge.getChildArtifact().getId(),
                        java.util.stream.Collectors.mapping(edge -> edge.getParentArtifact().getId(), java.util.stream.Collectors.toList())
                ));
        Map<Long, List<Long>> childrenByParent = edges.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        edge -> edge.getParentArtifact().getId(),
                        java.util.stream.Collectors.mapping(edge -> edge.getChildArtifact().getId(), java.util.stream.Collectors.toList())
                ));

        List<MdsAdminArtifactItemResponse> artifacts = artifactRecordRepository
                .findByRequestIdOrderByCreatedAtAscIdAsc(requestId)
                .stream()
                .map(record -> new MdsAdminArtifactItemResponse(
                        record.getId(),
                        record.getArtifactType(),
                        record.getSchemaVersion(),
                        record.getVersion(),
                        record.getStatus().name(),
                        parentByChild.getOrDefault(record.getId(), List.of()),
                        childrenByParent.getOrDefault(record.getId(), List.of()),
                        parseJson(record.getContentJson())
                ))
                .toList();

        List<MdsAdminArtifactLineageEdgeResponse> lineage = edges.stream()
                .map(edge -> new MdsAdminArtifactLineageEdgeResponse(
                        edge.getId(),
                        edge.getParentArtifact().getId(),
                        edge.getChildArtifact().getId(),
                        edge.getRelationType()
                ))
                .toList();

        return new MdsAdminArtifactsResponse(requestId, artifacts, lineage);
    }

    @Transactional
    public MdsAdminRetryResponse retryRequest(Long id) {
        MdsRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "mds request not found"));

        MdsRequestStatus previous = request.getStatus();
        if (!isRetryEligible(previous)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, buildRetryReason(previous));
        }

        request.setStatus(MdsRequestStatus.PENDING);
        request.setStartedAt(null);
        request.setFinishedAt(null);
        request.setFailureReason(null);

        MdsProcessingEvent retryEvent = MdsProcessingEvent.builder()
                .request(request)
                .stageName("orchestration")
                .eventType(MdsEventType.INFO)
                .message("request moved back to pending by admin retry")
                .payloadJson("{}")
                .build();
        processingEventRepository.save(retryEvent);

        return new MdsAdminRetryResponse(id, previous, request.getStatus(), "retry accepted");
    }

    private MdsAdminRequestListItemResponse toListItem(MdsRequest request) {
        MdsProcessingEvent latestEvent = processingEventRepository
                .findTopByRequestIdOrderByCreatedAtDescIdDesc(request.getId())
                .orElse(null);
        MdsProcessingEvent heartbeatEvent = processingEventRepository
                .findTopByRequestIdAndEventTypeOrderByCreatedAtDescIdDesc(request.getId(), MdsEventType.HEARTBEAT)
                .orElse(null);

        long attempts = processingEventRepository.countByRequestIdAndEventType(request.getId(), MdsEventType.INFO);
        int normalizedAttempts = (int) Math.max(attempts, request.getStatus() == MdsRequestStatus.PENDING ? 0 : 1);

        return new MdsAdminRequestListItemResponse(
                request.getId(),
                request.getMarket(),
                request.getProblem(),
                request.getDesiredOutcome(),
                request.getStatus(),
                latestEvent == null ? "pending" : latestEvent.getStageName(),
                normalizedAttempts,
                heartbeatEvent == null ? null : heartbeatEvent.getCreatedAt(),
                request.getUpdatedAt(),
                isRetryEligible(request.getStatus()),
                buildRetryReason(request.getStatus())
        );
    }

    private MdsAdminProcessingEventResponse toTimelineItem(MdsProcessingEvent event) {
        return new MdsAdminProcessingEventResponse(
                event.getId(),
                event.getStageName(),
                event.getEventType(),
                event.getMessage(),
                parseJson(event.getPayloadJson()),
                event.getCreatedAt()
        );
    }

    private String classifyFailure(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return "NONE";
        }
        String normalized = failureReason.toLowerCase();
        if (normalized.contains("timeout") || normalized.contains("429") || normalized.contains("network")) {
            return "RECOVERABLE";
        }
        return "NON_RECOVERABLE";
    }

    private boolean isRetryEligible(MdsRequestStatus status) {
        return status == MdsRequestStatus.FAILED;
    }

    private String buildRetryReason(MdsRequestStatus status) {
        if (isRetryEligible(status)) {
            return "READY";
        }
        return switch (status) {
            case PENDING -> "request is still pending execution";
            case IN_PROGRESS -> "request is in progress and cannot be retried";
            case COMPLETED -> "request is completed; retry requires manual replay flow";
            case FAILED -> "READY";
        };
    }

    private MdsRequestStatus parseStatus(String value) {
        try {
            return MdsRequestStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "status must be one of PENDING, IN_PROGRESS, COMPLETED, FAILED");
        }
    }

    private Map<String, Object> parseJson(String value) {
        try {
            return objectMapper.readValue(value == null ? "{}" : value, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "invalid json payload");
        }
    }
}
