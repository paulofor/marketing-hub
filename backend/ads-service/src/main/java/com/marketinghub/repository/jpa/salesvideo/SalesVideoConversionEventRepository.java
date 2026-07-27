package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.SalesVideoConversionEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável pela persistência de SalesVideoConversionEvent. */
public interface SalesVideoConversionEventRepository
    extends JpaRepository<SalesVideoConversionEvent, Long> {
  /** Lista eventos comerciais de vídeo de um tenant do mais recente para o mais antigo. */
  List<SalesVideoConversionEvent> findByTenantIdOrderByOccurredAtDesc(String tenantId);

  List<SalesVideoConversionEvent> findByProfileIdAndTenantIdOrderByOccurredAtDesc(
      Long profileId, String tenantId);

  List<SalesVideoConversionEvent>
      findByProfileIdAndTenantIdAndOccurredAtBetweenOrderByOccurredAtDesc(
          Long profileId, String tenantId, Instant from, Instant to);
}
