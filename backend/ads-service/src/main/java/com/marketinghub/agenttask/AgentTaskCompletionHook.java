package com.marketinghub.agenttask;

/** Responsabilidade: aplicar efeitos internos governados antes de concluir uma tarefa de agente. */
public interface AgentTaskCompletionHook {
  /** Informa se o handler especializado governa a tarefa reservada. */
  boolean supports(AgentTask task);

  /** Aplica o resultado e informa se a conclusão deve aguardar um gate assíncrono. */
  CompletionDisposition apply(AgentTask task, CompleteAgentTaskRequest request);

  /** Define se o backend conclui imediatamente ou mantém a tarefa em processamento. */
  enum CompletionDisposition {
    COMPLETE,
    DEFERRED
  }
}
