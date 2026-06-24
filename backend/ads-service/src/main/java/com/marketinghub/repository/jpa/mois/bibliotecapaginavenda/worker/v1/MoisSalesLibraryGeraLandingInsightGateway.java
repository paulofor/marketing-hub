package com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1;

import java.math.BigDecimal;
import java.util.List;

/** Responsabilidade: expor insumos comerciais da biblioteca MOIS para enriquecer prompts do GeraLanding sem acesso direto ao banco fora de repositories. */
public interface MoisSalesLibraryGeraLandingInsightGateway {

    /** Busca as melhores referências analisadas para servir de memória comercial ao GeraLanding. */
    List<GeraLandingReferenceInsight> findTopReferences(int limit);

    /** Representa uma referência de página vencedora com os insumos separados por etapa do GeraLanding. */
    record GeraLandingReferenceInsight(
            Long pageId,
            String urlCanonical,
            String title,
            BigDecimal scoreTotal,
            Object wireframeInsight,
            Object copyInsight,
            Object imagePromptInsight,
            Object designPresetInsight
    ) {
    }
}
