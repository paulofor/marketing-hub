package com.marketinghub.mois.dossie.v1.intake.service;

import com.marketinghub.mois.dossie.v1.intake.service.pending.DossierIntakePendingRequest;
import com.marketinghub.mois.dossie.v1.intake.service.pending.DossierIntakePendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa entrada inicial do pipeline de dossiê MOIS v1. */
@Service
public class DossierIntakeService {

    /** Entrega trabalhos pendentes ao executor sem assumir controle operacional de execução no backend. */
    public DossierIntakePendingResponse pending(DossierIntakePendingRequest request) {
        return new DossierIntakePendingResponse(false, List.of());
    }
}
