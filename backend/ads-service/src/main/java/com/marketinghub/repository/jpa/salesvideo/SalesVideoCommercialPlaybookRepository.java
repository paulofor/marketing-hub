package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.SalesVideoCommercialPlaybook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório JPA responsável pela persistência de SalesVideoCommercialPlaybook.
 */
public interface SalesVideoCommercialPlaybookRepository extends JpaRepository<SalesVideoCommercialPlaybook, Long> {
    List<SalesVideoCommercialPlaybook> findByProfileIdAndTenantIdOrderByCreatedAtDesc(Long profileId, String tenantId);
}
