package com.marketinghub.repository.jpa.processautomation;

import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.service.status.ProcessRunIdentity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir execuções e consultar pendências sem carregar auditorias. */
public interface ProcessRunRepository extends JpaRepository<ProcessRun, Long> {
  /**
   * Lê somente identidade, evitando entidade desatualizada no contexto Open EntityManager in View.
   */
  @Query(
      "select new com.marketinghub.businessprocess.automation.v1.service.status.ProcessRunIdentity(r.productId, r.processDefinitionId) from ProcessRun r where r.id = :id")
  Optional<ProcessRunIdentity> identity(@Param("id") Long id);

  /** Localiza a execução idempotente do contexto congelado. */
  Optional<ProcessRun> findByScopeKey(String scopeKey);

  /** Distribui a conciliação pela execução menos observada, excluindo contextos encerrados. */
  @Query(
      "select r.id from ProcessRun r where r.status not in ('PAUSED', 'COMPLETED', 'ERROR', 'CLOSED') order by r.lastReconciledAt, r.id")
  List<Long> pending(Pageable pageable);

  /** Ordena processos ativos sem deixar um ciclo encerrado reservar o produto indefinidamente. */
  @Query(
      "select r from ProcessRun r where r.productId = :productId and r.parentRunId is null and r.status not in ('PAUSED', 'COMPLETED', 'CLOSED') order by r.id")
  List<ProcessRun> activeRoots(@Param("productId") Long productId);

  /** Localiza delegações para aguardar trabalho em curso antes de liberar outro processo. */
  List<ProcessRun> findAllByParentRunId(Long parentRunId);
}
