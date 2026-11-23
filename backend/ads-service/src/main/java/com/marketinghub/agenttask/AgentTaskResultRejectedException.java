package com.marketinghub.agenttask;

/** Responsabilidade: distinguir recusa funcional recuperável de falha transitória de transporte. */
public class AgentTaskResultRejectedException extends RuntimeException {
  public static final String CODE = "AGENT_RESULT_CONTRACT_REJECTED";

  /** Preserva a causa técnica sem apresentá-la como mensagem de negócio. */
  public AgentTaskResultRejectedException(Throwable cause) {
    super(CODE + "|" + cause.getMessage(), cause);
  }
}
