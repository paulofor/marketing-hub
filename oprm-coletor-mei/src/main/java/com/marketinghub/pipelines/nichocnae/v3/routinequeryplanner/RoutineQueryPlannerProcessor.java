package com.marketinghub.pipelines.nichocnae.v3.routinequeryplanner;

import com.marketinghub.pipelines.nichocnae.v3.core.StageArtifact;
import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageProcessor;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Processa a etapa routine-query-planner do pipeline NichoCNAE v3. */
public final class RoutineQueryPlannerProcessor implements StageProcessor {
    /** Executa a etapa routine-query-planner produzindo saída estruturada para o backend decidir avanço. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "routine-query-planner");
        output.put("status", "QUERIES_PLANEJADAS");
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", "source-searcher");
        return new StageResult("QUERIES_PLANEJADAS", output, List.of(new StageArtifact("QUERIES_PLANEJADAS", "inline://nichocnae-v3/routine-query-planner", "Etapa routine-query-planner concluída com contrato estruturado.")));
    }
}
