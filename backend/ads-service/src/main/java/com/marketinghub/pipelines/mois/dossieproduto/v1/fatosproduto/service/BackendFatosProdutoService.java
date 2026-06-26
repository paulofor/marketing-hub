package com.marketinghub.pipelines.mois.dossieproduto.v1.fatosproduto.service;

import com.marketinghub.pipelines.mois.dossieproduto.v1.fatosproduto.service.pending.FatosProdutoPendingRequest;
import com.marketinghub.pipelines.mois.dossieproduto.v1.fatosproduto.service.pending.FatosProdutoPendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Mantém leitura, escrita e publicação de pendências da etapa fatos do produto do dossiê do produto MOIS v1. */
@Service
public class BackendFatosProdutoService {

    /** Retorna a fila pendente da etapa sem assumir execução operacional do worker. */
    public FatosProdutoPendingResponse pending(FatosProdutoPendingRequest request) {
        return new FatosProdutoPendingResponse(false, List.of());
    }
}
