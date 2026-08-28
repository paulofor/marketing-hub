package com.marketinghub.pde.harness.v1.internal;

import com.marketinghub.pde.harness.v1.PdeAgentRunRequest;
import com.marketinghub.pde.harness.v1.PdeHarnessException;
import com.marketinghub.pde.harness.v1.PdeHarnessFailureCategory;
import com.marketinghub.pde.harness.v1.PdeThreadBinding;

/** Valida memória e thread antes que qualquer histórico seja carregado pelo App Server. */
public final class PdeMemoryPolicy {

  /** Impede instanciação de uma política composta apenas por validações determinísticas. */
  private PdeMemoryPolicy() {}

  /** Bloqueia memória cruzada, thread fora de escopo e regressão da revisão já utilizada. */
  public static void validate(PdeAgentRunRequest request) {
    if (!request.context().customerScope().equals(request.memory().scope())) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.ISOLATION_VIOLATION,
          "Snapshot de memória não pertence ao cliente e produto da execução");
    }
    PdeThreadBinding binding = request.existingThreadBinding();
    if (binding == null) {
      return;
    }
    if (!binding.belongsTo(request.context().conversationScope())) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.ISOLATION_VIOLATION,
          "Thread não pertence ao tenant, produto, versão, cliente e conversa da execução");
    }
    if (binding.ephemeral()) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.ISOLATION_VIOLATION,
          "Thread efêmera não pode ser retomada após o descarte");
    }
    if (request.memory().revision() < binding.memoryRevision()) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.MEMORY_CONFLICT,
          "Snapshot de memória é anterior à revisão já utilizada pela thread");
    }
  }
}
