package com.marketinghub.nichocnae.evidencelevelgate;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Orquestra no executor externo a leitura, cálculo e escrita do gate comercial E0-E5. */
@Service
public class EvidenceLevelGateService {
    private static final Logger log = LoggerFactory.getLogger(EvidenceLevelGateService.class);
    private final EvidenceLevelGateBackendClient backendClient;
    private final EvidenceLevelGateEngine engine;

    /** Inicializa o serviço com cliente de backend e motor de regra local do executor. */
    public EvidenceLevelGateService(EvidenceLevelGateBackendClient backendClient, EvidenceLevelGateEngine engine) {
        this.backendClient = backendClient;
        this.engine = engine;
    }

    /** Processa pendências da etapa onze e persiste decisões calculadas no backend. */
    public int processPending() {
        List<EvidenceLevelGatePending> pendings = backendClient.listPending();
        int processed = 0;
        for (EvidenceLevelGatePending pending : pendings) {
            try {
                EvidenceLevelGateDecision decision = engine.evaluate(pending);
                backendClient.complete(pending, decision);
                processed++;
            } catch (RuntimeException ex) {
                log.error("Erro ao executar etapa onze E0-E5 OPRM nichocnae no executor (researchCycleId={}, routineCardId={})", pending.researchCycleId(), pending.routineCardId(), ex);
                backendClient.fail(pending, ex);
                throw ex;
            }
        }
        return processed;
    }
}
