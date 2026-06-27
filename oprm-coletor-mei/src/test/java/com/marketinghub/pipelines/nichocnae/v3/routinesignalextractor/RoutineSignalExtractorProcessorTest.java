package com.marketinghub.pipelines.nichocnae.v3.routinesignalextractor;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida que a etapa extrai sinais funcionais dos snapshots coletados. */
class RoutineSignalExtractorProcessorTest {
    /** Garante extração de tarefa, dor e sinal de compra sem saída genérica. */
    @Test
    void shouldExtractRoutineSignalsFromSnapshots() {
        StageResult result = new RoutineSignalExtractorProcessor().process(new StageContext("job", "7", Map.of("sourceSnapshots", List.of(Map.of(
                "snapshotId", "s1", "url", "https://exemplo.com", "evidenceText", "Controle manual de estoque gera retrabalho em planilhas e busca por sistema simples.")))));

        assertThat(result.status()).isEqualTo("SINAIS_EXTRAIDOS");
        assertThat(result.output()).containsEntry("nextStageCode", "daily-tasks-synthesizer");
        @SuppressWarnings("unchecked")
        Map<String, Object> signal = (Map<String, Object>) ((List<?>) result.output().get("routineSignals")).getFirst();
        assertThat(signal).containsEntry("painSignal", "RISCO_DE_ERRO_OU_RETRABALHO").containsEntry("buyingSignal", "PROCURA_FERRAMENTA_OU_MODELO");
    }
}
