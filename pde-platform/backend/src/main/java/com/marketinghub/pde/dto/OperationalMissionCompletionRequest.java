package com.marketinghub.pde.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

/** Recebe o artefato individual produzido ao concluir um marco operacional. */
public record OperationalMissionCompletionRequest(
        @Size(max = 160) String deliveryTitle,
        @Size(max = 80) String deliveryVersion,
        @Size(max = 20000) String deliveryContent,
        @Size(max = 20) List<DeliverySectionRequest> deliverySections
) {
    /** Mantém compatibilidade com microentregas livres que não possuem contrato de seções. */
    public OperationalMissionCompletionRequest(
            String deliveryTitle,
            String deliveryVersion,
            String deliveryContent) {
        this(deliveryTitle, deliveryVersion, deliveryContent, null);
    }

    /** Recebe uma seção material com todos os itens que serão entregues à cliente. */
    public record DeliverySectionRequest(
            @Size(max = 80) String sectionId,
            @Size(max = 20) List<@Size(max = 1000) String> items
    ) {}
}
