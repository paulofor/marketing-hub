package com.marketinghub.oprm.service;

import com.marketinghub.oprm.OprmJob;
import com.marketinghub.oprm.OprmJobEvent;
import com.marketinghub.oprm.OprmJobInput;
import com.marketinghub.oprm.OprmJobStatus;
import com.marketinghub.oprm.dto.OprmCreateJobRequestDto;
import com.marketinghub.oprm.dto.OprmJobClaimRequestDto;
import com.marketinghub.oprm.dto.OprmJobClaimResponseDto;
import com.marketinghub.oprm.dto.OprmJobDetailResponseDto;
import com.marketinghub.oprm.dto.OprmJobStatusUpdateRequestDto;
import com.marketinghub.oprm.dto.OprmWorkspaceOccupationSummaryDto;
import com.marketinghub.oprm.repository.OprmJobEventRepository;
import com.marketinghub.oprm.repository.OprmJobInputRepository;
import com.marketinghub.oprm.repository.OprmJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OprmJobOrchestrationService {
    private final OprmJobRepository jobRepository;
    private final OprmJobInputRepository jobInputRepository;
    private final OprmJobEventRepository jobEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OprmJobDetailResponseDto createJob(OprmCreateJobRequestDto request) {
        OprmJob job = OprmJob.builder()
                .jobType(request.jobType())
                .jobStatus(OprmJobStatus.PENDING)
                .occupationSeedRef(request.occupationSeedRef())
                .correlationId(resolveCorrelationId(request.correlationId()))
                .build();
        OprmJob saved = jobRepository.save(job);

        if (request.inputRefs() != null) {
            request.inputRefs().stream()
                    .filter(inputRef -> inputRef != null && !inputRef.isBlank())
                    .map(inputRef -> OprmJobInput.builder()
                            .job(saved)
                            .inputRef(inputRef)
                            .build())
                    .forEach(jobInputRepository::save);
        }

        jobEventRepository.save(OprmJobEvent.builder()
                .job(saved)
                .eventStatus(OprmJobStatus.PENDING)
                .phase("created")
                .message("job created via backend orchestration")
                .occurredAt(Instant.now())
                .build());

        return toJobDetail(saved, jobInputRepository.findByJobIdOrderByCreatedAtAsc(saved.getId()));
    }

    @Transactional
    public Optional<OprmJobClaimResponseDto> claimNextJob(OprmJobClaimRequestDto request) {
        Optional<UUID> nextJobId = jobRepository.findNextPendingJobId();
        if (nextJobId.isEmpty()) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        Instant leaseExpiresAt = now.plusSeconds(request.leaseSeconds());
        int updated = jobRepository.claimPendingJob(
                nextJobId.get(),
                request.workerId(),
                now,
                leaseExpiresAt,
                OprmJobStatus.PENDING,
                OprmJobStatus.CLAIMED
        );

        if (updated == 0) {
            return Optional.empty();
        }

        OprmJob job = jobRepository.findById(nextJobId.get())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OPRM job not found after claim"));

        jobEventRepository.save(OprmJobEvent.builder()
                .job(job)
                .eventStatus(OprmJobStatus.CLAIMED)
                .workerId(request.workerId())
                .phase("claim")
                .message("job claimed by worker")
                .occurredAt(now)
                .build());

        return Optional.of(new OprmJobClaimResponseDto(
                job.getId().toString(),
                job.getJobType(),
                job.getOccupationSeedRef(),
                job.getCorrelationId(),
                Map.of(),
                toIso(job.getClaimedAt()),
                toIso(job.getLeaseExpiresAt())
        ));
    }

    @Transactional(readOnly = true)
    public OprmJobDetailResponseDto getJobDetail(UUID jobId) {
        OprmJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OPRM job not found"));
        return toJobDetail(job, jobInputRepository.findByJobIdOrderByCreatedAtAsc(jobId));
    }

    @Transactional(readOnly = true)
    public List<OprmWorkspaceOccupationSummaryDto> listWorkspaceOccupations() {
        Map<String, OprmJob> latestJobByOccupation = new LinkedHashMap<>();
        for (OprmJob job : repository.findTop500ByOrderByCreatedAtDesc()) {
            latestJobByOccupation.putIfAbsent(job.getOccupationSeedRef(), job);
        }

        return latestJobByOccupation.values()
                .stream()
                .map(job -> new OprmWorkspaceOccupationSummaryDto(
                        job.getOccupationSeedRef(),
                        job.getJobStatus(),
                        job.getCorrelationId(),
                        job.getUpdatedAt().toString()
                ))
                .toList();
    }

    @Transactional
    public void updateJobStatus(UUID jobId, OprmJobStatusUpdateRequestDto request) {
        OprmJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OPRM job not found"));

        OprmJobStatus requestedStatus = request.status();
        if (!isTransitionAllowed(job.getJobStatus(), requestedStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "invalid OPRM job status transition: " + job.getJobStatus() + " -> " + requestedStatus);
        }

        job.setJobStatus(requestedStatus);
        if (requestedStatus == OprmJobStatus.RUNNING && job.getStartedAt() == null) {
            job.setStartedAt(parseOccurredAt(request.occurredAt()));
        }

        if (isTerminal(requestedStatus)) {
            job.setFinishedAt(parseOccurredAt(request.occurredAt()));
            job.setLeaseExpiresAt(null);
        }

        if (request.errorCode() != null && !request.errorCode().isBlank()) {
            job.setErrorCode(request.errorCode());
        }
        if (request.errorMessage() != null && !request.errorMessage().isBlank()) {
            job.setErrorMessage(request.errorMessage());
        }

        jobRepository.save(job);

        jobEventRepository.save(OprmJobEvent.builder()
                .job(job)
                .eventStatus(requestedStatus)
                .phase(request.phase())
                .message(mergeMessageWithMetrics(request.message(), request.metrics()))
                .workerId(request.workerId())
                .occurredAt(parseOccurredAt(request.occurredAt()))
                .build());
    }

    private OprmJobDetailResponseDto toJobDetail(OprmJob job, List<OprmJobInput> inputs) {
        return new OprmJobDetailResponseDto(
                job.getId().toString(),
                job.getJobType(),
                job.getJobStatus(),
                job.getOccupationSeedRef(),
                job.getCorrelationId(),
                job.getAttemptCount(),
                toIso(job.getCreatedAt()),
                toIso(job.getClaimedAt()),
                toIso(job.getStartedAt()),
                toIso(job.getFinishedAt()),
                Map.of(),
                inputs.stream().map(OprmJobInput::getInputRef).toList(),
                job.getErrorCode(),
                job.getErrorMessage()
        );
    }

    private boolean isTransitionAllowed(OprmJobStatus current, OprmJobStatus next) {
        if (current == next) {
            return true;
        }
        return switch (current) {
            case PENDING -> next == OprmJobStatus.CLAIMED || next == OprmJobStatus.CANCELLED;
            case CLAIMED -> next == OprmJobStatus.RUNNING || next == OprmJobStatus.RETRY_WAIT || next == OprmJobStatus.CANCELLED;
            case RUNNING -> next == OprmJobStatus.SUCCEEDED || next == OprmJobStatus.FAILED || next == OprmJobStatus.RETRY_WAIT;
            case RETRY_WAIT -> next == OprmJobStatus.PENDING || next == OprmJobStatus.CANCELLED;
            case SUCCEEDED, FAILED, CANCELLED -> false;
        };
    }

    private boolean isTerminal(OprmJobStatus status) {
        return status == OprmJobStatus.SUCCEEDED || status == OprmJobStatus.FAILED || status == OprmJobStatus.CANCELLED;
    }

    private String resolveCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private Instant parseOccurredAt(String occurredAt) {
        try {
            return Instant.parse(occurredAt);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "occurredAt must use ISO-8601 format");
        }
    }

    private String mergeMessageWithMetrics(String message, Map<String, Object> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return message;
        }
        try {
            String metricsJson = objectMapper.writeValueAsString(metrics);
            return (message == null || message.isBlank()) ? metricsJson : message + " | metrics=" + metricsJson;
        } catch (JsonProcessingException e) {
            return message;
        }
    }

    private String toIso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
