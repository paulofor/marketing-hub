package com.marketinghub.businessprocess.execution.service.agentactivity;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.product.Product;

/** Responsabilidade: validar pré-condições externas ao processo antes de acionar um agente. */
public interface AgentProductProcessActivityReadinessProvider {

  /** Informa se o provedor governa a atividade de agente recebida. */
  boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition);

  /** Verifica contratos predecessores sem executar modelo nem alterar estado. */
  AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference);
}
