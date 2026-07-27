package com.marketinghub.repository.jpa.funnel;

import com.marketinghub.funnel.FunnelStep;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link FunnelStep}. */
public interface FunnelStepRepository extends JpaRepository<FunnelStep, UUID> {
  @EntityGraph(attributePaths = "funnel")
  Optional<FunnelStep> findWithFunnelById(UUID id);

  List<FunnelStep> findByFunnelId(UUID funnelId);
}
