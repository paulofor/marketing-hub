package com.marketinghub.targeting.service;

import com.marketinghub.targeting.TargetingCandidate;
import com.marketinghub.targeting.TargetingResolutionJob;
import com.marketinghub.targeting.TargetingResolutionJobStatus;
import com.marketinghub.targeting.TargetingRequest;
import com.marketinghub.repository.jpa.targeting.TargetingResolutionJobRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
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

/**
 * Gerencia a fila persistida de resolução de candidatos de público pelo Facebook Ads Worker.
 */
@Service
public class TargetingResolutionJobService {
    private static final Logger log = LoggerFactory.getLogger(TargetingResolutionJobService.class);

    private final TargetingResolutionJobRepository repository;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    /**
     * Inicializa o serviço com persistência JPA e transação explícita para enfileiramento pós-commit.
     */
    public TargetingResolutionJobService(TargetingResolutionJobRepository repository,
                                         EntityManager entityManager,
                                         PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Agenda a criação dos jobs de resolução Meta somente depois que os candidatos forem gravados.
     */
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
        Runnable action = () -> runEnqueueInNewTransaction(requestId, candidateIds);
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

    /**
     * Executa o enfileiramento em transação própria para não depender da transação que acabou de commitar.
     */
    private void runEnqueueInNewTransaction(UUID requestId, Collection<Long> candidateIds) {
        try {
            transactionTemplate.executeWithoutResult(status -> enqueueInternal(requestId, candidateIds));
        } catch (RuntimeException ex) {
            log.error("Failed to enqueue targeting candidates for Meta resolution: requestId={}, candidateIds={}", requestId, candidateIds, ex);
            throw ex;
        }
    }

    /**
     * Persiste ou reinicia os jobs pendentes consumidos pelo Facebook Ads Worker.
     */
    private void enqueueInternal(UUID requestId, Collection<Long> candidateIds) {
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

    /**
     * Resume a situação operacional dos jobs por solicitação de targeting.
     */
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

    /**
     * Consolida contadores, última tentativa, última conclusão e último erro de uma lista de jobs.
     */
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

    /**
     * Representa o resumo operacional da fila de resolução Meta para uma solicitação.
     */
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
