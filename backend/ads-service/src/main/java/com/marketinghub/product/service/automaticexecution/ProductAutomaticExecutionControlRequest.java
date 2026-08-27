package com.marketinghub.product.service.automaticexecution;

import jakarta.validation.constraints.NotNull;

/** Responsabilidade: receber a decisão administrativa PLAY ou STOP de um produto. */
public record ProductAutomaticExecutionControlRequest(@NotNull Boolean automaticExecutionEnabled) {}
