package com.marketinghub.agenttask;

import java.util.Map;
import java.util.Optional;

/** Responsabilidade: fornecer o contrato estratégico de mercado aplicável a uma tarefa BPM. */
public interface MarketStrategicContextProvider {
  /** Resolve o contrato estratégico pela referência canônica da entidade do processo. */
  Optional<Map<String, Object>> resolve(String sourceReference);

  /** Oferece a ausência explícita usada por testes que não exercitam contexto estratégico. */
  static MarketStrategicContextProvider empty() {
    return sourceReference -> Optional.empty();
  }
}
