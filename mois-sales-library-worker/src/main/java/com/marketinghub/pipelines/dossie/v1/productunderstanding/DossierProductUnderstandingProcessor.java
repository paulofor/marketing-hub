package com.marketinghub.pipelines.dossie.v1.productunderstanding;

import com.marketinghub.pipelines.dossie.v1.DossierStageSupport;
import com.marketinghub.pipelines.dossie.v1.StageContext;
import com.marketinghub.pipelines.dossie.v1.StageProcessor;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa entendimento do produto cumprindo o objetivo funcional contratado para o dossiê. */
public class DossierProductUnderstandingProcessor implements StageProcessor {

    private static final String STAGE_NAME = "product-understanding";
    private static final String OBJECTIVE = "Estruturar produto, público, dor, promessa, mecanismo, formato, oferta e prova antes da pesquisa externa.";

    /** Informa o nome canônico da etapa entendimento do produto. */
    @Override
    public String stageName() {
        return STAGE_NAME;
    }

    /** Produz saída funcional auditável alinhada ao objetivo da etapa. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> evidence = DossierStageSupport.evidenceFor(context, STAGE_NAME);
        DossierProductUnderstandingOutput output = new DossierProductUnderstandingOutput(
                context.dossierId(),
                "OBJECTIVE_FULFILLED",
                OBJECTIVE,
                evidence);
        return StageResult.done(
                Map.of(STAGE_NAME, output),
                List.of(DossierStageSupport.objectiveArtifact(context, STAGE_NAME, OBJECTIVE, evidence)));
    }
}
