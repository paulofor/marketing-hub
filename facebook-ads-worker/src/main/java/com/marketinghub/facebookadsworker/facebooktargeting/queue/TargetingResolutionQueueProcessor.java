package com.marketinghub.facebookadsworker.facebooktargeting.queue;

import com.marketinghub.facebookadsworker.facebooktargeting.TargetingResolutionRequest;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingResolutionResponse;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingResolverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.UUID;

@Component
public class TargetingResolutionQueueProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TargetingResolutionQueueProcessor.class);

    private final TargetingResolutionJobRepository jobRepository;
    private final TargetingResolverService resolverService;
    private final TargetingResolutionQueueProperties properties;
    private final String workerId = UUID.randomUUID().toString();

    public TargetingResolutionQueueProcessor(TargetingResolutionJobRepository jobRepository,
                                             TargetingResolverService resolverService,
                                             TargetingResolutionQueueProperties properties) {
        this.jobRepository = jobRepository;
        this.resolverService = resolverService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "#{@targetingResolutionQueueProperties.pollInterval?.toMillis() ?: 30000}")
    public void pollQueue() {
        if (!properties.isEnabled()) {
            return;
        }
        int released = jobRepository.releaseExpiredLocks(properties.getLockTtl());
        if (released > 0) {
            LOGGER.warn("Released {} stuck targeting jobs due to lock TTL", released);
        }
        List<TargetingResolutionJobRecord> jobs = jobRepository.claimPendingJobs(workerId, properties.getBatchSize());
        if (CollectionUtils.isEmpty(jobs)) {
            return;
        }
        LOGGER.info("Processing {} targeting candidates via database queue", jobs.size());
        for (TargetingResolutionJobRecord job : jobs) {
            processJob(job);
        }
    }

    private void processJob(TargetingResolutionJobRecord job) {
        try {
            TargetingResolutionRequest request = buildRequest(job);
            TargetingResolutionResponse response = resolverService.resolve(job.requestId(), request, job.experimentId());
            TargetingResolutionResponse.CandidateResolutionSummary summary = response.candidates().stream()
                    .filter(candidate -> job.candidateId().equals(candidate.id()))
                    .findFirst()
                    .orElse(null);
            if (summary == null) {
                throw new IllegalStateException("Resolver não retornou resumo para o candidato " + job.candidateId());
            }
            jobRepository.markCompleted(job.jobId(), summary.resolvedOptions());
            LOGGER.info("Job {} resolved with status {} and {} options", job.jobId(), summary.status(), summary.resolvedOptions());
        } catch (Exception ex) {
            jobRepository.markFailed(job.jobId(), ex.getMessage());
            LOGGER.error("Failed to resolve targeting candidate {}: {}", job.candidateId(), ex.getMessage(), ex);
        }
    }

    private TargetingResolutionRequest buildRequest(TargetingResolutionJobRecord job) {
        TargetingResolutionRequest request = new TargetingResolutionRequest();
        request.setAdAccountId(job.requestAdAccountId());
        request.setLocale(job.requestLocale());
        request.setCountry(job.requestCountry());
        request.setCandidates(List.of(job.toPayload()));
        return request;
    }
}
