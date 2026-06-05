package com.marketinghub.nichocnae.routineresearchorchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marketinghub.nichocnae.pipeline.NoopArtifactStore;
import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageExecution;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida o processor da etapa zero nichocnae sem chamadas reais ao backend. */
@ExtendWith(MockitoExtension.class)
class RoutineResearchOrchestratorProcessorTest {
    @Mock private RoutineResearchOrchestratorBackendClient backendClient;

    /** Garante que a etapa zero propaga o ciclo iniciado e registra métrica de início. */
    @Test
    void shouldRunNextAndReturnStartedMetric() {
        RoutineResearchOrchestratorOutput backendOutput = new RoutineResearchOrchestratorOutput(
                true,
                1001L,
                55L,
                "9602501",
                "Cabeleireiros, manicure e pedicure",
                "Kit Agenda Cheia com IA para manicures",
                BigDecimal.valueOf(92),
                "AUTO_SCORE_QUEUE",
                "RUNNING",
                "Kit Agenda Cheia com IA para manicures",
                "Kit Agenda Cheia com IA para manicures",
                "ROUTINE_REALITY_RESEARCH",
                BigDecimal.ZERO,
                "RESEARCH_RUNNING",
                "Pesquisa de rotina iniciada.");
        when(backendClient.runNext()).thenReturn(backendOutput);
        RoutineResearchOrchestratorProcessor processor = new RoutineResearchOrchestratorProcessor(backendClient);
        StageExecution<RoutineResearchOrchestratorInput> execution = new StageExecution<>(
                "stage-0", new RoutineResearchOrchestratorInput("TEST"), Map.of());

        StageResult<RoutineResearchOrchestratorOutput> result = processor.process(
                new StageContext<>(execution, execution.input(), new NoopArtifactStore(), execution.config()));

        assertThat(result.output()).isEqualTo(backendOutput);
        assertThat(result.artifacts()).isEmpty();
        assertThat(result.metrics()).containsEntry("started", true);
    }
}
