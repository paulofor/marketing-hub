package com.marketinghub.mois.dossie.v1.investigationanchorbuilder.service;

import com.marketinghub.mois.dossie.v1.investigationanchorbuilder.service.pending.DossierInvestigationAnchorBuilderPendingRequest;
import com.marketinghub.mois.dossie.v1.investigationanchorbuilder.service.pending.DossierInvestigationAnchorBuilderPendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa geração de âncoras de investigação do pipeline de dossiê MOIS v1. */
@Service
public class DossierInvestigationAnchorBuilderService {

    /** Entrega trabalhos pendentes ao executor sem assumir controle operacional de execução no backend. */
    public DossierInvestigationAnchorBuilderPendingResponse pending(DossierInvestigationAnchorBuilderPendingRequest request) {
        return new DossierInvestigationAnchorBuilderPendingResponse(false, List.of());
    }
}
