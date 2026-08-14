package com.marketinghub.agentlearning.v1;

import jakarta.validation.constraints.NotBlank;

/** Contrato de resultado real observado após a promoção de uma skill. */
public record SkillMonitoringRequest(
    boolean approved,
    boolean safetyIncident,
    boolean costLimitRespected,
    @NotBlank String evidence) {}
