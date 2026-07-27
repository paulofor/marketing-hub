package com.marketinghub.experiment.salespagetype.service.listtypes;

import java.math.BigDecimal;

/** Descreve um tipo de pagina de venda selecionado para um experimento. */
public record ExperimentSalesPageTypeSelectionResponse(
    Long id,
    String typeCode,
    String typeName,
    String variantKey,
    BigDecimal trafficWeight,
    boolean active,
    String notes,
    SalesPageTypeResponse type) {}
