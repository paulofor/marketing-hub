package com.marketinghub.communication.v1;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.*;
import com.marketinghub.product.Product;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Responsabilidade: invalidar conclusões privadas quando contratos, provas ou política comercial
 * mudarem.
 */
@Service
@RequiredArgsConstructor
public class PrivateCommunicationActivityReadinessProvider
    implements AgentProductProcessActivityReadinessProvider {
  private final PrivateCommunicationJourney journey;

  /** Reconhece as atividades que não podem herdar prova privada incompatível no processo pai. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return "pde-communication-sales-journey".equals(process.getProcessCode())
        && Set.of("communicationContract", "creatives", "destination", "integration")
            .contains(activity.getActivityId());
  }

  /** Delega a prontidão ao executor backend, evitando recomputar a mesma prova na projeção. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    if (journey.incompatibleCommercialPolicy(process, reference)) {
      return new AgentProductProcessActivityReadiness(
          false,
          "A validação privada permanece no histórico, mas a versão atual exige um experimento comercial de Instagram Ads.");
    }
    return new AgentProductProcessActivityReadiness(
        true, "O executor backend confirma os contratos e as provas do regime selecionado.");
  }

  /**
   * Reabre a prova privada que deixou de corresponder à política, à versão ou aos predecessores.
   */
  @Override
  public boolean requiresFreshExecution(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    if (journey.incompatibleCommercialPolicy(process, reference)) return true;
    return Set.of("destination", "integration").contains(activity.getActivityId())
        && journey.applies(reference)
        && journey.stale(process, activity, product, reference);
  }
}
