package com.marketinghub.nichocnae.routineresearchcycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marketinghub.nichocnae.pipeline.NoopArtifactStore;
import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageExecution;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida o processor da etapa um nichocnae sem chamadas reais ao backend. */
@ExtendWith(MockitoExtension.class)
class RoutineResearchCycleProcessorTest {
    @Mock private RoutineResearchCycleBackendClient backendClient;

    /** Garante que a etapa um detalha o ciclo em execução e registra métricas de controle. */
    @Test
    void shouldDetailCycleAndReturnControlMetrics() {
        RoutineResearchCyclePending pending = new RoutineResearchCyclePending(
                1001L,
                55L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Kit Agenda Cheia com IA para manicures",
                BigDecimal.valueOf(92),
                "AUTO_SCORE_QUEUE",
                "RUNNING",
                Instant.parse("2026-06-02T03:00:00Z"),
                Instant.parse("2026-06-02T03:00:00Z"));
        RoutineResearchCycleDetail detail = new RoutineResearchCycleDetail(
                1001L,
                55L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Kit Agenda Cheia com IA para manicures",
                BigDecimal.valueOf(92),
                "RUNNING",
                0,
                0,
                0,
                0,
                Instant.parse("2026-06-02T03:00:00Z"),
                null,
                null);
        when(backendClient.detailStageExecution(1001L)).thenReturn(detail);
        RoutineResearchCycleProcessor processor = new RoutineResearchCycleProcessor(backendClient);
        StageExecution<RoutineResearchCyclePending> execution = new StageExecution<>("stage-1", pending, Map.of());

        StageResult<RoutineResearchCycleDetail> result = processor.process(
                new StageContext<>(execution, execution.input(), new NoopArtifactStore(), execution.config()));

        assertThat(result.output()).isEqualTo(detail);
        assertThat(result.artifacts()).isEmpty();
        assertThat(result.metrics())
                .containsEntry("researchCycleId", 1001L)
                .containsEntry("status", "RUNNING")
                .containsEntry("totalExtractedSignals", 0);
    }
}
