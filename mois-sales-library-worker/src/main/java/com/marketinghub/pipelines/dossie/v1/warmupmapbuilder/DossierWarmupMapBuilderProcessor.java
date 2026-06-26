package com.marketinghub.pipelines.dossie.v1.warmupmapbuilder;

import com.marketinghub.pipelines.dossie.v1.StageContext;
import com.marketinghub.pipelines.dossie.v1.StageProcessor;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa montagem do mapa de aquecimento sem acoplar o núcleo do pipeline às demais etapas do dossiê. */
public class DossierWarmupMapBuilderProcessor implements StageProcessor {

    /** Informa o nome canônico da etapa montagem do mapa de aquecimento. */
    @Override
    public String stageName() {
        return "warmup-map-builder";
    }

    /** Produz saída estruturada mínima para auditoria e evolução incremental da etapa. */
    @Override
    public StageResult process(StageContext context) {
        DossierWarmupMapBuilderOutput output = new DossierWarmupMapBuilderOutput(
                context.dossierId(),
                "READY_FOR_IMPLEMENTATION",
                "Organizar os recursos externos que aquecem o público antes da compra."
        );
        return StageResult.done(Map.of("warmup-map-builder", output), List.of());
    }
}
