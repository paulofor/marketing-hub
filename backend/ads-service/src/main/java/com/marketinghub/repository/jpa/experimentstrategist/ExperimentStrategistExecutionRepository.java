package com.marketinghub.repository.jpa.experimentstrategist;

import com.marketinghub.experimentstrategist.ExperimentStrategistExecution;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecutionStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

  /** Busca o parecer mais recente do planejamento para coordenacao entre agentes. */
  Optional<ExperimentStrategistExecution> findFirstByCommercialPlanIdOrderByCreatedAtDesc(
      Long planId);

  /** Reserva a pesquisa pendente mais antiga com bloqueio transacional. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<ExperimentStrategistExecution> findByStatusOrderByCreatedAtAsc(
      ExperimentStrategistExecutionStatus status, Pageable pageable);

  /** Lista leases em execução para recuperação auditável antes de uma nova reserva. */
  List<ExperimentStrategistExecution> findByStatusAndStartedAtBeforeOrderByStartedAtAsc(
      ExperimentStrategistExecutionStatus status, Instant threshold);

  /** Conta telemetria recente que comprova atividade viva da pesquisa. */
  @Query(
      value =
          "SELECT COUNT(*) FROM codex_agent_execution_telemetry "
              + "WHERE agent_type = 'EXPERIMENT_STRATEGIST' AND execution_id = ?1 "
              + "AND status = 'RUNNING' AND process_alive = 1 AND last_activity_at >= ?2",
      nativeQuery = true)
  long countRecentActiveTelemetry(Long executionId, Instant cutoff);

  /** Vincula a versão ativa do contrato do Estrategista à execução criada. */
  @Modifying
  @Query(
      value =
          "UPDATE experiment_strategist_execution e JOIN agent a ON a.agent_key = 'experiment-strategist' JOIN agent_version av ON av.agent_id = a.id AND av.version_number = a.current_version SET e.agent_version_id = av.id WHERE e.id = ?1 AND e.agent_version_id IS NULL",
      nativeQuery = true)
  void attachCurrentAgentVersion(Long executionId);
}
