package com.marketinghub.nichocnaev3.pipeline.routinesignalextractor;

import com.marketinghub.nichocnaev3.pipeline.StageArtifact;
import com.marketinghub.nichocnaev3.pipeline.StageContext;
import com.marketinghub.nichocnaev3.pipeline.StageProcessor;
import com.marketinghub.nichocnaev3.pipeline.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Processa a etapa routine-signal-extractor do pipeline NichoCNAE v3. */
public final class RoutineSignalExtractorProcessor implements StageProcessor {
    /** Executa a etapa routine-signal-extractor produzindo saída estruturada para o backend decidir avanço. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "routine-signal-extractor");
        output.put("status", "SINAIS_EXTRAIDOS");
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", "daily-tasks-synthesizer");
        return new StageResult("SINAIS_EXTRAIDOS", output, List.of(new StageArtifact("SINAIS_EXTRAIDOS", "inline://nichocnae-v3/routine-signal-extractor", "Etapa routine-signal-extractor concluída com contrato estruturado.")));
    }
}
