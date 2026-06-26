package com.marketinghub.pipelines.mois.dossieproduto.v1.qualificafontes.web;

import com.marketinghub.pipelines.mois.dossieproduto.v1.qualificafontes.service.BackendQualificaFontesService;
import com.marketinghub.pipelines.mois.dossieproduto.v1.qualificafontes.service.pending.QualificaFontesPendingRequest;
import com.marketinghub.pipelines.mois.dossieproduto.v1.qualificafontes.service.pending.QualificaFontesPendingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe os contratos internos da etapa qualificação de fontes do pipeline dossiê do produto MOIS v1. */
@RestController
@RequestMapping("/api/internal/mois/dossieproduto/v1/qualifica-fontes/stage-executions")
@RequiredArgsConstructor
public class BackendQualificaFontesController {

    private final BackendQualificaFontesService service;

    /** Entrega pendências da etapa qualificação de fontes para o módulo executor oficial. */
    @PostMapping("/pending")
    public QualificaFontesPendingResponse pending(@Valid @RequestBody QualificaFontesPendingRequest request) {
        return service.pending(request);
    }
}
