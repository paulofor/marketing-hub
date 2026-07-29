package com.marketinghub.repository.jpa.socialdistribution;

import com.marketinghub.socialdistribution.SocialPublicationMetric;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir snapshots de métricas das publicações orgânicas. */
public interface SocialPublicationMetricRepository
    extends JpaRepository<SocialPublicationMetric, Long> {
  /** Retorna a métrica mais recente de uma publicação. */
  Optional<SocialPublicationMetric> findFirstByPublicationIdOrderByCapturedAtDesc(
      Long publicationId);
}
