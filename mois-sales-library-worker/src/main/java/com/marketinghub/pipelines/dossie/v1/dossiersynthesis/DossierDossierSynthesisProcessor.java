package com.marketinghub.pipelines.dossie.v1.dossiersynthesis;

import com.marketinghub.pipelines.dossie.v1.DossierStageSupport;
import com.marketinghub.pipelines.dossie.v1.StageContext;
import com.marketinghub.pipelines.dossie.v1.StageProcessor;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa síntese final do dossiê cumprindo o objetivo funcional contratado para o dossiê. */
public class DossierDossierSynthesisProcessor implements StageProcessor {

    private static final String STAGE_NAME = "dossier-synthesis";
    private static final String OBJECTIVE = "Consolidar conclusão de negócio, evidências, recursos de aquecimento, recomendação final e próximos passos comerciais para exibição na tela.";

    /** Informa o nome canônico da etapa síntese final do dossiê. */
    @Override
    public String stageName() {
        return STAGE_NAME;
    }

    /** Produz saída funcional auditável alinhada ao objetivo da etapa. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> evidence = DossierStageSupport.evidenceFor(context, STAGE_NAME);
        DossierDossierSynthesisOutput output = new DossierDossierSynthesisOutput(
                context.dossierId(),
                "OBJECTIVE_FULFILLED",
                OBJECTIVE,
                evidence);
        return StageResult.done(
                Map.of(STAGE_NAME, output),
                List.of(DossierStageSupport.objectiveArtifact(context, STAGE_NAME, OBJECTIVE, evidence)));
    }
}
