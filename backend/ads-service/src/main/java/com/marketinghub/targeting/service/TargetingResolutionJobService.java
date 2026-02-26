package com.marketinghub.targeting.service;

import com.marketinghub.targeting.TargetingCandidate;
import com.marketinghub.targeting.TargetingResolutionJob;
import com.marketinghub.targeting.TargetingResolutionJobStatus;
import com.marketinghub.targeting.TargetingRequest;
import com.marketinghub.targeting.repository.TargetingResolutionJobRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TargetingResolutionJobService {
    private static final Logger log = LoggerFactory.getLogger(TargetingResolutionJobService.class);

    private final TargetingResolutionJobRepository repository;
    private final EntityManager entityManager;

    public TargetingResolutionJobService(TargetingResolutionJobRepository repository,
                                         EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public void enqueueAfterCommit(TargetingRequest request, Collection<TargetingCandidate> candidates) {
        if (request == null || request.getId() == null || CollectionUtils.isEmpty(candidates)) {
            return;
        }
        UUID requestId = request.getId();
        List<Long> candidateIds = candidates.stream()
                .map(TargetingCandidate::getId)
                .filter(Objects::nonNull)
                .toList();
        if (CollectionUtils.isEmpty(candidateIds)) {
            return;
        }
        Runnable action = () -> enqueueInternal(requestId, candidateIds);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    @Transactional
    protected void enqueueInternal(UUID requestId, Collection<Long> candidateIds) {
        if (requestId == null || CollectionUtils.isEmpty(candidateIds)) {
            return;
        }
        TargetingRequest requestRef;
        try {
            requestRef = entityManager.getReference(TargetingRequest.class, requestId);
        } catch (EntityNotFoundException ex) {
            log.warn("Targeting request {} not found when scheduling Meta resolution", requestId);
            return;
        }
        for (Long candidateId : candidateIds) {
            if (candidateId == null) {
                continue;
            }
            TargetingCandidate candidateRef;
            try {
                candidateRef = entityManager.getReference(TargetingCandidate.class, candidateId);
            } catch (EntityNotFoundException ex) {
                log.warn("Targeting candidate {} not found when scheduling Meta resolution", candidateId);
                continue;
            }
            TargetingResolutionJob job = repository.findByCandidateId(candidateId)
                    .orElse(TargetingResolutionJob.builder().candidate(candidateRef).request(requestRef).build());
            job.setRequest(requestRef);
            job.setCandidate(candidateRef);
            job.setStatus(TargetingResolutionJobStatus.PENDING);
            job.setAttemptCount(0);
            job.setResultCount(null);
            job.setLastError(null);
            job.setLockedAt(null);
            job.setLockedBy(null);
            job.setStartedAt(null);
            job.setFinishedAt(null);
            repository.save(job);
            log.debug("Enqueued candidate {} for Meta Ads resolution", candidateId);
        }
    }

    public Map<UUID, TargetingResolutionSummary> summarizeByRequestIds(List<UUID> requestIds) {
        if (CollectionUtils.isEmpty(requestIds)) {
            return Collections.emptyMap();
        }
        List<TargetingResolutionJob> jobs = repository.findByRequestIdIn(requestIds);
        Map<UUID, List<TargetingResolutionJob>> grouped = jobs.stream()
                .filter(job -> job.getRequest() != null && job.getRequest().getId() != null)
                .collect(Collectors.groupingBy(job -> job.getRequest().getId()));
        Map<UUID, TargetingResolutionSummary> summaries = new HashMap<>();
        for (UUID requestId : requestIds) {
            List<TargetingResolutionJob> requestJobs = grouped.getOrDefault(requestId, List.of());
            summaries.put(requestId, summarize(requestJobs));
        }
        return summaries;
    }

    private TargetingResolutionSummary summarize(List<TargetingResolutionJob> jobs) {
        if (CollectionUtils.isEmpty(jobs)) {
            return TargetingResolutionSummary.empty();
        }
        int pending = 0;
        int processing = 0;
        int succeeded = 0;
        int failed = 0;
        Instant lastAttempt = null;
        Instant lastCompleted = null;
        Instant lastErrorAt = null;
        String lastError = null;
        for (TargetingResolutionJob job : jobs) {
            TargetingResolutionJobStatus status = job.getStatus();
            if (status == null) {
                pending++;
            } else {
                switch (status) {
                    case PROCESSING -> processing++;
                    case SUCCEEDED -> succeeded++;
                    case FAILED -> failed++;
                    case PENDING -> pending++;
                }
            }
            if (job.getStartedAt() != null && (lastAttempt == null || job.getStartedAt().isAfter(lastAttempt))) {
                lastAttempt = job.getStartedAt();
            }
            if (job.getFinishedAt() != null && (lastCompleted == null || job.getFinishedAt().isAfter(lastCompleted))) {
                lastCompleted = job.getFinishedAt();
            }
            if (status == TargetingResolutionJobStatus.FAILED && job.getLastError() != null) {
                Instant updatedAt = job.getUpdatedAt();
                if (updatedAt == null || lastErrorAt == null || updatedAt.isAfter(lastErrorAt)) {
                    lastErrorAt = updatedAt;
                    lastError = job.getLastError();
                }
            }
        }
        return new TargetingResolutionSummary(pending, processing, succeeded, failed, lastAttempt, lastCompleted, lastError);
    }

    public record TargetingResolutionSummary(
            int pending,
            int processing,
            int succeeded,
            int failed,
            Instant lastAttemptAt,
            Instant lastCompletedAt,
            String lastError
    ) {
        public static TargetingResolutionSummary empty() {
            return new TargetingResolutionSummary(0, 0, 0, 0, null, null, null);
        }
    }
}
