package com.marketinghub.pipelines.salespagepatterns.v1.pagepatternextraction;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.pipelines.dossie.v1.StageContext;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida a execução local rastreável da etapa de extração de padrões de página. */
class SalesPagePatternsPagePatternExtractionProcessorTest {

    /** Garante saída funcional mínima quando a OpenAI não está configurada no ambiente de teste. */
    @Test
    void deveGerarSaidaLocalEstruturada() {
        SalesPagePatternsPagePatternExtractionProcessor processor = new SalesPagePatternsPagePatternExtractionProcessor();
        StageContext context = new StageContext(31L, 401L, "workspace-mois", "page-pattern-extraction", Map.of(
                "headline", "Promessa clara",
                "proofSummary", "Prova social",
                "offerSummary", "Oferta com garantia"));

        StageResult result = processor.process(context);

        assertThat(result.status()).isEqualTo("DONE");
        assertThat(result.output()).containsKey("page-pattern-extraction");
        assertThat(String.valueOf(result.output())).contains("padrões abstratos");
        assertThat(result.artifacts()).hasSize(1);
        assertThat(result.errorMessage()).isNull();
    }
}
