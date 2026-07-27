package com.marketinghub.experiment.salespagetype.service.updateselection;

import java.math.BigDecimal;

/** Representa um tipo escolhido para compor o teste de pagina de venda do experimento. */
public record UpdateExperimentSalesPageTypeSelectionItem(
    String typeCode, String variantKey, BigDecimal trafficWeight, Boolean active, String notes) {}
