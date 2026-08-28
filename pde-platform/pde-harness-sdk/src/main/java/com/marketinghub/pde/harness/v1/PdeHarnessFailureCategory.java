package com.marketinghub.pde.harness.v1;

/** Classifica falhas do harness para que o worker reporte uma causa operacional estável. */
public enum PdeHarnessFailureCategory {
  CONFIGURATION,
  PROTOCOL_INCOMPATIBLE,
  APP_SERVER_UNAVAILABLE,
  AUTHENTICATION_REQUIRED,
  TIMEOUT,
  EXECUTION_FAILED
}
