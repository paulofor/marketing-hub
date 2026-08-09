package com.marketinghub.creative.dto;

import java.math.BigDecimal;

/** Responsabilidade: receber do AI Worker a nova versão gerada ou a falha da melhoria. */
public record CreativeImprovementResultRequest(
    String imageUrl, BigDecimal costUsd, String requestJson, String responseJson, String error) {}
