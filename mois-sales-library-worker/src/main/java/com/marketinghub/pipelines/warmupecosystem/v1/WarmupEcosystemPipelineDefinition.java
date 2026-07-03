package com.marketinghub.pipelines.warmupecosystem.v1;

import java.util.List;

/** Define o pipeline de aquecimento e ecossistema, focado em canais, sinais externos e pré-venda. */
public final class WarmupEcosystemPipelineDefinition {

    /** Código canônico usado nos contratos e relatórios do Marketing Hub. */
    public static final String CODE = "warmupecosystem.v1";

    private static final List<String> STAGES = List.of(
            "intake",
            "product-understanding",
            "investigation-anchor-builder",
            "warmup-resource-discovery",
            "source-product-match",
            "warmup-signal-extraction",
            "warmup-map-builder",
            "dossier-synthesis");

    private WarmupEcosystemPipelineDefinition() {
    }

    /** Lista as etapas independentes executadas pelo pipeline de aquecimento e ecossistema. */
    public static List<String> stages() {
        return STAGES;
    }
}
