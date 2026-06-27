package com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.imagem.service.consultaSituacao;

import java.util.List;

/** Requisição com os status usados para filtrar auditorias da etapa Imagem. */
public record GeraAnuncioImagemSituacaoRequest(List<String> status) {}
