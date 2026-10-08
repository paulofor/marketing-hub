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
  /** Localiza somente trabalho vigente da modalidade solicitada, sem reabrir ciclos históricos. */
  @Query(
      "select e from VegaAdjustmentExecution e, VegaPrivateSession s, com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle c "
          + "where e.sessionId = s.id and s.cycleId = c.id and s.origin in :origins "
          + "and s.revoked = false and s.expiresAt > :now and c.status = 'OPEN' "
          + "and c.stage in ('ADJUSTMENT', 'VALIDATION') and c.productVersion = s.prototypeVersion "
          + "and (e.status = 'QUEUED' or (e.status = 'RUNNING' and e.leaseUntil < :now)) order by e.id")
  List<VegaAdjustmentExecution> pending(
      @Param("now") Instant now, @Param("origins") List<String> origins, Pageable page);

  /** Conta tentativas efetivamente persistidas da própria sessão, inclusive falhas. */
  long countBySessionId(String sessionId);

  /** Conta o consumo sintético acumulado do ciclo e versão diretamente no banco. */
  @Query(
      "select count(e) from VegaAdjustmentExecution e, VegaPrivateSession s "
          + "where e.sessionId = s.id and s.cycleId = :cycleId and s.prototypeVersion = :version "
          + "and s.origin in :origins")
  long countSyntheticAttempts(
      @Param("cycleId") Long cycleId,
      @Param("version") String version,
      @Param("origins") List<String> origins);

  /** Serializa reserva e callback da execução para impedir conclusões concorrentes. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from VegaAdjustmentExecution e where e.id = :id")
  Optional<VegaAdjustmentExecution> findLocked(@Param("id") Long id);

  /** Preserva o histórico de tentativas da mesma leitura para auditoria. */
  List<VegaAdjustmentExecution> findBySessionIdOrderByIdAsc(String sessionId);
}
