package com.marketinghub.pipelines.mois.dossieproduto.v1.planejabuscas.service;

import com.marketinghub.pipelines.mois.dossieproduto.v1.planejabuscas.service.pending.PlanejaBuscasPendingRequest;
import com.marketinghub.pipelines.mois.dossieproduto.v1.planejabuscas.service.pending.PlanejaBuscasPendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Mantém leitura, escrita e publicação de pendências da etapa planejamento de buscas do dossiê do produto MOIS v1. */
@Service
public class BackendPlanejaBuscasService {

    /** Retorna a fila pendente da etapa sem assumir execução operacional do worker. */
    public PlanejaBuscasPendingResponse pending(PlanejaBuscasPendingRequest request) {
        return new PlanejaBuscasPendingResponse(false, List.of());
    }
}
