package com.marketinghub.pde.harness.v1;

/** Recebe eventos incrementais para heartbeat, auditoria e relatório persistido pelo worker. */
@FunctionalInterface
public interface PdeExecutionObserver {

  /** Processa um evento já correlacionado sem assumir decisão de pipeline. */
  void onEvent(PdeHarnessEvent event);

  /** Retorna um observer neutro para execuções que ainda não enviam telemetria incremental. */
  static PdeExecutionObserver noop() {
    return event -> {};
  }
}
