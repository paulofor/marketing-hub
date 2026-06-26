package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.sourceproductmatch.service;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.sourceproductmatch.service.pending.DossierSourceProductMatchPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.sourceproductmatch.service.pending.DossierSourceProductMatchPendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa validação de relação fonte-produto do pipeline de dossiê MOIS v1. */
@Service
public class DossierSourceProductMatchService {
    private static final String STAGE_CODE = "source-product-match";
    private static final String NEXT_STAGE = "warmup-signal-extraction";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING = "AGUARDANDO_RETORNO_MODULO";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";

    /** Registra a intenção de iniciar a etapa para o produto informado sem executar a rotina no backend. */
    public void start(String productKey) {
        // Método reservado para criação futura da pendência canônica da etapa para a chave do produto.
    }

    /** Entrega trabalhos pendentes ao executor sem assumir controle operacional de execução no backend. */
    public DossierSourceProductMatchPendingResponse pending(DossierSourceProductMatchPendingRequest request) {
        return new DossierSourceProductMatchPendingResponse(false, List.of());
    }
}
