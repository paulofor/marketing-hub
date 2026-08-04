package com.marketinghub.repository.jpa.growthoperator;

import com.marketinghub.growthoperator.GrowthOperatorExecution;
import com.marketinghub.growthoperator.GrowthOperatorExecutionStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/** Responsabilidade: persistir execucoes auditaveis do Operador de Crescimento. */
public interface GrowthOperatorExecutionRepository
    extends JpaRepository<GrowthOperatorExecution, Long> {
  /** Lista as execucoes recentes de um planejamento. */
  List<GrowthOperatorExecution> findByCommercialPlanIdOrderByCreatedAtDesc(Long planId);

  /** Busca a proxima pendencia para consumo controlado pelo worker. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<GrowthOperatorExecution> findByStatusOrderByCreatedAtAsc(
      GrowthOperatorExecutionStatus status, Pageable pageable);

  /** Busca uma execucao vinculada ao plano informado. */
  Optional<GrowthOperatorExecution> findByIdAndCommercialPlanId(Long id, Long planId);

  /** Busca o ciclo mais recente de um planejamento. */
  Optional<GrowthOperatorExecution> findFirstByCommercialPlanIdOrderByCreatedAtDesc(Long planId);
}
