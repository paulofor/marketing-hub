package com.marketinghub.pipelines.nichocnae.v3.dailytaskssynthesizer;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida que a síntese transforma sinais em tarefas diárias acionáveis. */
class DailyTasksSynthesizerProcessorTest {
    /** Garante saída com tarefa, dor, evidência e alavanca de facilidade. */
    @Test
    void shouldSynthesizeDailyTasksFromSignals() {
        StageResult result = new DailyTasksSynthesizerProcessor().process(new StageContext("job", "8", Map.of("routineSignals", List.of(Map.of(
                "routineTask", "Conferir estoque", "painSignal", "CONTROLE_OPERACIONAL_MANUAL", "buyingSignal", "PROCURA_FERRAMENTA_OU_MODELO", "sourceUrl", "https://exemplo.com")))));

        assertThat(result.status()).isEqualTo("TAREFAS_DIARIAS_SINTETIZADAS");
        @SuppressWarnings("unchecked")
        Map<String, Object> task = (Map<String, Object>) ((List<?>) result.output().get("dailyTasks")).getFirst();
        assertThat(task).containsEntry("easeLever", "simplificar controle operacional");
    }
}
