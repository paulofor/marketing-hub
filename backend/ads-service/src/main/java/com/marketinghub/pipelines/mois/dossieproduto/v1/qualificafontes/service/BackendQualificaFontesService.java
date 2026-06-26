package com.marketinghub.pipelines.mois.dossieproduto.v1.qualificafontes.service;

import com.marketinghub.pipelines.mois.dossieproduto.v1.qualificafontes.service.pending.QualificaFontesPendingRequest;
import com.marketinghub.pipelines.mois.dossieproduto.v1.qualificafontes.service.pending.QualificaFontesPendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Mantém leitura, escrita e publicação de pendências da etapa qualificação de fontes do dossiê do produto MOIS v1. */
@Service
public class BackendQualificaFontesService {

    /** Retorna a fila pendente da etapa sem assumir execução operacional do worker. */
    public QualificaFontesPendingResponse pending(QualificaFontesPendingRequest request) {
        return new QualificaFontesPendingResponse(false, List.of());
    }
}
