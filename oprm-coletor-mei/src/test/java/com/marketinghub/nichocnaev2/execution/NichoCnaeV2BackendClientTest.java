package com.marketinghub.nichocnaev2.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida a preservação de detalhes operacionais nos callbacks do backend NichoCNAE v2. */
class NichoCnaeV2BackendClientTest {
    /** Deve registrar ponto de falha e stack trace completo quando a exception não possui mensagem. */
    @Test
    void detailedErrorMessageKeepsFailurePointForNullPointerException() {
        NichoCnaeV2StageDefinition stage = new NichoCnaeV2StageDefinition(
                "source-safety-filter",
                "/api/internal/oprm/nichocnae/v2/source-safety-filter/stage-executions",
                context -> new StageResult("IGNORED", Map.of(), List.of()));
        NichoCnaeV2PendingExecution pending = new NichoCnaeV2PendingExecution(
                "96",
                "nichocnae-v2-candidate-3-job-1",
                "7319002",
                "Promoção de vendas",
                null,
                3L,
                1,
                3,
                1,
                false,
                "{}",
                Map.of());
        RuntimeException exception = npeFromApplicationFrame();

        String detail = NichoCnaeV2BackendClient.detailedErrorMessage(
                stage, pending, exception, "SCHEDULER_PROCESSING_ERROR");

        assertThat(detail)
                .contains("reasonCode=SCHEDULER_PROCESSING_ERROR")
                .contains("stage=source-safety-filter")
                .contains("stageExecutionId=96")
                .contains("jobId=nichocnae-v2-candidate-3-job-1")
                .contains("exception=NullPointerException")
                .contains("failurePoint=com.marketinghub.nichocnaev2.execution.NichoCnaeV2BackendClientTest")
                .contains("java.lang.NullPointerException");
    }

    /** Gera uma NPE real dentro do pacote da aplicação para validar localização do primeiro frame útil. */
    private RuntimeException npeFromApplicationFrame() {
        String value = null;
        return assertThrows(NullPointerException.class, () -> value.length());
    }
}
