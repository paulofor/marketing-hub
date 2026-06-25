package com.marketinghub.nichocnaev3.pipeline.personacandidategenerator;

import com.marketinghub.nichocnaev3.pipeline.StageArtifact;
import com.marketinghub.nichocnaev3.pipeline.StageContext;
import com.marketinghub.nichocnaev3.pipeline.StageProcessor;
import com.marketinghub.nichocnaev3.pipeline.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Processa a etapa persona-candidate-generator do pipeline NichoCNAE v3. */
public final class PersonaCandidateGeneratorProcessor implements StageProcessor {
    /** Executa a etapa persona-candidate-generator produzindo saída estruturada para o backend decidir avanço. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "persona-candidate-generator");
        output.put("status", "PERSONAS_CANDIDATAS");
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", "persona-tournament");
        return new StageResult("PERSONAS_CANDIDATAS", output, List.of(new StageArtifact("PERSONAS_CANDIDATAS", "inline://nichocnae-v3/persona-candidate-generator", "Etapa persona-candidate-generator concluída com contrato estruturado.")));
    }
}
