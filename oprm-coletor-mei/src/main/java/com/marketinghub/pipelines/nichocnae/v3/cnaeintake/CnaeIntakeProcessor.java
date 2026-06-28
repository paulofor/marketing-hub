package com.marketinghub.pipelines.nichocnae.v3.cnaeintake;

import com.marketinghub.pipelines.nichocnae.v3.core.StageArtifact;
import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageProcessor;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Processa a etapa cnae-intake do pipeline NichoCNAE v3. */
public final class CnaeIntakeProcessor implements StageProcessor {
    /** Executa a etapa cnae-intake qualificando o CNAE e o público MEI/autônomo antes de gerar personas. */
    @Override
    public StageResult process(StageContext context) {
        String cnaeCode = requiredText(context.input().get("cnaeCode"), "cnaeCode");
        String cnaeDescription = requiredText(context.input().get("cnaeDescription"), "cnaeDescription");
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "cnae-intake");
        output.put("status", "CNAE_RECEBIDO");
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("cnaeCode", cnaeCode);
        output.put("cnaeDescription", cnaeDescription);
        output.put("targetAudienceType", "MEI_PROFISSIONAIS_AUTONOMOS_NAO_CLT");
        output.put("targetAudienceDefinition", "Estamos falando de MEI, donos-operadores e profissionais autônomos que atuam por conta própria, sem contratação direta como CLT.");
        output.put("cnaeRole", "CNAE_E_PONTO_DE_PARTIDA_ESTATISTICO_NAO_PUBLICO_FINAL");
        output.put("discoveryFrame", "Este CNAE possui volume de MEIs, mas ainda é necessário descobrir qual pessoa real trabalha nele e em qual situação operacional cotidiana.");
        output.put("researchMode", "REALIDADE_OPERACIONAL_DE_ROTINA_COM_CANAIS_DO_MEI");
        output.put("employmentBoundary", "NAO_ANALISAR_FUNCIONARIOS_CLT_CONTRATADOS_DIRETAMENTE");
        output.put("inputKeys", context.input().keySet());
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", "persona-candidate-generator");
        return new StageResult("CNAE_RECEBIDO", output, List.of(new StageArtifact("CNAE_RECEBIDO", "inline://nichocnae-v3/cnae-intake", "Etapa cnae-intake concluída com CNAE qualificado para geração de personas.")));
    }

    /** Extrai texto obrigatório do payload de entrada para impedir avanço sem contexto mínimo. */
    private String requiredText(Object value, String fieldName) {
        if (value == null || value.toString().isBlank()) {
            throw new IllegalStateException("Etapa cnae-intake exige " + fieldName + " para qualificar a entrada do pipeline.");
        }
        return value.toString();
    }
}
