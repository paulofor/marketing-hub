package com.marketinghub.gerasalespage.v1.service;

import java.time.Instant;
import java.util.List;

/** Resposta publica com a auditoria historica de uma pagina de venda gerada pelo pipeline. */
public record GeraSalesPagePublicationResponse(
        Long id,
        Long experimentId,
        String publicationJobId,
        Instant publishedAt,
        String salesPageUrl,
        String checkoutUrl,
        String html,
        String publicationPackageJson,
        List<GeraSalesPagePublicationStageResponse> stages) {}
