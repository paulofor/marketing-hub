package com.marketinghub.agenttask;

/** Responsabilidade: preparar pré-condições auditáveis antes de reservar uma tarefa de agente. */
public interface AgentTaskClaimPreparationHook {

  /** Informa se a preparação especializada governa a tarefa ainda pendente. */
  boolean supports(AgentTask task);

  /** Prepara a fonte de verdade e informa se o backend já pode entregar a tarefa ao executor. */
  Preparation prepare(AgentTask task);

  /**
   * Representa uma liberação ou uma espera persistível sem iniciar modelo nem integração externa.
   */
  record Preparation(boolean ready, String blockerCategory, String reason) {

    /** Libera a reserva quando toda a preparação foi comprovada. */
    public static Preparation ready(String reason) {
      return new Preparation(true, null, reason);
    }

    /** Mantém a tarefa pendente enquanto uma evidência externa obrigatória não existe. */
    public static Preparation waiting(String blockerCategory, String reason) {
      return new Preparation(false, blockerCategory, reason);
    }
  }
}
