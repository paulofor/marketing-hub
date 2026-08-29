package com.marketinghub.agenttask;

import java.util.Optional;

/** Responsabilidade: resolver a identidade comercial exata da entidade vinculada à tarefa. */
public interface AgentTaskTargetContextProvider {
  /** Localiza produto, experimento e versão sem inferir identidade pelo título da tarefa. */
  Optional<AgentTaskTargetResponse> resolve(String sourceReference);

  /** Resolve o alvo considerando o processo para não misturar landing e versão produtiva do PDE. */
  default Optional<AgentTaskTargetResponse> resolve(String sourceReference, String processCode) {
    return resolve(sourceReference);
  }

  /** Oferece ausência explícita para testes sem entidade comercial vinculada. */
  static AgentTaskTargetContextProvider empty() {
    return sourceReference -> Optional.empty();
  }
}
