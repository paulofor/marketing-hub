package com.marketinghub.targeting.service;

import com.marketinghub.targeting.TargetingRequest;
import com.marketinghub.targeting.TargetingResolutionJob;
import com.marketinghub.targeting.TargetingResolutionJobStatus;
import com.marketinghub.targeting.repository.TargetingResolutionJobRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TargetingResolutionJobServiceTest {

    @Mock
    private TargetingResolutionJobRepository repository;

    @Mock
    private EntityManager entityManager;

    @Test
    void summarizeByRequestIdsShouldReturnLatestFinishedAtAsLastCompletedAt() {
        TargetingResolutionJobService service = new TargetingResolutionJobService(repository, entityManager);
        UUID requestId = UUID.randomUUID();
        Instant firstFinish = Instant.parse("2026-01-01T10:00:00Z");
        Instant latestFinish = Instant.parse("2026-01-01T12:00:00Z");

        TargetingRequest request = TargetingRequest.builder().id(requestId).descricao("desc").build();
        TargetingResolutionJob first = TargetingResolutionJob.builder()
                .request(request)
                .status(TargetingResolutionJobStatus.SUCCEEDED)
                .finishedAt(firstFinish)
                .build();
        TargetingResolutionJob second = TargetingResolutionJob.builder()
                .request(request)
                .status(TargetingResolutionJobStatus.FAILED)
                .finishedAt(latestFinish)
                .lastError("timeout")
                .build();

        when(repository.findByRequestIdIn(List.of(requestId))).thenReturn(List.of(first, second));

        Map<UUID, TargetingResolutionJobService.TargetingResolutionSummary> summaryMap = service.summarizeByRequestIds(List.of(requestId));

        assertThat(summaryMap).containsKey(requestId);
        assertThat(summaryMap.get(requestId).lastCompletedAt()).isEqualTo(latestFinish);
    }
}
