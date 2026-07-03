package com.marketinghub.pipelines.salespagepatterns.v1;

import java.util.List;

/** Define o pipeline de padrões de página de venda, focado em design, visual e copy reutilizáveis. */
public final class SalesPagePatternsPipelineDefinition {

    /** Código canônico usado nos contratos e relatórios do Marketing Hub. */
    public static final String CODE = "salespagepatterns.v1";

    private static final List<String> STAGES = List.of(
            "intake",
            "page-pattern-extraction",
            "pattern-synthesis");

    private SalesPagePatternsPipelineDefinition() {
    }

    /** Lista as etapas independentes planejadas para o pipeline de padrões de página. */
    public static List<String> stages() {
        return STAGES;
    }
}
