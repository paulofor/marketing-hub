package com.marketinghub.pde.harness.v1;

/** Normaliza o estado terminal de um turno para o contrato funcional do worker PDE. */
public enum PdeRunStatus {
  COMPLETED,
  INTERRUPTED,
  FAILED,
  BLOCKED
}
