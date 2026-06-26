package com.marketinghub.pipelines.mois.dossieproduto.v1.analisepagina.service;

import com.marketinghub.pipelines.mois.dossieproduto.v1.analisepagina.service.pending.AnalisePaginaPendingRequest;
import com.marketinghub.pipelines.mois.dossieproduto.v1.analisepagina.service.pending.AnalisePaginaPendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Mantém leitura, escrita e publicação de pendências da etapa análise da página do dossiê do produto MOIS v1. */
@Service
public class BackendAnalisePaginaService {

    /** Retorna a fila pendente da etapa sem assumir execução operacional do worker. */
    public AnalisePaginaPendingResponse pending(AnalisePaginaPendingRequest request) {
        return new AnalisePaginaPendingResponse(false, List.of());
    }
}
