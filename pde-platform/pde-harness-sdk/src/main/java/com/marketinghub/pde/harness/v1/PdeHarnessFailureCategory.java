package com.marketinghub.pde.harness.v1;

/** Classifica falhas do harness para que o worker reporte uma causa operacional estável. */
public enum PdeHarnessFailureCategory {
  CONFIGURATION,
  PROTOCOL_INCOMPATIBLE,
  APP_SERVER_UNAVAILABLE,
  AUTHENTICATION_REQUIRED,
  ISOLATION_VIOLATION,
  MEMORY_CONFLICT,
  CONVERSATION_BUSY,
  TIMEOUT,
  EXECUTION_FAILED
}
