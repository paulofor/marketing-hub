package com.marketinghub.businessprocess.independent.service.catalog;

/** Contrato de um campo de entrada declarado pelo backend para uma execução independente. */
public record IndependentBusinessProcessInputFieldResponse(
    String key,
    String label,
    String controlType,
    boolean required,
    Integer maxLength,
    String defaultValue,
    String helpText) {}
