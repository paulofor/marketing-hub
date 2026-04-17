package com.marketinghub.salesvideo.repository;

import com.marketinghub.salesvideo.SalesVideoConversionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SalesVideoConversionEventRepository extends JpaRepository<SalesVideoConversionEvent, Long> {
    List<SalesVideoConversionEvent> findByProfileIdAndTenantIdOrderByOccurredAtDesc(Long profileId, String tenantId);

    List<SalesVideoConversionEvent> findByProfileIdAndTenantIdAndOccurredAtBetweenOrderByOccurredAtDesc(Long profileId,
                                                                                                          String tenantId,
                                                                                                          Instant from,
                                                                                                          Instant to);
}
