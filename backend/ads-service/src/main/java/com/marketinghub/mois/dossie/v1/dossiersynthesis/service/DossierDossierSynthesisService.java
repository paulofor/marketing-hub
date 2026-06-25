package com.marketinghub.mois.dossie.v1.dossiersynthesis.service;

import com.marketinghub.mois.dossie.v1.dossiersynthesis.service.pending.DossierDossierSynthesisPendingRequest;
import com.marketinghub.mois.dossie.v1.dossiersynthesis.service.pending.DossierDossierSynthesisPendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa síntese final do dossiê do pipeline de dossiê MOIS v1. */
@Service
public class DossierDossierSynthesisService {

    /** Entrega trabalhos pendentes ao executor sem assumir controle operacional de execução no backend. */
    public DossierDossierSynthesisPendingResponse pending(DossierDossierSynthesisPendingRequest request) {
        return new DossierDossierSynthesisPendingResponse(false, List.of());
    }
}
