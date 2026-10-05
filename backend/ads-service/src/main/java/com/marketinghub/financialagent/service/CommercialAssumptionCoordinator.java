package com.marketinghub.financialagent.service;

import com.marketinghub.experimentstrategist.service.ExperimentStrategistExecutionService.CommercialAssumptionsProposed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Responsabilidade: encaminhar a proposta concluída de Atena para a validação de Plutus. */
@Component
public class CommercialAssumptionCoordinator {
  private static final Logger log = LoggerFactory.getLogger(CommercialAssumptionCoordinator.class);
  private final FinancialAgentService financialAgentService;

  /** Configura o serviço financeiro que recebe a proposta estratégica. */
  public CommercialAssumptionCoordinator(FinancialAgentService financialAgentService) {
    this.financialAgentService = financialAgentService;
  }

  /** Persiste a validação em nova transação, preservando o parecer já pago e confirmado. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onProposed(CommercialAssumptionsProposed event) {
    try {
      financialAgentService.startAssumptionValidation(
          event.commercialPlanId(), event.strategistExecutionId(), event.recommendationJson());
    } catch (Exception ex) {
      log.error(
          "Falha na passagem Atena para Plutus; planId={} strategistExecutionId={}",
          event.commercialPlanId(),
          event.strategistExecutionId(),
          ex);
      throw ex;
    }
  }
}
