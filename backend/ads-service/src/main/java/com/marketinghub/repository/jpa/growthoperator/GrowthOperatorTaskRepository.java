package com.marketinghub.repository.jpa.growthoperator;

import com.marketinghub.growthoperator.GrowthOperatorTask;
import com.marketinghub.growthoperator.GrowthOperatorTaskStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar pendencias auditaveis do Operador de Crescimento. */
public interface GrowthOperatorTaskRepository extends JpaRepository<GrowthOperatorTask, Long> {
  /** Lista pendencias do planejamento da mais recente para a mais antiga. */
  List<GrowthOperatorTask> findByCommercialPlanIdOrderByCreatedAtDesc(Long planId);

  /** Localiza recomendacao ainda aberta para impedir duplicacao entre ciclos. */
  Optional<GrowthOperatorTask> findFirstByCommercialPlanIdAndActionKeyAndStatus(
      Long planId, String actionKey, GrowthOperatorTaskStatus status);
}
