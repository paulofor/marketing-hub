package com.marketinghub.pipelines.dossie.v1.dossiersynthesis;

import com.marketinghub.pipelines.dossie.v1.StageContext;
import com.marketinghub.pipelines.dossie.v1.StageProcessor;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa síntese final do dossiê sem acoplar o núcleo do pipeline às demais etapas do dossiê. */
public class DossierDossierSynthesisProcessor implements StageProcessor {

    /** Informa o nome canônico da etapa síntese final do dossiê. */
    @Override
    public String stageName() {
        return "dossier-synthesis";
    }

    /** Produz saída estruturada mínima para auditoria e evolução incremental da etapa. */
    @Override
    public StageResult process(StageContext context) {
        DossierDossierSynthesisOutput output = new DossierDossierSynthesisOutput(
                context.dossierId(),
                "READY_FOR_IMPLEMENTATION",
                "Gerar conclusão de negócio, evidências, estrutura reutilizável e próximos passos comerciais."
        );
        return StageResult.done(Map.of("dossier-synthesis", output), List.of());
    }
}
