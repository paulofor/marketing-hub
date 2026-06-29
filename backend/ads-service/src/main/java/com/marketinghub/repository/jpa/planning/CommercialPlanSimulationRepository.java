package com.marketinghub.repository.jpa.planning;

import com.marketinghub.planning.CommercialPlanSimulation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e consultar simulacoes de planos comerciais. */
public interface CommercialPlanSimulationRepository extends JpaRepository<CommercialPlanSimulation, Long> {
    /** Lista simulacoes de um plano priorizando as mais recentes. */
    List<CommercialPlanSimulation> findByPlanIdOrderByCreatedAtDesc(Long planId);
}
