package com.marketinghub.facebookadsworker.facebooktargeting.queue;

import com.marketinghub.facebookadsworker.facebooktargeting.TargetingCandidateStatus;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingCandidateType;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingResolutionRequest;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingResolutionResponse;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingResolverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TargetingResolutionQueueProcessorTest {
    @Mock
    private TargetingResolutionJobRepository jobRepository;

    @Mock
    private TargetingResolverService resolverService;

    private TargetingResolutionQueueProperties properties;
    private TargetingResolutionQueueProcessor processor;

    @BeforeEach
    void setUp() {
        properties = new TargetingResolutionQueueProperties();
        properties.setEnabled(true);
        processor = new TargetingResolutionQueueProcessor(jobRepository, resolverService, properties);
    }

    @Test
    void pollQueueProcessesJobAndMarksCompletion() {
        TargetingResolutionJobRecord job = sampleJob();
        when(jobRepository.claimPendingJobs(anyString(), anyInt())).thenReturn(List.of(job));
        when(resolverService.resolve(any(UUID.class), any(TargetingResolutionRequest.class)))
            .thenReturn(new TargetingResolutionResponse(
                job.requestId(),
                List.of(new TargetingResolutionResponse.CandidateResolutionSummary(
                    job.candidateId(), TargetingCandidateStatus.VALIDATED, 2, "ok"
                ))
            ));

        processor.pollQueue();

        verify(jobRepository).releaseExpiredLocks(properties.getLockTtl());
        verify(jobRepository).markCompleted(job.jobId(), 2);
        ArgumentCaptor<TargetingResolutionRequest> captor = ArgumentCaptor.forClass(TargetingResolutionRequest.class);
        verify(resolverService).resolve(any(UUID.class), captor.capture());
        assertThat(captor.getValue().getAdAccountId()).isEqualTo("act_987654321");
        assertThat(captor.getValue().getCandidates()).hasSize(1);
        assertThat(captor.getValue().getCandidates().get(0).seed()).isEqualTo("Pilates");
    }

    @Test
    void pollQueueMarksFailureWhenResolverThrows() {
        TargetingResolutionJobRecord job = sampleJob();
        when(jobRepository.claimPendingJobs(anyString(), anyInt())).thenReturn(List.of(job));
        when(resolverService.resolve(any(UUID.class), any(TargetingResolutionRequest.class)))
            .thenThrow(new IllegalStateException("backend offline"));

        processor.pollQueue();

        verify(jobRepository).markFailed(job.jobId(), "backend offline");
    }

    @Test
    void pollQueueSkipsWhenDisabled() {
        properties.setEnabled(false);
        processor.pollQueue();
        verifyNoInteractions(jobRepository);
        verifyNoInteractions(resolverService);
    }

    private TargetingResolutionJobRecord sampleJob() {
        return new TargetingResolutionJobRecord(
            10L,
            UUID.randomUUID(),
            "act_987654321",
            "pt_BR",
            "BR",
            55L,
            "Pilates",
            List.of("Pilates"),
            TargetingCandidateType.INTEREST,
            "pt_BR",
            "pt_BR",
            "BR",
            "AI",
            BigDecimal.valueOf(0.8),
            "Seed",
            null
        );
    }
}
