package com.marketinghub.salesvideo.repository;

import com.marketinghub.salesvideo.SalesVideoCommercialPlaybook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesVideoCommercialPlaybookRepository extends JpaRepository<SalesVideoCommercialPlaybook, Long> {
    List<SalesVideoCommercialPlaybook> findByProfileIdAndTenantIdOrderByCreatedAtDesc(Long profileId, String tenantId);
}
