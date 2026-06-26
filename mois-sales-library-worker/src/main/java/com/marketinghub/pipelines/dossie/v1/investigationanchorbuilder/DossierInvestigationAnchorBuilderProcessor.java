package com.marketinghub.pipelines.dossie.v1.investigationanchorbuilder;

import com.marketinghub.pipelines.dossie.v1.StageContext;
import com.marketinghub.pipelines.dossie.v1.StageProcessor;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa geração de âncoras de investigação sem acoplar o núcleo do pipeline às demais etapas do dossiê. */
public class DossierInvestigationAnchorBuilderProcessor implements StageProcessor {

    /** Informa o nome canônico da etapa geração de âncoras de investigação. */
    @Override
    public String stageName() {
        return "investigation-anchor-builder";
    }

    /** Produz saída estruturada mínima para auditoria e evolução incremental da etapa. */
    @Override
    public StageResult process(StageContext context) {
        DossierInvestigationAnchorBuilderOutput output = new DossierInvestigationAnchorBuilderOutput(
                context.dossierId(),
                "READY_FOR_IMPLEMENTATION",
                "Gerar âncoras confiáveis como produto, produtor, domínio, marca, promessa e termos proprietários."
        );
        return StageResult.done(Map.of("investigation-anchor-builder", output), List.of());
    }
}
