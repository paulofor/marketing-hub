package com.marketinghub.repository.jpa.financialagent;

import com.marketinghub.financialagent.FinancialAgentExecution;
import com.marketinghub.financialagent.FinancialAgentExecutionStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/** Responsabilidade: persistir execucoes auditaveis do Agente Financeiro. */
public interface FinancialAgentExecutionRepository
    extends JpaRepository<FinancialAgentExecution, Long> {
  /** Lista os relatorios mais recentes de um planejamento. */
  List<FinancialAgentExecution> findByCommercialPlanIdOrderByCreatedAtDesc(Long planId);

  /** Reserva a conciliacao pendente mais antiga. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<FinancialAgentExecution> findByStatusOrderByCreatedAtAsc(
      FinancialAgentExecutionStatus status, Pageable pageable);
}
