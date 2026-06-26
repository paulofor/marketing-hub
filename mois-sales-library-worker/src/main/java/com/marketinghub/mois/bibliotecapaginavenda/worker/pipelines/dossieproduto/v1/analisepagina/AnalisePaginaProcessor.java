package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.analisepagina;

import com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.StageContext;
import com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.StageProcessor;
import com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.StageResult;
import java.util.List;
import java.util.Map;

/** Transforma página capturada em leitura comercial. */
public class AnalisePaginaProcessor implements StageProcessor {

    /** Informa o nome canônico da etapa analisepagina. */
    @Override
    public String stageName() {
        return "analisepagina";
    }

    /** Executa a etapa analisepagina a partir do contexto recebido do backend. */
    @Override
    public StageResult process(StageContext context) {
        AnalisePaginaOutput output = new AnalisePaginaOutput(
                context.salesPageId(),
                "READY_FOR_IMPLEMENTATION",
                "Transforma página capturada em leitura comercial."
        );
        return StageResult.done(Map.of("analisepagina", output), List.of());
    }
}
