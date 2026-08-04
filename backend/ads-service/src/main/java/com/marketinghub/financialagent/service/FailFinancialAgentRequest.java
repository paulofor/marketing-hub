package com.marketinghub.financialagent.service;

/** Responsabilidade: receber uma falha tecnica do worker financeiro. */
public record FailFinancialAgentRequest(String errorMessage) {}
