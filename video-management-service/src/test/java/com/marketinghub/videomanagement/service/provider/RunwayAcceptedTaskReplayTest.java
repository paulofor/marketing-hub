package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.*;
import com.marketinghub.videomanagement.client.dto.*;
import com.marketinghub.videomanagement.service.BoundedVideoSpending;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Responsabilidade: recuperar o ID aceito mesmo quando o callback de progresso se perdeu após a
 * resposta.
 */
class RunwayAcceptedTaskReplayTest {
  /** Usa somente a resposta já preservada, sem novo begin, POST ou upload de clipe. */
  @Test
  void reconstructsAcceptedEventFromSameRequestAndOriginalTaskId() throws Exception {
    var json = new ObjectMapper();
    var backend = mock(BackendVideoClient.class);
    var spending = mock(BoundedVideoSpending.class);
    var assets = mock(VideoAssetClient.class);
    var job = mock(SalesVideoJob.class);
    when(job.id()).thenReturn(981L);
    String started =
        "{\"eventType\":\"PROVIDER_SUBMISSION_STARTED\",\"jobId\":981,\"scene\":1,\"estimatedCredits\":120,\"request\":{\"duration\":10}}";
    when(backend.fetchJobEvents(981L)).thenReturn(List.of(new VideoJobEvent(1L, started)));
    when(spending.replayOnly(981L, "CLIP_1"))
        .thenReturn(
            json.readTree(
                "{\"request\":{\"duration\":10},\"response\":{\"id\":\"original-task-id\"}}"));
    var store = new RunwayRecoveryStore(backend, assets, json, WebClient.builder());
    store.setSpending(spending);
    var recovered = store.load(job);
    assertThat(recovered.submissions().get(1).path("providerTaskId").asText())
        .isEqualTo("original-task-id");
    assertThat(recovered.submissions().get(1).path("estimatedCredits").asInt()).isEqualTo(120);
    verify(backend).reportProgress(eq(981L), any());
    verifyNoInteractions(assets);
    when(spending.replayOnly(981L, "CLIP_1")).thenReturn(null);
    assertThatThrownBy(() -> store.load(job)).hasMessageContaining("resultado incerto");
    verifyNoInteractions(assets);
  }
}
