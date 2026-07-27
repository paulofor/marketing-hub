package com.marketinghub.experiment.salespageab.dto;

import java.util.List;

/** Responsabilidade: expor o resultado consolidado de um teste A/B de pagina de venda. */
public record ExperimentSalesPageAbTestResultDto(
    ExperimentSalesPageAbTestDto test,
    List<ExperimentSalesPageAbVariantResultDto> variants,
    String winnerVariantKey,
    String status,
    String recommendation) {}
