package com.marketinghub.repository.jpa.experimentstrategist;

import com.marketinghub.experimentstrategist.ExperimentStrategistExecution;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecutionStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Responsabilidade: persistir e reservar execucoes auditaveis do Estrategista. */
public interface ExperimentStrategistExecutionRepository
    extends JpaRepository<ExperimentStrategistExecution, Long> {
  /** Lista as pesquisas recentes de um planejamento. */
  List<ExperimentStrategistExecution> findByCommercialPlanIdOrderByCreatedAtDesc(Long planId);

  /** Reserva a pesquisa pendente mais antiga com bloqueio transacional. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<ExperimentStrategistExecution> findByStatusOrderByCreatedAtAsc(
      ExperimentStrategistExecutionStatus status, Pageable pageable);

  /** Vincula a versão ativa do contrato do Estrategista à execução criada. */
  @Modifying
  @Query(
      value =
          "UPDATE experiment_strategist_execution e JOIN agent a ON a.agent_key = 'experiment-strategist' JOIN agent_version av ON av.agent_id = a.id AND av.version_number = a.current_version SET e.agent_version_id = av.id WHERE e.id = ?1 AND e.agent_version_id IS NULL",
      nativeQuery = true)
  void attachCurrentAgentVersion(Long executionId);
}
