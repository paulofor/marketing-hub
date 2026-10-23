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
  /** Consulta no SQL apenas a pesquisa de Big Idea da identidade solicitada. */
  @Query(
      value =
          "SELECT * FROM experiment_strategist_execution WHERE authority_mode = 'PRODUCT_BIG_IDEA_RESEARCH' "
              + "AND JSON_UNQUOTE(JSON_EXTRACT(evidence_snapshot, '$.bigIdeaResearch.productId')) = CAST(?1 AS CHAR) ORDER BY id DESC LIMIT 1",
      nativeQuery = true)
  Optional<ExperimentStrategistExecution> findLatestBigIdea(Long productId);

  /** Lista as pesquisas recentes de um planejamento. */
  List<ExperimentStrategistExecution> findByCommercialPlanIdOrderByCreatedAtDesc(Long planId);

  /** Busca o parecer do planejamento sem substituir sua coordenação pela pesquisa de Big Idea. */
  @Query(
      value =
          "SELECT * FROM experiment_strategist_execution WHERE commercial_plan_id = ?1 "
              + "AND (authority_mode IS NULL OR authority_mode <> 'PRODUCT_BIG_IDEA_RESEARCH') "
              + "ORDER BY created_at DESC, id DESC LIMIT 1",
      nativeQuery = true)
  Optional<ExperimentStrategistExecution> findFirstByCommercialPlanIdOrderByCreatedAtDesc(
      Long planId);

  /** Localiza a proposta econômica mais recente para retomada no mesmo contexto. */
  Optional<ExperimentStrategistExecution>
      findFirstByCommercialPlanIdAndAuthorityModeOrderByCreatedAtDescIdDesc(
          Long planId, String authorityMode);

  /** Busca a última estratégia concluída no modo autoritativo de pesquisa. */
  Optional<ExperimentStrategistExecution>
      findFirstByCommercialPlanIdAndStatusAndAuthorityModeOrderByFinishedAtDescIdDesc(
          Long planId, ExperimentStrategistExecutionStatus status, String authorityMode);

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
