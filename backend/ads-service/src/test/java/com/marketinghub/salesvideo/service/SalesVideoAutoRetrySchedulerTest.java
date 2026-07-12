package com.marketinghub.salesvideo.service;

import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.SalesVideoRetryReason;
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
                .failureDetail("retryable=true;code=PROVIDER_TIMEOUT;message=timeout")
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

    /** Não deve reprocessar automaticamente falha marcada como não retryable pelo worker. */
    @Test
    void shouldSkipNonRetryableFailure() {
        SalesVideoJob job = SalesVideoJob.builder()
                .id(20423L)
                .jobType(SalesVideoJobType.RENDER)
                .status(SalesVideoStatus.VIDEO_FAILED)
                .failureCode("VIDEO_PROVIDER_ERROR")
                .failureDetail("retryable=false;code=VIDEO_PROVIDER_ERROR;message=Nenhum provider configurado para o job")
                .finishedAt(Instant.parse("2026-07-11T23:01:04Z"))
                .build();
        given(jobRepository.findByStatusAndFinishedAtBefore(eq(SalesVideoStatus.VIDEO_FAILED), any()))
                .willReturn(List.of(job));
        SalesVideoAutoRetryScheduler scheduler = new SalesVideoAutoRetryScheduler(
                jobRepository,
                jobService,
                reprocessPolicy,
                true,
                15,
                20);

        scheduler.retryFailedJobs();

        verify(reprocessPolicy, never()).hasAttemptsRemaining(job);
        verify(jobService, never()).retry(eq(20423L), any());
    }

    /** Não deve criar cadeias automáticas a partir de job que já nasceu por AUTO_RECOVERY. */
    @Test
    void shouldSkipAutoRecoveryChildFailure() {
        SalesVideoJob job = SalesVideoJob.builder()
                .id(20424L)
                .jobType(SalesVideoJobType.RENDER)
                .status(SalesVideoStatus.VIDEO_FAILED)
                .failureDetail("retryable=true;code=PROVIDER_TIMEOUT;message=timeout")
                .retryReason(SalesVideoRetryReason.AUTO_RECOVERY)
                .finishedAt(Instant.parse("2026-07-11T23:06:04Z"))
                .build();
        given(jobRepository.findByStatusAndFinishedAtBefore(eq(SalesVideoStatus.VIDEO_FAILED), any()))
                .willReturn(List.of(job));
        SalesVideoAutoRetryScheduler scheduler = new SalesVideoAutoRetryScheduler(
                jobRepository,
                jobService,
                reprocessPolicy,
                true,
                15,
                20);

        scheduler.retryFailedJobs();

        verify(reprocessPolicy, never()).hasAttemptsRemaining(job);
        verify(jobService, never()).retry(eq(20424L), any());
    }
}
