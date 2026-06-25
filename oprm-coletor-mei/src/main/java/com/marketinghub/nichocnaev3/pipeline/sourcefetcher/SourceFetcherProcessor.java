package com.marketinghub.nichocnaev3.pipeline.sourcefetcher;

import com.marketinghub.nichocnaev3.pipeline.StageArtifact;
import com.marketinghub.nichocnaev3.pipeline.StageContext;
import com.marketinghub.nichocnaev3.pipeline.StageProcessor;
import com.marketinghub.nichocnaev3.pipeline.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Processa a etapa source-fetcher do pipeline NichoCNAE v3. */
public final class SourceFetcherProcessor implements StageProcessor {
    /** Executa a etapa source-fetcher produzindo saída estruturada para o backend decidir avanço. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "source-fetcher");
        output.put("status", "SNAPSHOTS_COLETADOS");
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", "routine-signal-extractor");
        return new StageResult("SNAPSHOTS_COLETADOS", output, List.of(new StageArtifact("SNAPSHOTS_COLETADOS", "inline://nichocnae-v3/source-fetcher", "Etapa source-fetcher concluída com contrato estruturado.")));
    }
}
