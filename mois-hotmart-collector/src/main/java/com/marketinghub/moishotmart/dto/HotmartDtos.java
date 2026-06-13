package com.marketinghub.moishotmart.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

/** Contratos de entrada e saída usados pelo coletor Hotmart do MOIS. */
public final class HotmartDtos {

    /** Impede instanciação de classe utilitária de DTOs. */
    private HotmartDtos() {
    }

    /** Representa o pedido operacional para executar um ciclo de coleta Hotmart. */
    public record HotmartCollectionRequest(
            @NotBlank String source,
            int maxProducts
    ) {
    }

    /** Representa um snapshot bruto de produto obtido nos ciclos Hotmart. */
    public record HotmartProductSnapshot(
            String ucode,
            String title,
            String image,
            String rating,
            Integer totalAnswers,
            Double blueprint,
            String commission,
            Double priceValue,
            String category,
            String format,
            String description,
            String producerName,
            String detailsUrl,
            Double temperature,
            String salesPageUrl,
            Instant collectedAt
    ) {
    }

    /** Representa o resultado consolidado de um ciclo de coleta Hotmart. */
    public record HotmartCollectionResponse(
            String status,
            String message,
            List<HotmartProductSnapshot> products
    ) {
    }
}
