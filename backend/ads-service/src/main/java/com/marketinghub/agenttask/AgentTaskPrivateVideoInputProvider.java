package com.marketinghub.agenttask;

import java.util.Map;

/**
 * Responsabilidade: entregar entradas privadas de vídeo sem fabricar estratégia ou aceite
 * comercial.
 */
public interface AgentTaskPrivateVideoInputProvider {
  /** Retorna contexto apenas para a tarefa exata vinculada ao pedido autorizado no Estúdio. */
  Map<String, Object> forTask(AgentTask task);
}
