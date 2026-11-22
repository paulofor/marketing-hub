package com.marketinghub.repository.jpa.processautomation;

import com.marketinghub.businessprocess.automation.v1.ProcessRunEvent;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: consultar o diário imutável e deduplicar disparos por entrada funcional. */
public interface ProcessRunEventRepository extends JpaRepository<ProcessRunEvent, Long> {
  /** Recupera a explicação mais recente da pausa sem confundir execuções do mesmo produto. */
  java.util.Optional<ProcessRunEvent> findFirstByRunIdAndEventTypeOrderByIdDesc(
      Long runId, String eventType);

  /** Impede repetir a mesma entrada sem progresso ou retomada explícita. */
  boolean existsByRunIdAndActionKey(Long runId, String actionKey);

  /** Confirma uma retomada explícita posterior à conclusão da origem, sem inferir autorização. */
  boolean existsByRunIdAndEventTypeAndCreatedAtAfter(
      Long runId, String eventType, java.time.Instant createdAt);

  /** Pagina decisões da execução exata sem transferir histórico ilimitado. */
  @Query(
      "select e from ProcessRunEvent e where e.runId = :runId and (:beforeId is null or e.id < :beforeId) order by e.id desc")
  List<ProcessRunEvent> history(
      @Param("runId") Long runId, @Param("beforeId") Long beforeId, Pageable pageable);

  /** Consulta recibos de continuidade exclusivamente dentro do produto, ciclo e referência. */
  @Query(
      "select e from ProcessRunEvent e, ProcessRun r where e.runId = r.id "
          + "and e.eventType = 'VERSION_CONTINUATION' and r.productId = :productId "
          + "and r.learningCycleId = :cycleId and r.sourceReference = :reference order by e.id desc")
  List<ProcessRunEvent> continuations(
      @Param("productId") Long productId,
      @Param("cycleId") Long cycleId,
      @Param("reference") String reference);
}
