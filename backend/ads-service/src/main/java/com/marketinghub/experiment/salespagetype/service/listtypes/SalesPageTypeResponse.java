package com.marketinghub.experiment.salespagetype.service.listtypes;

/** Descreve um tipo de pagina de venda disponivel para uso comercial. */
public record SalesPageTypeResponse(
        String code,
        String name,
        String description,
        String commercialMechanism,
        String leadCaptureStrategy,
        String digitalBaitDelivery,
        boolean defaultForAbTest,
        boolean active) {
}
