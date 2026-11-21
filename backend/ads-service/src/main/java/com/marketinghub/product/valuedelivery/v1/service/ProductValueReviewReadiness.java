package com.marketinghub.product.valuedelivery.v1.service;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.*;
import com.marketinghub.product.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Evita revisões pagas quando a implementação ainda não comprova as histórias da proposta. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductValueReviewReadiness implements AgentProductProcessActivityReadinessProvider {
  private final ProductValueDeliveryService delivery;

  /** Aplica a preparação apenas às revisões independentes da nova versão do processo. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return activity != null
        && delivery.enabled(process)
        && ProductValueDeliveryService.REVIEWS.contains(activity.getActivityId());
  }

  /** Explica a falta real sem criar tarefa, aprovação ou inferência adicional. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String source) {
    try {
      delivery.verify(process, source, false);
      return new AgentProductProcessActivityReadiness(
          true, "Histórias e provas disponíveis para revisão independente.");
    } catch (RuntimeException ex) {
      log.info(
          "Revisão de valor aguarda implementação productId={} source={} activity={}",
          product == null ? null : product.getId(),
          source,
          activity.getActivityId(),
          ex);
      return new AgentProductProcessActivityReadiness(false, ex.getMessage());
    }
  }
}
