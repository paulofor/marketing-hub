package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.SalesVideoJobEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Auditoria de eventos dos jobs. */
public interface SalesVideoJobEventRepository extends JpaRepository<SalesVideoJobEvent, Long> {
  List<SalesVideoJobEvent> findByJobIdOrderByCreatedAtAsc(Long jobId);
}
