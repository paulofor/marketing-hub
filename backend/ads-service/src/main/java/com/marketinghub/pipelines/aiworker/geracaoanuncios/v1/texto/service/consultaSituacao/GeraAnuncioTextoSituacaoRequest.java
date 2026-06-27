package com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.service.consultaSituacao;

import java.util.List;

/** Requisição com os status usados para filtrar auditorias da etapa Texto. */
public record GeraAnuncioTextoSituacaoRequest(List<String> status) {}
