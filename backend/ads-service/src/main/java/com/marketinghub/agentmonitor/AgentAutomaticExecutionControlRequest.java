package com.marketinghub.agentmonitor;

import jakarta.validation.constraints.NotNull;

/** Responsabilidade: receber a decisão administrativa PLAY ou STOP de um agente. */
public record AgentAutomaticExecutionControlRequest(@NotNull Boolean automaticExecutionEnabled) {}
