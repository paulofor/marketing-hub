package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.SalesVideoJobEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Auditoria de eventos dos jobs.
 */
public interface SalesVideoJobEventRepository extends JpaRepository<SalesVideoJobEvent, Long> {
    List<SalesVideoJobEvent> findByJobIdOrderByCreatedAtAsc(Long jobId);
}
