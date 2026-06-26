package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.fatosproduto;

import com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.StageContext;
import com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.StageProcessor;
import com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.StageResult;
import java.util.List;
import java.util.Map;

/** Coleta fatos Hotmart e dados mínimos do produto. */
public class FatosProdutoProcessor implements StageProcessor {

    /** Informa o nome canônico da etapa fatosproduto. */
    @Override
    public String stageName() {
        return "fatosproduto";
    }

    /** Executa a etapa fatosproduto a partir do contexto recebido do backend. */
    @Override
    public StageResult process(StageContext context) {
        FatosProdutoOutput output = new FatosProdutoOutput(
                context.salesPageId(),
                "READY_FOR_IMPLEMENTATION",
                "Coleta fatos Hotmart e dados mínimos do produto."
        );
        return StageResult.done(Map.of("fatosproduto", output), List.of());
    }
}
