package com.marketinghub.nichocnaev3.pipeline.cnaeintake;

import com.marketinghub.nichocnaev3.pipeline.StageArtifact;
import com.marketinghub.nichocnaev3.pipeline.StageContext;
import com.marketinghub.nichocnaev3.pipeline.StageProcessor;
import com.marketinghub.nichocnaev3.pipeline.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Processa a etapa cnae-intake do pipeline NichoCNAE v3. */
public final class CnaeIntakeProcessor implements StageProcessor {
    /** Executa a etapa cnae-intake produzindo saída estruturada para o backend decidir avanço. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "cnae-intake");
        output.put("status", "CNAE_RECEBIDO");
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", "persona-candidate-generator");
        return new StageResult("CNAE_RECEBIDO", output, List.of(new StageArtifact("CNAE_RECEBIDO", "inline://nichocnae-v3/cnae-intake", "Etapa cnae-intake concluída com contrato estruturado.")));
    }
}
