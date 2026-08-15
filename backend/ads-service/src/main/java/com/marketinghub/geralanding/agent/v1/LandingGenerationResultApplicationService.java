package com.marketinghub.geralanding.agent.v1;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: isolar a aplicação da landing gerada da persistência do callback do agente. */
@Service
public class LandingGenerationResultApplicationService {
  private final LandingGenerationAgentCoordinator coordinator;

  /** Inicializa o aplicador com o coordenador canônico da landing. */
  public LandingGenerationResultApplicationService(LandingGenerationAgentCoordinator coordinator) {
    this.coordinator = coordinator;
  }

  /** Aplica a decisão em transação independente para não invalidar o callback auditável. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void apply(Long experimentId, String autonomousCycleId, String decisionJson) {
    coordinator.continueAfterQualityReview(experimentId, autonomousCycleId, decisionJson);
  }
}
