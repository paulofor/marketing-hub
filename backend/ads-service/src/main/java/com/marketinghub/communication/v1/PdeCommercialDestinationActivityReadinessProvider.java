package com.marketinghub.communication.v1;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Responsabilidade: reabrir o destino do Processo 4 quando a publicação comercial material mudar.
 */
@Service
@RequiredArgsConstructor
public class PdeCommercialDestinationActivityReadinessProvider
    implements AgentProductProcessActivityReadinessProvider {
  private final PdeCommercialCommunicationDestination destination;

  /** Reconhece apenas a atividade canônica de destino da comunicação. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return "pde-communication-sales-journey".equals(process.getProcessCode())
        && "destination".equals(activity.getActivityId());
  }

  /** Mantém o executor backend como autoridade para explicar a prontidão material do destino. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String sourceReference) {
    return new AgentProductProcessActivityReadiness(
        true, "O backend confere o slot comercial e sua publicação atual.");
  }

  /** Exige nova ocorrência quando a prova concluída não representa mais o slot vigente. */
  @Override
  public boolean requiresFreshExecution(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String sourceReference) {
    return destination.requiresFreshExecution(activity, product, sourceReference);
  }
}
