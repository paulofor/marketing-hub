package com.marketinghub.repository.jpa.vega;

import com.marketinghub.pde.vega.privateprototype.v1.VegaAdjustmentExecution;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir tentativas e entregar a fila de geração do Vega ao executor. */
public interface VegaAdjustmentExecutionRepository
    extends JpaRepository<VegaAdjustmentExecution, Long> {
  /** Localiza pendências e reservas vencidas sem realizar processamento externo no backend. */
  @Query(
      "select e from VegaAdjustmentExecution e where e.status = 'QUEUED' or (e.status = 'RUNNING' and e.leaseUntil < :now) order by e.id")
  List<VegaAdjustmentExecution> pending(@Param("now") Instant now, Pageable page);

  /** Serializa reserva e callback da execução para impedir conclusões concorrentes. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from VegaAdjustmentExecution e where e.id = :id")
  Optional<VegaAdjustmentExecution> findLocked(@Param("id") Long id);

  /** Preserva o histórico de tentativas da mesma leitura para auditoria. */
  List<VegaAdjustmentExecution> findBySessionIdOrderByIdAsc(String sessionId);
}
