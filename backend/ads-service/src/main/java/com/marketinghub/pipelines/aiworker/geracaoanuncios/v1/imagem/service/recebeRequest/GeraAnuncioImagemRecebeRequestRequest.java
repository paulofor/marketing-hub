package com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.recebeRequest;

/** Contrato que recebe o request operacional montado pelo AI Worker para a etapa Imagem. */
public record GeraAnuncioImagemRecebeRequestRequest(Object request, String plataforma, String prompt, String schema) {}
