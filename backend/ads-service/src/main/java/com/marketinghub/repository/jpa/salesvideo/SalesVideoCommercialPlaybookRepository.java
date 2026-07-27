package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.SalesVideoCommercialPlaybook;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de SalesVideoCommercialPlaybook. */
public interface SalesVideoCommercialPlaybookRepository
    extends JpaRepository<SalesVideoCommercialPlaybook, Long> {
  List<SalesVideoCommercialPlaybook> findByProfileIdAndTenantIdOrderByCreatedAtDesc(
      Long profileId, String tenantId);
}
