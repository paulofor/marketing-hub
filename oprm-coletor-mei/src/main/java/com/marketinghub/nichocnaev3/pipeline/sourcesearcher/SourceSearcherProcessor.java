package com.marketinghub.nichocnaev3.pipeline.sourcesearcher;

import com.marketinghub.nichocnaev3.pipeline.StageArtifact;
import com.marketinghub.nichocnaev3.pipeline.StageContext;
import com.marketinghub.nichocnaev3.pipeline.StageProcessor;
import com.marketinghub.nichocnaev3.pipeline.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Processa a etapa source-searcher do pipeline NichoCNAE v3. */
public final class SourceSearcherProcessor implements StageProcessor {
    /** Executa a etapa source-searcher produzindo saída estruturada para o backend decidir avanço. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "source-searcher");
        output.put("status", "FONTES_ENCONTRADAS");
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", "source-fetcher");
        return new StageResult("FONTES_ENCONTRADAS", output, List.of(new StageArtifact("FONTES_ENCONTRADAS", "inline://nichocnae-v3/source-searcher", "Etapa source-searcher concluída com contrato estruturado.")));
    }
}
