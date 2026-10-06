package com.marketinghub.product.service.valuechainposition;

import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: priorizar a navegação do ciclo pendente sobre a posição comercial histórica.
 */
@Component
@RequiredArgsConstructor
public class ProductLearningCycleNavigationResolver {
  private final LearningSalesCycleRepository cycles;

  /** Consulta a última passagem da versão exata da cadeia, preservando decisões e autorizações. */
  public ProductLearningCycleNavigationResponse resolve(
      Long productId, BusinessProcessChainDefinition chain) {
    var cycle =
        cycles
            .findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(productId, chain.getId())
            .orElse(null);
    if (cycle == null
        || !Objects.equals(productId, cycle.getProductId())
        || !Objects.equals(chain.getId(), cycle.getChainDefinitionId())
        || !Objects.equals(chain.getChainCode(), cycle.getChainCode())) return null;
    boolean open = "OPEN".equals(cycle.getStatus()) && cycle.getClosedAt() == null;
    boolean awaitingSuccessor =
        "ADJUSTED".equals(cycle.getStatus())
            && cycle.getClosedAt() != null
            && cycles.findByPreviousCycleId(cycle.getId()).isEmpty();
    if (!open && !awaitingSuccessor) return null;
    return new ProductLearningCycleNavigationResponse(
        productId,
        cycle.getId(),
        cycle.getExperimentId(),
        cycle.getChainDefinitionId(),
        cycle.getStage(),
        cycle.getStatus(),
        open
            ? "Analise as evidências e acompanhe a próxima ação no ciclo atual."
            : "Ajuste aprovado. Falta o backend preparar o novo ciclo e experimento. Consulte a continuidade e os limites no ciclo; não refaça a homologação anterior.",
        "/business-process-chains/learning-cycles?productId="
            + productId
            + "&chainId="
            + cycle.getChainDefinitionId()
            + "&cycleId="
            + cycle.getId());
  }
}
