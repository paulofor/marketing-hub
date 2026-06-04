package com.marketinghub.nichocnae.signalextractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageExecution;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a orquestração da etapa cinco entre extrator local, backend e StageProcessor. */
class SignalExtractorProcessorTest {

    /** Deve extrair sinais e concluir a etapa cinco no backend com métricas estruturadas. */
    @Test
    void shouldExtractSignalsAndCompleteSignalExtractorOutput() {
        SignalExtractorEngine engine = mock(SignalExtractorEngine.class);
        SignalExtractorBackendClient backendClient = mock(SignalExtractorBackendClient.class);
        SignalExtractorProcessor processor = new SignalExtractorProcessor(engine, backendClient);
        SignalExtractorPending pending = pending();
        List<ExtractedSignal> signals = List.of(new ExtractedSignal("PAIN_POINT", "Faltas na agenda", "faltas na agenda", 85));
        SignalExtractorOutput output = output();
        when(engine.extract(pending)).thenReturn(signals);
        when(backendClient.completeStageExecution(pending, signals)).thenReturn(output);

        var result = processor.process(new StageContext<>(
                new StageExecution<>("job-1", pending, Map.of()),
                pending,
                (artifact, content) -> artifact,
                Map.of()));

        assertThat(result.output()).isEqualTo(output);
        assertThat(result.metrics())
                .containsEntry("sourceSnapshotId", 9001L)
                .containsEntry("researchCycleId", 1001L)
                .containsEntry("extractedSignalCount", 1)
                .containsEntry("cycleTotalExtractedSignals", 5);
        verify(backendClient).completeStageExecution(pending, signals);
    }

    /** Cria uma pendência mínima para a etapa cinco. */
    private SignalExtractorPending pending() {
        return new SignalExtractorPending(
                9001L,
                1001L,
                301L,
                "https://exemplo.com/fonte",
                "exemplo.com",
                "Como organizar agenda",
                "PUBLIC_CONTENT",
                "Resumo público da fonte",
                "Trecho curto permitido para extração de sinais.",
                Instant.parse("2026-06-04T00:00:00Z"),
                "COMPLETED",
                200,
                "SHORT_EXCERPT_ONLY",
                "PUBLIC_SNIPPET",
                "PENDING",
                Instant.parse("2026-06-04T00:00:00Z"));
    }

    /** Cria uma saída mínima suficiente para validar o processor. */
    private SignalExtractorOutput output() {
        return new SignalExtractorOutput(
                9001L,
                1001L,
                "COMPLETED",
                1,
                5,
                List.of(new ExtractedSignalResponse(
                        7001L,
                        1001L,
                        9001L,
                        301L,
                        "PAIN_POINT",
                        "Faltas na agenda",
                        "faltas na agenda",
                        "exemplo.com",
                        85,
                        "test",
                        Instant.parse("2026-06-04T00:01:00Z"))));
    }
}
