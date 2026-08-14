package com.marketinghub.videomanagement.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.videomanagement.client.BackendVideoClient;
import com.marketinghub.videomanagement.client.dto.SalesVideoJob;
import com.marketinghub.videomanagement.client.dto.SalesVideoStatus;
import com.marketinghub.videomanagement.exception.BackendIntegrationException;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar que o executor obedece ao bloqueio financeiro do backend. */
class VideoJobProgressReporterTest {
  /** Interrompe o processamento antes da próxima task quando o backend bloqueia o job atual. */
  @Test
  void deveInterromperJobBloqueadoPeloBackend() {
    BackendVideoClient backend = mock(BackendVideoClient.class);
    SalesVideoJob blocked = mock(SalesVideoJob.class);
    when(blocked.status()).thenReturn(SalesVideoStatus.VIDEO_FAILED);
    when(backend.reportProgress(org.mockito.ArgumentMatchers.eq(21105L), any()))
        .thenReturn(blocked);

    VideoJobProgressReporter reporter = new VideoJobProgressReporter(backend, 21105L);

    assertThatThrownBy(
            () ->
                reporter.onProgress(
                    25,
                    SalesVideoStatus.VIDEO_PROCESSING,
                    "Runway aceitou cena 1/3",
                    "{\"eventType\":\"PROVIDER_TASK_ACCEPTED\"}"))
        .isInstanceOf(BackendIntegrationException.class)
        .hasMessageContaining("gate financeiro");
    verify(backend, never()).reportHeartbeat(org.mockito.ArgumentMatchers.eq(21105L), any());
  }
}
