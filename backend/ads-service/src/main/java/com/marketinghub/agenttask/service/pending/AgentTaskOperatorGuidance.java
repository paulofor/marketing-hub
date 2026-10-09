package com.marketinghub.agenttask.service.pending;

import com.marketinghub.agenttask.AgentTaskPendingResponse;
import com.marketinghub.agenttask.service.pending.AgentTaskPendingWithOperatorGuidance.OperatorGuidance;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: consultar a orientação do operador somente para o produto canônico da tarefa.
 */
@Component
@RequiredArgsConstructor
public class AgentTaskOperatorGuidance {
  private final ProductRepository products;
  private final Clock clock;

  /**
   * Reconsulta as notas atuais sem interpretar saldo, conceder gasto ou inferir produto pelo nome.
   */
  @Transactional(readOnly = true)
  public OperatorGuidance read(AgentTaskPendingResponse task) {
    var target = task.taskTarget();
    if (target == null
        || target.productId() == null
        || !Objects.equals(task.sourceReference(), target.sourceReference())) return null;
    return products
        .findById(target.productId())
        .filter(product -> Objects.equals(target.productId(), product.getId()))
        .map(product -> product.getCommercialNotes())
        .filter(notes -> !notes.isBlank())
        .map(
            notes ->
                new OperatorGuidance(
                    "PRODUCT_OPERATOR_GUIDANCE_V1",
                    target.productId(),
                    "internal://products/" + target.productId() + "/commercial-notes",
                    Instant.now(clock),
                    notes))
        .orElse(null);
  }
}
