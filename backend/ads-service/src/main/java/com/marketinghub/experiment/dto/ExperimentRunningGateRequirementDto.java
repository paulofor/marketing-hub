package com.marketinghub.experiment.dto;

/** Representa um requisito auditável para a transição segura do experimento para RUNNING. */
public record ExperimentRunningGateRequirementDto(
    String code, String title, boolean ready, String detail, String recommendation) {}
