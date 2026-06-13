package com.marketinghub.mois.dto;

import java.time.Instant;
import java.util.List;

/** Contratos HTTP para consulta de produtos Hotmart coletados pelo MOIS. */
public final class MoisHotmartProductDtos {

    /** Impede instanciação de classe utilitária de DTOs. */
    private MoisHotmartProductDtos() {}

    /** Representa um produto Hotmart coletado com dados comerciais e data da coleta. */
    public record HotmartCollectedProductResponse(
            String jobId,
            String referenceId,
            String title,
            String productUrl,
            String description,
            String producerName,
            String imageUrl,
            String price,
            String currency,
            String salesPageUrl,
            String pageSalesLink,
            Double temperature,
            Instant collectedAt
    ) {}

    /** Representa a lista paginada simples de produtos Hotmart por workspace. */
    public record HotmartCollectedProductListResponse(
            String workspaceId,
            List<HotmartCollectedProductResponse> items
    ) {}
}
