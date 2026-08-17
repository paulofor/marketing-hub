package com.marketinghub.repository.jpa.agentlearning;

import com.marketinghub.agentlearning.v1.TemisVisualLearningRun;
import com.marketinghub.agentlearning.v1.TemisVisualLearningRunStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir e reservar consolidações governadas de Têmis. */
public interface TemisVisualLearningRunRepository
    extends JpaRepository<TemisVisualLearningRun, Long> {
  /** Reserva pendências ou recupera uma execução interrompida pelo lease. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select r from TemisVisualLearningRun r where r.status = :pending"
          + " or (r.status = :processing and r.startedAt < :cutoff) order by r.id asc")
  List<TemisVisualLearningRun> findClaimable(
      @Param("pending") TemisVisualLearningRunStatus pending,
      @Param("processing") TemisVisualLearningRunStatus processing,
      @Param("cutoff") Instant cutoff);

  /** Lista as execuções mais recentes para a prestação de contas administrativa. */
  List<TemisVisualLearningRun> findAllByOrderByIdDesc();
}
