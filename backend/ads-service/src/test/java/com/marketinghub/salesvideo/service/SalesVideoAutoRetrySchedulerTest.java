package com.marketinghub.salesvideo.service;

import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.SalesVideoStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Responsabilidade: validar o retry automático dos jobs de vídeo. */
@ExtendWith(MockitoExtension.class)
class SalesVideoAutoRetrySchedulerTest {

    @Mock
    private SalesVideoJobRepository jobRepository;

    @Mock
    private SalesVideoJobService jobService;

    @Mock
    private SalesVideoReprocessPolicy reprocessPolicy;

    /** Não deve reprocessar novamente um job que já possui filho de retry. */
    @Test
    void shouldSkipFailedJobWhenRetryChildAlreadyExists() {
        SalesVideoJob job = SalesVideoJob.builder()
                .id(10108L)
                .jobType(SalesVideoJobType.RENDER)
                .status(SalesVideoStatus.VIDEO_FAILED)
                .finishedAt(Instant.parse("2026-07-10T03:20:47Z"))
                .build();
        given(jobRepository.findByStatusAndFinishedAtBefore(eq(SalesVideoStatus.VIDEO_FAILED), any()))
                .willReturn(List.of(job));
        given(reprocessPolicy.hasAttemptsRemaining(job)).willReturn(true);
        given(jobRepository.existsByRetryOfJob_Id(10108L)).willReturn(true);
        SalesVideoAutoRetryScheduler scheduler = new SalesVideoAutoRetryScheduler(
                jobRepository,
                jobService,
                reprocessPolicy,
                true,
                15,
                20);

        scheduler.retryFailedJobs();

        verify(jobService, never()).retry(eq(10108L), any());
    }
}
