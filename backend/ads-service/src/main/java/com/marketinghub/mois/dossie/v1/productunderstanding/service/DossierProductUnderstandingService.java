package com.marketinghub.mois.dossie.v1.productunderstanding.service;

import com.marketinghub.mois.dossie.v1.productunderstanding.service.pending.DossierProductUnderstandingPendingRequest;
import com.marketinghub.mois.dossie.v1.productunderstanding.service.pending.DossierProductUnderstandingPendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa entendimento do produto do pipeline de dossiê MOIS v1. */
@Service
public class DossierProductUnderstandingService {

    /** Entrega trabalhos pendentes ao executor sem assumir controle operacional de execução no backend. */
    public DossierProductUnderstandingPendingResponse pending(DossierProductUnderstandingPendingRequest request) {
        return new DossierProductUnderstandingPendingResponse(false, List.of());
    }
}
