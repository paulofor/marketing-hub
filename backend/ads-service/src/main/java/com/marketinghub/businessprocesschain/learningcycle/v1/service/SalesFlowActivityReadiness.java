package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.product.Product;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Responsabilidade: impedir comandos de agente fora da passagem comercial autorizada. */
@Component
@RequiredArgsConstructor
public class SalesFlowActivityReadiness implements AgentProductProcessActivityReadinessProvider {
  private final SalesFlowResolver flow;

  /** Restringe esta política aos subprocessos de operação e entrega do Processo 6. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return Set.of(SalesFlowResolver.OPERATION_CODE, SalesFlowResolver.DELIVERY_CODE)
        .contains(process.getProcessCode());
  }

  /** Usa a mesma posição apresentada nas telas para validar o comando solicitado. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String sourceReference) {
    String blocker = flow.executionBlocker(product.getId(), process, sourceReference);
    return new AgentProductProcessActivityReadiness(
        blocker == null,
        blocker == null ? "Subprocesso compatível com a passagem atual do produto." : blocker);
  }
}
