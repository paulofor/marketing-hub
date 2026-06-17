package com.marketinghub.nichocnae.routinequalitygate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.nichocnae.pipeline.StageContext;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a orquestração externa da etapa sete, incluindo decisão de reprocessamento. */
class RoutineQualityGateProcessorTest {
    private final RoutineQualityGateEngine engine = mock(RoutineQualityGateEngine.class);
    private final RoutineQualityGateBackendClient backendClient = mock(RoutineQualityGateBackendClient.class);
    private final RoutineQualityReprocessPolicy reprocessPolicy = new RoutineQualityReprocessPolicy();
    private final RoutineQualityGateProcessor processor = new RoutineQualityGateProcessor(engine, backendClient, reprocessPolicy);

    /** Deve solicitar novo ciclo ao backend quando o módulo externo reprova qualidade por causa recuperável. */
    @Test
    void shouldRequestReprocessWhenExternalGateRejectsRecoverableStatus() {
        RoutineQualityGatePending pending = pending();
        RoutineQualityDecision decision = decision("SOLUTION_CONTAMINATED", false);
        RoutineQualityGateOutput output = output("SOLUTION_CONTAMINATED", false);
        when(engine.evaluate(pending)).thenReturn(decision);
        when(backendClient.completeStageExecution(pending, decision)).thenReturn(output);

        processor.process(context(pending));

        verify(backendClient).completeStageExecution(pending, decision);
        verify(backendClient).reprocessAfterQualityRejection(output);
    }

    /** Não deve solicitar novo ciclo quando a qualidade foi aprovada para hipótese. */
    @Test
    void shouldNotRequestReprocessWhenGateApprovesQuality() {
        RoutineQualityGatePending pending = pending();
        RoutineQualityDecision decision = decision("MEI_AUDIENCE_READY", true);
        RoutineQualityGateOutput output = output("MEI_AUDIENCE_READY", true);
        when(engine.evaluate(pending)).thenReturn(decision);
        when(backendClient.completeStageExecution(pending, decision)).thenReturn(output);

        processor.process(context(pending));

        verify(backendClient).completeStageExecution(pending, decision);
        verify(backendClient, never()).reprocessAfterQualityRejection(output);
    }

    /** Cria o contexto de etapa com armazenamento inerte para validar somente a orquestração. */
    private StageContext<RoutineQualityGatePending> context(RoutineQualityGatePending pending) {
        return new StageContext<>(
                new com.marketinghub.nichocnae.pipeline.StageExecution<>("job-1", pending, Map.of()),
                pending,
                (artifact, content) -> artifact,
                Map.of());
    }

    /** Cria uma pendência mínima porque o processor não recalcula a decisão diretamente. */
    private RoutineQualityGatePending pending() {
        return new RoutineQualityGatePending(
                10L, 1001L, "Manicure autônoma",
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                Instant.parse("2026-06-17T00:00:00Z"));
    }

    /** Cria uma decisão mínima para o fluxo do processor. */
    private RoutineQualityDecision decision(String status, boolean readyForHypothesis) {
        return new RoutineQualityDecision(status, readyForHypothesis, 70, 60, 10, "status=" + status);
    }

    /** Cria uma saída mínima persistida pelo backend. */
    private RoutineQualityGateOutput output(String status, boolean readyForHypothesis) {
        return new RoutineQualityGateOutput(10L, 1001L, status, status, readyForHypothesis, 70, 60, 10, Instant.now());
    }
}
