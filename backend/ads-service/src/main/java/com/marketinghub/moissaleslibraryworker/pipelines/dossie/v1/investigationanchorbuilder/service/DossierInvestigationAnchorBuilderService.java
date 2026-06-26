package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.investigationanchorbuilder.service;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.investigationanchorbuilder.service.pending.DossierInvestigationAnchorBuilderPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.investigationanchorbuilder.service.pending.DossierInvestigationAnchorBuilderPendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa geração de âncoras de investigação do pipeline de dossiê MOIS v1. */
@Service
public class DossierInvestigationAnchorBuilderService {
    private static final String STAGE_CODE = "investigation-anchor-builder";
    private static final String NEXT_STAGE = "warmup-resource-discovery";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING = "AGUARDANDO_RETORNO_MODULO";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";

    /** Registra a intenção de iniciar a etapa para o produto informado sem executar a rotina no backend. */
    public void start(String productKey) {
        // Método reservado para criação futura da pendência canônica da etapa para a chave do produto.
    }

    /** Entrega trabalhos pendentes ao executor sem assumir controle operacional de execução no backend. */
    public DossierInvestigationAnchorBuilderPendingResponse pending(DossierInvestigationAnchorBuilderPendingRequest request) {
        return new DossierInvestigationAnchorBuilderPendingResponse(false, List.of());
    }
}
