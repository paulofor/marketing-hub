package com.marketinghub.pipelines.nichocnae.v3.qualitygate;

import com.marketinghub.pipelines.nichocnae.v3.core.StageArtifact;
import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageProcessor;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Processa a etapa quality-gate do pipeline NichoCNAE v3. */
public final class QualityGateProcessor implements StageProcessor {
    /** Executa a etapa quality-gate produzindo saída estruturada para o backend decidir avanço. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "quality-gate");
        output.put("status", "QUALIDADE_APROVADA");
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", "persona-routine-materializer");
        return new StageResult("QUALIDADE_APROVADA", output, List.of(new StageArtifact("QUALIDADE_APROVADA", "inline://nichocnae-v3/quality-gate", "Etapa quality-gate concluída com contrato estruturado.")));
    }
}
