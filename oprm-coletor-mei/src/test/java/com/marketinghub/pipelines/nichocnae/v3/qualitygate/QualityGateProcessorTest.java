package com.marketinghub.pipelines.nichocnae.v3.qualitygate;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida que o gate decide avanço por evidência persistível. */
class QualityGateProcessorTest {
    /** Garante aprovação apenas quando há volume mínimo e fonte rastreável. */
    @Test
    void shouldApproveWhenTasksHaveEnoughEvidence() {
        StageResult result = new QualityGateProcessor().process(new StageContext("job", "9", Map.of("dailyTasks", List.of(
                Map.of("task", "Conferir estoque", "sourceUrl", "https://exemplo.com/1"),
                Map.of("task", "Acompanhar metas", "sourceUrl", "")))));

        assertThat(result.status()).isEqualTo("QUALIDADE_APROVADA");
        assertThat(result.output()).containsEntry("approved", true).containsEntry("nextStageCode", "persona-routine-materializer");
    }
}
