package com.marketinghub.mds.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mds.*;
import com.marketinghub.mds.dto.*;
import com.marketinghub.repository.jpa.mds.MdsProcessingEventRepository;
import com.marketinghub.repository.jpa.mds.MdsRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class MdsRequestService {
    private final MdsRequestRepository requestRepository;
    private final MdsProcessingEventRepository processingEventRepository;
    private final ObjectMapper objectMapper;

    public MdsRequestService(MdsRequestRepository requestRepository,
                             MdsProcessingEventRepository processingEventRepository,
                             ObjectMapper objectMapper) {
        this.requestRepository = requestRepository;
        this.processingEventRepository = processingEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MdsRequestStatusResponse createRequest(MdsRequestCreateRequest request) {
        MdsRequest entity = MdsRequest.builder()
                .status(MdsRequestStatus.PENDING)
                .market(request.market())
                .problem(request.problem())
                .desiredOutcome(request.desiredOutcome())
                .contextJson(toJson(request.context()))
                .deliveryConstraint(request.deliveryConstraint())
                .evidencePreference(request.evidencePreference())
                .correlationId(request.correlationId())
                .build();

        return toResponse(requestRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<MdsRequestStatusResponse> pendingRequests() {
        return requestRepository.findTop50ByStatusOrderByCreatedAtAsc(MdsRequestStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public MdsRequestStatusResponse claim(Long id, MdsClaimRequest claimRequest) {
        MdsRequest request = findRequest(id);
        if (request.getStatus() != MdsRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "request is not pending");
        }
        request.setStatus(MdsRequestStatus.IN_PROGRESS);
        request.setStartedAt(Instant.now());
        persistEvent(request, "orchestration", MdsEventType.INFO,
                "request claimed by worker " + claimRequest.workerId(), claimRequest);
        return toResponse(request);
    }

    @Transactional
    public MdsRequestStatusResponse heartbeat(Long id, MdsHeartbeatRequest heartbeatRequest) {
        MdsRequest request = findRequest(id);
        assertInProgress(request, "heartbeat can only be sent for in-progress requests");
        persistEvent(
                request,
                defaultValue(heartbeatRequest.stageName(), "pipeline"),
                MdsEventType.HEARTBEAT,
                defaultValue(heartbeatRequest.message(), "heartbeat received"),
                heartbeatRequest.payload()
        );
        return toResponse(request);
    }

    @Transactional
    public MdsRequestStatusResponse complete(Long id, MdsCompleteRequest completeRequest) {
        MdsRequest request = findRequest(id);
        assertInProgress(request, "request can only be completed from in-progress status");
        request.setStatus(MdsRequestStatus.COMPLETED);
        request.setFinishedAt(Instant.now());
        request.setFailureReason(null);
        persistEvent(request, "orchestration", MdsEventType.INFO,
                defaultValue(completeRequest.message(), "request completed"), completeRequest);
        return toResponse(request);
    }

    @Transactional
    public MdsRequestStatusResponse fail(Long id, MdsFailRequest failRequest) {
        MdsRequest request = findRequest(id);
        assertInProgress(request, "request can only fail from in-progress status");
        request.setStatus(MdsRequestStatus.FAILED);
        request.setFinishedAt(Instant.now());
        request.setFailureReason(failRequest.reason());
        persistEvent(request,
                defaultValue(failRequest.stageName(), "pipeline"),
                MdsEventType.ERROR,
                defaultValue(failRequest.message(), failRequest.reason()),
                failRequest);
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public MdsRequestStatusResponse getById(Long id) {
        return toResponse(findRequest(id));
    }

    private MdsRequest findRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "mds request not found"));
    }

    private void persistEvent(MdsRequest request,
                              String stageName,
                              MdsEventType eventType,
                              String message,
                              Object payload) {
        MdsProcessingEvent event = MdsProcessingEvent.builder()
                .request(request)
                .stageName(stageName)
                .eventType(eventType)
                .message(message)
                .payloadJson(toJson(payload == null ? java.util.Map.of() : payload))
                .build();
        processingEventRepository.save(event);
    }

    private MdsRequestStatusResponse toResponse(MdsRequest request) {
        return new MdsRequestStatusResponse(
                request.getId(),
                request.getStatus(),
                request.getMarket(),
                request.getProblem(),
                request.getDesiredOutcome(),
                request.getCorrelationId(),
                request.getFailureReason(),
                request.getCreatedAt(),
                request.getStartedAt(),
                request.getFinishedAt(),
                request.getUpdatedAt()
        );
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "invalid json payload");
        }
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void assertInProgress(MdsRequest request, String message) {
        if (request.getStatus() != MdsRequestStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }
}
