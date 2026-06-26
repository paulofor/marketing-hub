package com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.service.recebeRequest;

/** Contrato que recebe o request operacional montado pelo AI Worker para a etapa Texto. */
public record GeraAnuncioTextoRecebeRequestRequest(Object request, String plataforma, String prompt, String schema) {}
