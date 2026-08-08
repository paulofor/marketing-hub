package com.marketinghub.repository.jpa.growthoperator;

import com.marketinghub.growthoperator.GrowthOperatorExecution;
import com.marketinghub.growthoperator.GrowthOperatorExecutionStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  /** Conta telemetrias recentes que comprovam atividade viva da execucao do Operador. */
  @Query(
      value =
          "SELECT COUNT(*) FROM codex_agent_execution_telemetry "
              + "WHERE agent_type = 'GROWTH_OPERATOR' AND execution_id = :executionId "
              + "AND status = 'RUNNING' AND process_alive = 1 AND last_activity_at >= :cutoff",
      nativeQuery = true)
  long countRecentActiveTelemetry(
      @Param("executionId") Long executionId, @Param("cutoff") Instant cutoff);

  /** Vincula a execucao a versao ativa do cadastro canonico do Operador. */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      value =
          "UPDATE growth_operator_execution goe "
              + "JOIN agent a ON a.agent_key = 'growth-operator' "
              + "JOIN agent_version av ON av.agent_id = a.id AND av.version_number = a.current_version "
              + "SET goe.agent_version_id = av.id WHERE goe.id = :executionId",
      nativeQuery = true)
  void attachCurrentAgentVersion(@Param("executionId") Long executionId);
}
