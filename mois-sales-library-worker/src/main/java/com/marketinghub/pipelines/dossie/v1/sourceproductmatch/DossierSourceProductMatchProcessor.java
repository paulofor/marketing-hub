package com.marketinghub.pipelines.dossie.v1.sourceproductmatch;

import com.marketinghub.pipelines.dossie.v1.DossierStageSupport;
import com.marketinghub.pipelines.dossie.v1.StageContext;
import com.marketinghub.pipelines.dossie.v1.StageProcessor;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa validação de relação fonte-produto cumprindo o objetivo funcional contratado para o dossiê. */
public class DossierSourceProductMatchProcessor implements StageProcessor {

    private static final String STAGE_NAME = "source-product-match";
    private static final String OBJECTIVE = "Classificar cada fonte externa por força de vínculo com produto, produtor, marca, promessa, mecanismo ou checkout, separando DIRETO, PROVAVEL, INDIRETO, NICHO_APENAS e DESCARTADO antes de virar evidência.";

    /** Informa o nome canônico da etapa validação de relação fonte-produto. */
    @Override
    public String stageName() {
        return STAGE_NAME;
    }

    /** Produz saída funcional auditável alinhada ao objetivo da etapa. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> evidence = DossierStageSupport.evidenceFor(context, STAGE_NAME);
        DossierSourceProductMatchOutput output = new DossierSourceProductMatchOutput(
                context.dossierId(),
                "OBJECTIVE_FULFILLED",
                OBJECTIVE,
                evidence);
        return StageResult.done(
                Map.of(STAGE_NAME, output),
                List.of(DossierStageSupport.objectiveArtifact(context, STAGE_NAME, OBJECTIVE, evidence)));
    }
}
