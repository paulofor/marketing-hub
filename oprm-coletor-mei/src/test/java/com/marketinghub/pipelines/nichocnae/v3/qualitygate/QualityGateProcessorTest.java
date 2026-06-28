package com.marketinghub.pipelines.nichocnae.v3.qualitygate;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida que o gate decide avanço por evidência persistível. */
class QualityGateProcessorTest {
    /** Garante aprovação quando há volume mínimo, fonte rastreável e contexto comercial cotidiano. */
    @Test
    void shouldApproveWhenTasksHaveEnoughEvidence() {
        StageResult result = new QualityGateProcessor().process(new StageContext("job", "9", Map.of("dailyTasks", List.of(
                Map.of("task", "Conferir estoque do cliente no WhatsApp", "sourceUrl", "https://exemplo.com/1", "channelContext", "whatsapp", "evidenceText", "cliente pediu tamanho pelo WhatsApp"),
                Map.of("task", "Acompanhar agenda de cobrança", "sourceUrl", "", "channelContext", "agenda", "evidenceText", "agenda e cobrança recorrente")))));

        assertThat(result.status()).isEqualTo("QUALIDADE_APROVADA");
        assertThat(result.output()).containsEntry("approved", true).containsEntry("nextStageCode", "persona-routine-materializer");
    }
}
