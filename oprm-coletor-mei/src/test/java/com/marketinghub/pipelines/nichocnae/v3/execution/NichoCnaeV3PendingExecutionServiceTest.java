package com.marketinghub.pipelines.nichocnae.v3.execution;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Valida proteções operacionais do executor de pendências NichoCNAE v3. */
class NichoCnaeV3PendingExecutionServiceTest {
    /** Cancela source-searcher demorado, registra falha no backend e libera a varredura. */
    @Test
    void shouldFailSourceSearcherWhenJobExceedsDurationLimit() {
        NichoCnaeV3BackendClient backendClient = org.mockito.Mockito.mock(NichoCnaeV3BackendClient.class);
        NichoCnaeV3StageDefinitions stageDefinitions = org.mockito.Mockito.mock(NichoCnaeV3StageDefinitions.class);
        NichoCnaeV3StageDefinition sourceSearcher = new NichoCnaeV3StageDefinition("source-searcher", "/source-searcher", context -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return new StageResult("LENTO", Map.of(), List.of());
        });
        NichoCnaeV3PendingExecution pending = new NichoCnaeV3PendingExecution("17", "job-antigo", "8219999", "{}", Map.of());
        when(stageDefinitions.all()).thenReturn(List.of(sourceSearcher));
        when(backendClient.listPending(sourceSearcher)).thenReturn(List.of(pending));
        when(backendClient.parseInput("{}")).thenReturn(new LinkedHashMap<>());

        new NichoCnaeV3PendingExecutionService(backendClient, stageDefinitions, 25).processAllPending();

        ArgumentCaptor<RuntimeException> errorCaptor = ArgumentCaptor.forClass(RuntimeException.class);
        verify(backendClient).fail(any(), any(), errorCaptor.capture());
        verify(backendClient, never()).complete(any(), any(), any());
        assertThat(errorCaptor.getValue().getMessage())
                .contains("source-searcher excedeu 25ms")
                .contains("liberar a fila");
    }
}
