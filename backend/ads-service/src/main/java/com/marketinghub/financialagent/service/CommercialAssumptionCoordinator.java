package com.marketinghub.financialagent.service;

import com.marketinghub.experimentstrategist.service.ExperimentStrategistExecutionService.CommercialAssumptionsProposed;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Responsabilidade: encaminhar a proposta concluída de Atena para a validação de Plutus. */
@Component
public class CommercialAssumptionCoordinator {
  private final FinancialAgentService financialAgentService;

  /** Configura o serviço financeiro que recebe a proposta estratégica. */
  public CommercialAssumptionCoordinator(FinancialAgentService financialAgentService) {
    this.financialAgentService = financialAgentService;
  }

  /** Abre a validação financeira somente depois do parecer de Atena ser persistido. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onProposed(CommercialAssumptionsProposed event) {
    financialAgentService.startAssumptionValidation(
        event.commercialPlanId(), event.strategistExecutionId(), event.recommendationJson());
  }
}
