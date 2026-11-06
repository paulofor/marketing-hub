package com.marketinghub.agenttask;

/** Responsabilidade: impedir entrega paga a um transporte incompatível com o teto do contexto. */
public interface AgentTaskSpendingPolicy {
  /** Identifica o recibo protegido sem depender do nome do produto ou da tarefa. */
  boolean protectedExecution(AgentTask task);

  /** Retorna a causa da recusa antes da lease, ou ausência de impedimento financeiro adicional. */
  String claimBlocker(AgentTask task);

  /**
   * Fixa o transporte verificado antes de entregar a lease, sem declarar inferência ou custo zero.
   */
  default void prepareClaim(AgentTask task) {}
}
