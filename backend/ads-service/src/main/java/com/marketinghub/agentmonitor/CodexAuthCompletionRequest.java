package com.marketinghub.agentmonitor;

/** Responsabilidade: receber apenas o resultado validado da autenticação no executor. */
public record CodexAuthCompletionRequest(boolean authenticated, String detail) {}
