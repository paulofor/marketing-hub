package com.marketinghub.targeting.service;

import com.marketinghub.targeting.TargetingCandidate;
import com.marketinghub.targeting.TargetingCandidateStatus;
import com.marketinghub.targeting.TargetingRequest;
import com.marketinghub.targeting.integration.TargetingResolverClient;
import com.marketinghub.targeting.integration.TargetingResolverIntegrationProperties;
import com.marketinghub.targeting.repository.TargetingRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TargetingResolutionRetryScheduler {
    private static final Logger log = LoggerFactory.getLogger(TargetingResolutionRetryScheduler.class);

    private final TargetingRequestRepository requestRepository;
    private final TargetingResolverClient targetingResolverClient;
    private final TargetingResolverIntegrationProperties properties;
    private final int retryLimit;

    public TargetingResolutionRetryScheduler(TargetingRequestRepository requestRepository,
                                             TargetingResolverClient targetingResolverClient,
                                             TargetingResolverIntegrationProperties properties) {
        this.requestRepository = requestRepository;
        this.targetingResolverClient = targetingResolverClient;
        this.properties = properties;
        int configuredLimit = properties != null ? properties.getRetryLimit() : 25;
        this.retryLimit = configuredLimit > 0 ? configuredLimit : 25;
    }

    @Scheduled(
            initialDelayString = "#{@targetingResolverIntegrationProperties.retryInitialDelay?.toMillis() ?: 60000}",
            fixedDelayString = "#{@targetingResolverIntegrationProperties.retryInterval?.toMillis() ?: 300000}"
    )
    public void retryPendingCandidates() {
        if (!properties.isEnabled()) {
            return;
        }
        List<TargetingRequest> requests = requestRepository.findRequestsWithPendingCandidates(
                PageRequest.of(0, Math.max(retryLimit, 1))
        );
        if (CollectionUtils.isEmpty(requests)) {
            return;
        }
        for (TargetingRequest request : requests) {
            List<TargetingCandidate> pending = resolvePendingCandidates(request);
            if (pending.isEmpty()) {
                continue;
            }
            log.info("Retrying Facebook resolution for request {} ({} pending candidates)",
                    request.getId(), pending.size());
            targetingResolverClient.requestResolution(request, pending);
        }
    }

    private List<TargetingCandidate> resolvePendingCandidates(TargetingRequest request) {
        if (request == null || CollectionUtils.isEmpty(request.getCandidates())) {
            return List.of();
        }
        return request.getCandidates().stream()
                .filter(candidate -> candidate != null && candidate.getStatus() == TargetingCandidateStatus.PENDING_FACEBOOK_MATCH)
                .collect(Collectors.toList());
    }
}
