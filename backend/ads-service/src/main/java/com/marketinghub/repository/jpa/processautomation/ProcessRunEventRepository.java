package com.marketinghub.repository.jpa.processautomation;

import com.marketinghub.businessprocess.automation.v1.ProcessRunEvent;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: consultar o diário imutável e deduplicar disparos por entrada funcional. */
public interface ProcessRunEventRepository extends JpaRepository<ProcessRunEvent, Long> {
  /** Impede repetir a mesma entrada sem progresso ou retomada explícita. */
  boolean existsByRunIdAndActionKey(Long runId, String actionKey);

  /** Pagina decisões da execução exata sem transferir histórico ilimitado. */
  @Query(
      "select e from ProcessRunEvent e where e.runId = :runId and (:beforeId is null or e.id < :beforeId) order by e.id desc")
  List<ProcessRunEvent> history(
      @Param("runId") Long runId, @Param("beforeId") Long beforeId, Pageable pageable);
}
