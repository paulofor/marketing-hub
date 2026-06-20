package com.marketinghub.nichocnaev2.execution;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida a classificação operacional de falhas feita no executor NichoCNAE v2. */
class NichoCnaeV2PendingExecutionServiceTest {
    /** Deve bloquear loop quando contrato de entrada inválido chega à etapa de segurança. */
    @Test
    void classifiesInvalidStageInputAsValidationFailureWithoutTechnicalRetry() {
        NichoCnaeV2BackendClient backendClient = mock(NichoCnaeV2BackendClient.class);
        NichoCnaeV2StageDefinitions stageDefinitions = new NichoCnaeV2StageDefinitions();
        NichoCnaeV2PendingExecutionService service = new NichoCnaeV2PendingExecutionService(backendClient, stageDefinitions);
        NichoCnaeV2PendingExecution pending = new NichoCnaeV2PendingExecution(
                "54",
                "job-1",
                "4781400",
                "Comércio varejista de artigos do vestuário e acessórios",
                1L,
                2L,
                1,
                26,
                1,
                false,
                "{\"stage\":\"candidate-generator\"}",
                Map.of());
        when(backendClient.listPending(any())).thenAnswer(invocation -> {
            NichoCnaeV2StageDefinition stage = invocation.getArgument(0);
            return "source-safety-filter".equals(stage.stageCode()) ? List.of(pending) : List.of();
        });
        when(backendClient.parseInput(pending.inputPayload())).thenReturn(Map.of("stage", "candidate-generator"));

        service.processAllPending();

        verify(backendClient).fail(
                any(NichoCnaeV2StageDefinition.class),
                eq(pending),
                any(IllegalArgumentException.class),
                eq("VALIDATION"),
                eq("INVALID_STAGE_INPUT_CONTRACT"));
    }
}
