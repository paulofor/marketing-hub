package com.marketinghub.nichocnaev3.pipeline.personatournament;

import com.marketinghub.nichocnaev3.pipeline.StageArtifact;
import com.marketinghub.nichocnaev3.pipeline.StageContext;
import com.marketinghub.nichocnaev3.pipeline.StageProcessor;
import com.marketinghub.nichocnaev3.pipeline.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Processa a etapa persona-tournament do pipeline NichoCNAE v3. */
public final class PersonaTournamentProcessor implements StageProcessor {
    /** Executa a etapa persona-tournament produzindo saída estruturada para o backend decidir avanço. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "persona-tournament");
        output.put("status", "PERSONA_PRIORIZADA");
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", "routine-query-planner");
        return new StageResult("PERSONA_PRIORIZADA", output, List.of(new StageArtifact("PERSONA_PRIORIZADA", "inline://nichocnae-v3/persona-tournament", "Etapa persona-tournament concluída com contrato estruturado.")));
    }
}
