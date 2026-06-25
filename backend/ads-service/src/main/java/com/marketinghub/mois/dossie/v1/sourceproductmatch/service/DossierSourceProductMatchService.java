package com.marketinghub.mois.dossie.v1.sourceproductmatch.service;

import com.marketinghub.mois.dossie.v1.sourceproductmatch.service.pending.DossierSourceProductMatchPendingRequest;
import com.marketinghub.mois.dossie.v1.sourceproductmatch.service.pending.DossierSourceProductMatchPendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa validação de relação fonte-produto do pipeline de dossiê MOIS v1. */
@Service
public class DossierSourceProductMatchService {

    /** Entrega trabalhos pendentes ao executor sem assumir controle operacional de execução no backend. */
    public DossierSourceProductMatchPendingResponse pending(DossierSourceProductMatchPendingRequest request) {
        return new DossierSourceProductMatchPendingResponse(false, List.of());
    }
}
