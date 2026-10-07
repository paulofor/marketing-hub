package com.marketinghub.agenttask;

import java.util.Map;
import java.util.Optional;

/**
 * Responsabilidade: fornecer a Íris o produto, a economia e as provas congeladas da comunicação.
 */
public interface CommunicationMaterializationContextProvider {
  /** Resolve o contrato de entrada pela referência canônica da tarefa. */
  Optional<Map<String, Object>> resolve(String sourceReference);

  /** Informa mudança funcional de entrada sem invalidar a mensagem ou o contrato legado. */
  default Optional<String> executionInputVersion(String processCode, String sourceReference) {
    return Optional.empty();
  }

  /** Oferece ausência explícita para testes que não exercitam o domínio de comunicação. */
  static CommunicationMaterializationContextProvider empty() {
    return sourceReference -> Optional.empty();
  }
}
