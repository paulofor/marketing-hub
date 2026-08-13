package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.SalesVideoJobEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Auditoria de eventos dos jobs. */
public interface SalesVideoJobEventRepository extends JpaRepository<SalesVideoJobEvent, Long> {
  /** Lista eventos que comprovam cenas aceitas por uma família de provedor. */
  @Query(
      "select e from SalesVideoJobEvent e join fetch e.job j "
          + "where upper(j.providerName) like concat(upper(:provider), '%') "
          + "and e.eventType = com.marketinghub.salesvideo.SalesVideoJobEventType.PROGRESS "
          + "and lower(e.message) like '%processando cena %' order by e.createdAt asc")
  List<SalesVideoJobEvent> findAcceptedSceneEvents(@Param("provider") String provider);

  /** Lista eventos de cena com task id explícito produzidos pelos executores novos. */
  @Query(
      "select e from SalesVideoJobEvent e join fetch e.job j "
          + "where upper(j.providerName) like concat(upper(:provider), '%') "
          + "and e.eventType = com.marketinghub.salesvideo.SalesVideoJobEventType.PROGRESS "
          + "and lower(e.message) like '%aceitou cena %' order by e.createdAt asc")
  List<SalesVideoJobEvent> findExplicitAcceptedSceneEvents(@Param("provider") String provider);

  List<SalesVideoJobEvent> findByJobIdOrderByCreatedAtAsc(Long jobId);
}
