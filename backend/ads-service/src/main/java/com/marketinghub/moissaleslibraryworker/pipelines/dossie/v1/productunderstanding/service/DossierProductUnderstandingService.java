package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.productunderstanding.service;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.productunderstanding.service.pending.DossierProductUnderstandingPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.productunderstanding.service.pending.DossierProductUnderstandingPendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa entendimento do produto do pipeline de dossiê MOIS v1. */
@Service
public class DossierProductUnderstandingService {
    private static final String STAGE_CODE = "product-understanding";
    private static final String NEXT_STAGE = "investigation-anchor-builder";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING = "AGUARDANDO_RETORNO_MODULO";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";

    /** Registra a intenção de iniciar a etapa para o produto informado sem executar a rotina no backend. */
    public void start(String productKey) {
        // Método reservado para criação futura da pendência canônica da etapa para a chave do produto.
    }

    /** Entrega trabalhos pendentes ao executor sem assumir controle operacional de execução no backend. */
    public DossierProductUnderstandingPendingResponse pending(DossierProductUnderstandingPendingRequest request) {
        return new DossierProductUnderstandingPendingResponse(false, List.of());
    }
}
