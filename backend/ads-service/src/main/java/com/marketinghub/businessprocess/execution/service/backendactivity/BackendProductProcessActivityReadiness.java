package com.marketinghub.businessprocess.execution.service.backendactivity;

import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityRequirementResponse;
import java.util.List;

/** Responsabilidade: explicar se uma atividade determinística do backend pode ser executada. */
public record BackendProductProcessActivityReadiness(
    boolean ready,
    String reason,
    String actionLabel,
    String description,
    String workspaceCode,
    Long workspaceReferenceId,
    List<ProductProcessActivityRequirementResponse> requirements) {

  /** Mantém o contrato simples dos executores que não precisam de uma área operacional própria. */
  public BackendProductProcessActivityReadiness(boolean ready, String reason) {
    this(
        ready,
        reason,
        "Executar atividade",
        "O backend validará e registrará o resultado desta atividade.",
        null,
        null,
        List.of());
  }

  /** Preserva requisitos imutáveis na projeção da tela. */
  public BackendProductProcessActivityReadiness {
    requirements = requirements == null ? List.of() : List.copyOf(requirements);
  }
}
