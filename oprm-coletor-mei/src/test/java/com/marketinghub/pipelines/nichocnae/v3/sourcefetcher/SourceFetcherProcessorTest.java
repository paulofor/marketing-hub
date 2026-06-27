package com.marketinghub.pipelines.nichocnae.v3.sourcefetcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida que a etapa de coleta gera snapshots compatíveis com extração de rotina. */
class SourceFetcherProcessorTest {
    /** Garante que fontes selecionadas viram snapshots auditáveis e rastreáveis. */
    @Test
    void shouldCreateAuditableSourceSnapshots() {
        StageResult result = new SourceFetcherProcessor().process(new StageContext("job", "6", Map.of("selectedSources", List.of(Map.of(
                "url", "https://exemplo.com/estoque", "title", "Controle de estoque", "snippet", "Gerente perde tempo com controle manual de estoque e retrabalho em planilhas.")))));

        assertThat(result.status()).isEqualTo("SNAPSHOTS_COLETADOS");
        assertThat(result.output()).containsEntry("nextStageCode", "routine-signal-extractor");
        assertThat((List<?>) result.output().get("sourceSnapshots")).hasSize(1);
    }
}
