package com.marketinghub.pipelines.nichocnae.v3.sourcesearcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida a decisão de avanço da etapa source-searcher no pipeline NichoCNAE v3. */
class SourceSearcherProcessorTest {
    /** Bloqueia avanço quando existem apenas queries planejadas, mas nenhuma fonte real auditável. */
    @Test
    void shouldBlockWithoutRealSources() {
        StageResult result = new SourceSearcherProcessor().process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of("query", "gerente loja rotina estoque")))));

        assertThat(result.status()).isEqualTo("FONTES_NAO_COLETADAS");
        assertThat(result.output()).containsEntry("nextStageCode", "");
        assertThat(result.output()).containsEntry("blocked", true);
        assertThat(result.output()).containsEntry("recommendedCorrectionStage", "source-searcher");
        assertThat((List<?>) result.output().get("foundSources")).isEmpty();
    }

    /** Permite avanço somente quando a entrada traz fontes reais para o source-fetcher coletar. */
    @Test
    void shouldAdvanceWithRealFoundSources() {
        StageResult result = new SourceSearcherProcessor().process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of("query", "gerente loja rotina estoque")),
                "foundSources", List.of(Map.of(
                        "url", "https://exemplo.com/rotina-loja",
                        "title", "Rotina de loja",
                        "snippet", "Gerente acompanha estoque, metas e atendimento diariamente.")))));

        assertThat(result.status()).isEqualTo("FONTES_ENCONTRADAS");
        assertThat(result.output()).containsEntry("nextStageCode", "source-fetcher");
        assertThat(result.output()).containsEntry("blocked", false);
        assertThat(result.output()).containsEntry("foundSourceCount", 1);
        assertThat((List<?>) result.output().get("foundSources")).hasSize(1);
    }
}
