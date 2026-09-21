package com.marketinghub.gerasalespage.v1.service.retry;

import jakarta.validation.constraints.NotBlank;

/** Responsabilidade: vincular a retomada à tentativa exata conferida pelo usuário. */
public record StageRetryRequest(@NotBlank String failedJobId) {}
