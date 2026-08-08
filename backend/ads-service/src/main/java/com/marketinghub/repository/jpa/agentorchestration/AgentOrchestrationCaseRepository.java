package com.marketinghub.repository.jpa.agentorchestration;

import com.marketinghub.agentorchestration.AgentOrchestrationCase;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e consultar casos de orquestracao de agentes. */
public interface AgentOrchestrationCaseRepository
    extends JpaRepository<AgentOrchestrationCase, Long> {
  /** Busca o caso idempotente do plano e experimento. */
  Optional<AgentOrchestrationCase> findByCommercialPlanIdAndExperimentId(
      Long commercialPlanId, Long experimentId);

  /** Lista o historico corrente de coordenacao de um planejamento. */
  List<AgentOrchestrationCase> findByCommercialPlanIdOrderByUpdatedAtDesc(Long commercialPlanId);
}
