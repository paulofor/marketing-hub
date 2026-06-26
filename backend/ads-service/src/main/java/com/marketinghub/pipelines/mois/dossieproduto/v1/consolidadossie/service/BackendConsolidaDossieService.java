package com.marketinghub.pipelines.mois.dossieproduto.v1.consolidadossie.service;

import com.marketinghub.pipelines.mois.dossieproduto.v1.consolidadossie.service.pending.ConsolidaDossiePendingRequest;
import com.marketinghub.pipelines.mois.dossieproduto.v1.consolidadossie.service.pending.ConsolidaDossiePendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Mantém leitura, escrita e publicação de pendências da etapa consolidação do dossiê do dossiê do produto MOIS v1. */
@Service
public class BackendConsolidaDossieService {

    /** Retorna a fila pendente da etapa sem assumir execução operacional do worker. */
    public ConsolidaDossiePendingResponse pending(ConsolidaDossiePendingRequest request) {
        return new ConsolidaDossiePendingResponse(false, List.of());
    }
}
